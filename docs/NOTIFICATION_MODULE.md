# Notification Module — Phase 8.2

Builds on Phase 8.1 (durable outbox + notification preferences).
No external providers are integrated yet; EMAIL/SMS/PUSH strategies log only.

## Architecture

```
Business event ──▶ outbox_events ──▶ OutboxDispatcher ──▶ OutboxDispatchEvent
                                                          │
                                                          ▼
                                            NotificationConsumer
                                                          │
                                                          ▼
                                   NotificationService.createAndDispatch
                                                          │
         ┌──── PreferenceEvaluator ───── TemplateRenderer ─┤
         ▼                                                 ▼
  (suppress if no channels)                  Notification + N×NotificationDelivery
                                                          │
                                                          ▼
                                          NotificationDeliveryService
                                              │
                                              ▼
                              InApp / Email / SMS / Push strategies
```

## States (`NotificationStateMachine`)

`CREATED → QUEUED → PROCESSING → DELIVERED | FAILED → QUEUED (retry) | EXPIRED`
`CREATED → SUPPRESSED` (terminal). `DELIVERED/SUPPRESSED/EXPIRED` are terminal.

## Database (`V014__notification_module.sql`)

| table | purpose |
|-------|---------|
| `notification_templates` | code + channel + locale + version, `{{var}}` substitution |
| `notification_batches` | optional grouping by source event |
| `notifications` | one row per recipient per event (soft-deletable) |
| `notification_deliveries` | per-(notification, channel) delivery + attempts |
| `notification_status_history` | append-only audit trail |

RLS: owners can read/update their own notifications; templates are read-only for authenticated users; status history is append-only (`REVOKE UPDATE, DELETE`).

## Template system

`TemplateRenderer` does minimal `{{var}}` substitution — no external engine.
Templates are versioned per `(code, channel, locale)`; the latest active version wins.

## Preference evaluation

`NotificationPreferenceEvaluator` filters requested channels against the
user's `notification_preferences` matrix. `AUTH` category over `IN_APP`
can never be suppressed (security inbox). When zero channels remain,
the notification is persisted as `SUPPRESSED` and a `notification.suppressed`
event is published to the outbox.

## Event consumers (`NotificationConsumer`)

Listens to `OutboxDispatchEvent` and routes by `event_type`:

| event_type | category |
|------------|----------|
| `auth.user.registered`, `auth.user.logged_in`, `auth.password.changed`, `auth.password.reset_requested`, `auth.email.verified` | `AUTH` |
| `vendor.applied`, `vendor.approved`, `vendor.rejected`, `product.approved`, `product.rejected`, `payout.completed` | `VENDOR` |
| `order.created`, `order.delivered`, `order.cancelled` | `ORDER` |
| `payment.captured` | `PAYMENT` |
| `refund.processed` | `REFUND` |

`userId` is inferred from payload keys (`userId`, `customerId`, `vendorUserId`, `vendorId`) or from the aggregate id when `aggregateType = USER`.

## Retry strategy

Per-delivery exponential backoff: `30s · 2^(attempt-1)` capped at 1 h, max
5 attempts (configurable on the row). After max attempts the delivery
stays `FAILED` with `next_attempt_at = null` (dead-lettered).

## API

| verb | path | purpose |
|------|------|---------|
| `GET`  | `/api/v1/notifications?page&size` | inbox list |
| `GET`  | `/api/v1/notifications/unread-count` | unread badge |
| `POST` | `/api/v1/notifications/{id}/mark-read` | mark single read |
| `POST` | `/api/v1/notifications/mark-all-read` | mark all read |
| `GET`  | `/api/v1/notification-templates` | admin-only template list |

## Outbox events emitted

`notification.created`, `notification.queued`, `notification.delivered`,
`notification.failed`, `notification.suppressed`, `notification.read`.

## Out of scope (later sprints)

- Real email / SMS / push providers
- Webhook delivery
- Analytics persistence
- Digest / quiet-hours / locale negotiation