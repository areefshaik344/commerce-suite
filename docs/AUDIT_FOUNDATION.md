# Audit Foundation

_Phase 8.1 — addresses HIGH risk R-04._

## Table — `audit_log`

Append-only. `REVOKE UPDATE, DELETE FROM authenticated` is enforced in
`V013`. RLS: actor may SELECT their own rows; admin reads go through
application-layer `has_role` checks.

| column | notes |
|--------|-------|
| id | uuid PK |
| actor_id / actor_type | who did it (USER / ADMIN / VENDOR / SYSTEM) |
| entity_type / entity_id | target aggregate |
| action | enum-backed string (see `AuditAction.java`) |
| severity | INFO / WARNING / CRITICAL |
| metadata | jsonb context |
| request_id / correlation_id | trace propagation |
| ip_address / user_agent | forensic context |
| created_at | immutable |

## API

`AuditService.record(AuditContext)` — single entry point. Always called
inside a transaction; never persists outside one.

## Auto-audit pipeline

`AuditPublisher` subscribes to `OutboxDispatchEvent`. It maps the outbox
event type to an `AuditAction` and writes an `audit_log` row. The set of
auditable events is:

* All Auth events
* `vendor.approved`, `vendor.rejected`
* `product.approved`, `product.rejected`
* `inventory.adjusted`
* `order.created`, `order.cancelled`
* `refund.approved`
* `settlement.locked`
* `payout.completed`

Severity is `CRITICAL` for refresh-token reuse and security violations;
`WARNING` for rejections / suspensions; `INFO` otherwise.

## Retention

Indefinite for now; rotation policy will be defined in Sprint 8.4 along
with regulator-driven retention windows.
---

## Phase 8.3 update — superseded by Audit Module

The hardcoded `AuditPublisher` switch documented above is **replaced** by
the registry-driven `AuditConsumer`. Severity ladder gained `HIGH`, and
audit records now carry an `AuditCategory`. See `docs/AUDIT_MODULE.md`
for the full Phase 8.3 design (search, export, retention, coverage,
admin APIs, V015 schema).
