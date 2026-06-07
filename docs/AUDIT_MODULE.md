# Audit Module (Phase 8.3)

Production audit subsystem built on the Phase 8.1 durable outbox and the
Phase 8.1 audit foundation. Append-only by construction: no `UPDATE`,
no `DELETE` for `authenticated`. All writes are mediated by
`AuditService`; all reads by `AuditSearchService` (admin only).

## Architecture

```text
 domain TX ── OutboxPublisher ──► outbox_events
                                        │
                                        ▼ (dispatcher)
                                OutboxDispatchEvent
                                        │
                ┌───────────────────────┼──────────────────────┐
                ▼                       ▼                      ▼
        AuditConsumer          NotificationConsumer        (future SIEM)
                │
                ▼ (via AuditEventRegistry)
         AuditService.record ──► audit_log (append-only)
                                        │
                                        ▼
                           OutboxPublisher (audit.record_created)
```

No domain service writes directly to `audit_log`. All audit rows arrive
through `AuditConsumer`, which resolves the event via the registry — so
changing severity / category / actor type is a config change, not code.

## Categories

`AUTH`, `SECURITY`, `VENDOR`, `CATALOG`, `INVENTORY`, `ORDER`, `PAYMENT`,
`REFUND`, `PAYOUT`, `SYSTEM`, `ADMIN`.

## Severity ladder

`INFO < WARNING < HIGH < CRITICAL`. `HIGH` is added in V015; specific
events (e.g. `settlement.locked`, `vendor.suspended`) are promoted at
boot by `AuditEventRegistry` because the new enum value cannot be used
inside the same migration that declares it.

## Event mappings

Source of truth: `audit_event_mappings`. Seeded by V015 with 32 mappings
spanning auth, vendor, catalog, inventory, order, payment, refund,
payout, notification. Admins override via direct table updates (DDL by
migration only — `REVOKE UPDATE, DELETE FROM authenticated`).

## Search

`AuditSearchService` exposes a JPA `Specification` built from
`AuditSearchCriteria` — all dimensions optional. Supports `actorId`,
`entityType/id`, `action`, `category`, `minSeverity`, time range,
`requestId`.

## Export

`AuditExportService.request(format, criteria)` writes an
`audit_export_requests` row in status `PENDING` and emits
`audit.export_requested` via the outbox. File generation is intentionally
deferred to a future sprint; this phase tracks intent only.

## Retention policies

Per-category days in `audit_retention_policies` (AUTH=365, PAYMENT/PAYOUT
/REFUND/ORDER=2555, etc.). Read via `AuditRetentionPolicyService`.
No purge job in this phase.

## Coverage validation

`AuditCoverageValidator` runs at boot. For each `REQUIRED_EVENTS` entry
missing from the registry, or any mapping with missing severity, it
publishes `audit.coverage_warning` via the outbox.

## Database (V015)

| Table | Append-only | Purpose |
|---|---|---|
| `audit_log` (extended) | yes | adds `category audit_category` + indexes |
| `audit_event_mappings` | config | event_type → action / category / severity / actor |
| `audit_retention_policies` | config | per-category retention days |
| `audit_export_requests` | yes | export intent + status |

## API contracts

All under `/api/v1/admin/audit`, all `hasRole('ADMIN')`:

- `GET /` — paged search (`actorId`, `entityType`, `entityId`, `action`, `category`, `minSeverity`, `from`, `to`, `requestId`, `page`, `size`).
- `GET /{id}` — single audit record.
- `POST /export` — body `AuditExportPayload`; returns the queued request.
- `GET /categories` — categories with retention days.
- `GET /actions` — registered mappings.

## Domain events emitted

- `audit.record_created`
- `audit.export_requested`
- `audit.coverage_warning`

All published through the durable outbox.

## Tests

- `AuditRegistryTest` — seeded mappings + HIGH promotion.
- `AuditConsumerIT` — mapped vs. unmapped event behaviour.
- `AuditSearchIT` — filters by actor / category / minSeverity.
- `AuditExportIT` — export request persistence.
- `AuditCoverageValidatorTest` — required events covered.

## Invariants

1. `audit_log` is append-only.
2. No service writes directly — all rows come from `AuditService.record`.
3. No hardcoded event → action mapping in services. All mappings flow
   through `AuditEventRegistry`.
4. Audit publishes its own events via the outbox; the consumer
   short-circuits on `audit.*` to prevent loops.