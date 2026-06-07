# Webhooks & External Integration Foundation

_Phase 8.5 — closes the BLOCKER R-05 (no webhook delivery) identified in
`PLATFORM_INTEGRATION_AUDIT.md` and the Sprint 8.5 scope of
`PHASE8_IMPLEMENTATION_BLUEPRINT.md`._

## Goals

- **Decoupled from business flows** — webhook failures MUST NEVER impact
  orders, payments, inventory, settlements, etc. All consumption and
  delivery runs `REQUIRES_NEW` with exception suppression.
- **At-least-once delivery** — every active subscription receives its
  matching event; receivers must remain idempotent (use `X-Webhook-Id`).
- **Replay safe** — HMAC-SHA256 signing with timestamp + nonce +
  rotation window prevents replay attacks.
- **Provider neutral** — no real ERP/CRM/Accounting/Marketing integrations
  are shipped; only abstraction (`ExternalIntegrationProvider`) and a
  registry exist.

## Architecture

```text
Business Event -> Outbox -> OutboxDispatchEvent -> WebhookConsumer
                                                     |
                                                     v
                                            WebhookDispatcher (scheduled)
                                              - sign + POST
                                              - retry w/ backoff
                                              - FSM transitions
```

Webhook subscribers NEVER read transactional services directly. The
single source of truth is the durable outbox from Phase 8.1.

## Delivery FSM

```text
PENDING -> QUEUED -> DELIVERING -> DELIVERED
                              `-> FAILED -> QUEUED (retry)
                                        `-> DEAD_LETTER
```

Implemented by `WebhookStateMachine`; every transition writes an
append-only row to `webhook_status_history`.

## Signature model

| Header | Description |
|--------|-------------|
| `X-Webhook-Id`        | Delivery UUID (use for idempotency). |
| `X-Webhook-Event`     | Canonical event type. |
| `X-Webhook-Timestamp` | Unix seconds. Rejected if >5m skew. |
| `X-Webhook-Nonce`     | Random 12-byte token; rejected on replay. |
| `X-Webhook-Signature` | Base64(HMAC-SHA256(secret, `ts.nonce.body`)). |

`WebhookSignatureVerifier` accepts the **active** secret and the
**previous** secret during rotation (single window) so subscribers can
roll keys without downtime.

## Retry strategy

`WebhookRetryService` — exponential backoff (`base * 2^(attempt-1)`)
capped at `webhooks.retry.max-seconds` (default 1h). After
`max_attempts` (default 10) the delivery transitions to `DEAD_LETTER`
and emits `webhook.dead_letter` via the outbox.

## Subscription model

- `webhook_endpoints` — physical URL + owner (ADMIN | VENDOR).
- `webhook_subscriptions` — endpoint x event-type (unique).
- `webhook_secrets` — HMAC secrets with status `ACTIVE`/`ROTATING`/`RETIRED`.

Subscriptions are filtered by `WebhookEventType.isKnown(eventType)` —
only events listed in `WebhookEventType` are routable.

## Event routing

`WebhookEventType` catalogs the routable surface (auth, vendor, catalog,
inventory, checkout, orders, shipping, returns, refunds, payments,
settlements, payouts, notifications, audit, analytics — see source
constants for the exhaustive list).

## Database tables (V017)

`webhook_endpoints`, `webhook_subscriptions`, `webhook_secrets`,
`webhook_deliveries`, `webhook_attempts`, `webhook_status_history`,
`external_integrations`. Append-only tables (`webhook_attempts`,
`webhook_status_history`) have `REVOKE UPDATE, DELETE`. RLS limits
writes to `admin` (and vendor owners for their own endpoints).

## API contracts (admin only)

| Method | Path | Purpose |
|--------|------|---------|
| GET    | `/api/v1/admin/webhooks`                    | list endpoints |
| POST   | `/api/v1/admin/webhooks`                    | create endpoint |
| GET    | `/api/v1/admin/webhooks/{id}`               | get endpoint |
| PUT    | `/api/v1/admin/webhooks/{id}`               | update endpoint |
| GET    | `/api/v1/admin/webhooks/{id}/deliveries`    | recent deliveries |
| POST   | `/api/v1/admin/webhooks/{id}/rotate-secret` | rotate signing secret (returns plaintext ONCE) |
| GET    | `/api/v1/admin/webhooks/{id}/subscriptions` | list event subscriptions |
| POST   | `/api/v1/admin/webhooks/{id}/subscriptions` | add subscription |
| GET    | `/api/v1/admin/webhooks/event-types`        | enumerate routable events |

## External integrations

`ExternalIntegrationProvider` is the abstraction. `ExternalIntegrationRegistry`
is the runtime lookup. Phase 8.5 ships ONLY placeholder providers (one
per type). Real connectors (ERP/CRM/Accounting/Marketing) are explicitly
out of scope.

## Events emitted (via outbox)

| Event type | Trigger |
|------------|---------|
| `webhook.queued`         | delivery row enqueued from outbox |
| `webhook.delivered`      | 2xx response from endpoint |
| `webhook.failed`         | transient failure; retry scheduled |
| `webhook.dead_letter`    | attempts exhausted |
| `webhook.secret_rotated` | new secret provisioned |

Self-emitted `webhook.*` events are short-circuited by `WebhookConsumer`
to prevent loops.
