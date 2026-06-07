# Analytics Module — Phase 8.4

_Closes BLOCKER R-05 from `PLATFORM_INTEGRATION_AUDIT.md`._

Builds on:
- Phase 8.1 — `docs/OUTBOX_ARCHITECTURE.md`
- Phase 8.2 — `docs/NOTIFICATION_MODULE.md`
- Phase 8.3 — `docs/AUDIT_MODULE.md`

---

## 1. Architecture

Analytics is a **read-side downstream consumer**. It NEVER writes into
or blocks any transactional flow (Orders, Payments, Inventory, Checkout).

```
 ┌──────────────┐   commit    ┌─────────────────┐   poll    ┌──────────────────┐
 │ Domain TX    ├────────────▶│ outbox_events   ├──────────▶│ OutboxDispatcher │
 │ (Order, Pay) │             │ (durable)       │           └────────┬─────────┘
 └──────────────┘             └─────────────────┘                    │ in-proc fan-out
                                                                     ▼
                                                       ┌──────────────────────────┐
                                                       │ AnalyticsConsumer        │
                                                       │   classifies + persists  │
                                                       │   (REQUIRES_NEW + swallow│
                                                       │    all exceptions)       │
                                                       └──────────┬───────────────┘
                                                                  │
                          ┌───────────────────────────────────────┼───────────────┐
                          ▼                                       ▼               ▼
               analytics_events (raw)           analytics_aggregations   dashboard_metrics
                                                  (DAY/WEEK/MONTH/         (latest value
                                                   LIFETIME × scope)        per scope)
```

**Isolation guarantee.** `AnalyticsService.record` and
`AnalyticsAggregator.applyEvent` run in `Propagation.REQUIRES_NEW`. The
consumer catches `Exception` and only logs — analytics failures cannot
propagate into the dispatcher transaction.

---

## 2. Categories

`AnalyticsCategory` (V016 enum):
`CUSTOMER · VENDOR · CATALOG · INVENTORY · CHECKOUT · ORDER · PAYMENT · REFUND · PAYOUT · SYSTEM`

---

## 3. Event ingestion

`AnalyticsEventClassifier` is the single source of truth mapping
`outbox_events.event_type` to:

1. an `AnalyticsCategory` (whether the event is analytics-relevant), and
2. one or more KPI `metric_code`s.

Idempotency is enforced by the UNIQUE constraint on
`analytics_events.source_event_id`. Replaying the same outbox row is
safe — the consumer short-circuits via `findBySourceEventId`.

Supported event types (Phase 8.4):

| event_type | category | metrics |
|---|---|---|
| `auth.user_registered` | CUSTOMER | customer.registrations |
| `auth.user_logged_in` | CUSTOMER | customer.logins |
| `vendor.applied` | VENDOR | vendor.applications |
| `vendor.approved` | VENDOR | vendor.approvals |
| `product.created` | CATALOG | catalog.products_created |
| `product.approved` | CATALOG | catalog.products_approved |
| `product.viewed` | CATALOG | catalog.product_views |
| `checkout.started` | CHECKOUT | checkout.started |
| `checkout.completed` | CHECKOUT | checkout.completed |
| `order.created` | ORDER | order.created, order.gmv |
| `order.delivered` | ORDER | order.delivered |
| `order.cancelled` | ORDER | order.cancelled |
| `payment.captured` | PAYMENT | payment.captured.{count,amount} |
| `payment.failed` | PAYMENT | payment.failed |
| `refund.requested` | REFUND | refund.requested |
| `refund.completed` | REFUND | refund.completed.{count,amount} |
| `commission.accrued` | PAYOUT | commission.accrued |
| `settlement.released` | PAYOUT | settlement.released.amount |
| `payout.completed` | PAYOUT | payout.completed.{count,amount} |
| `notification.delivered` | SYSTEM | notification.delivered |
| `audit.record_created` | SYSTEM | audit.records |

`analytics.*` events are explicitly skipped to avoid feedback loops.

---

## 4. Aggregation model

`AnalyticsAggregator.applyEvent` rolls each event into the following
buckets for every relevant metric:

| period | bucket boundary |
|---|---|
| DAY | UTC midnight |
| WEEK | Monday UTC 00:00 |
| MONTH | 1st of month UTC 00:00 |
| LIFETIME | epoch → 9999-12-31 |

For each `(metric_code, period, bucket_start, scope, scope_id)` the
row's `value_count`, `value_sum`, `value_min`, `value_max` are updated.

- **Counter metrics** contribute `+1` per event.
- **Amount metrics** (`AnalyticsEventClassifier.AMOUNT_METRICS`) contribute the event's `amount` field.

On every LIFETIME bucket update the matching `dashboard_metrics` row is
upserted with the canonical value (sum for amount metrics, count
otherwise) and `analytics.aggregation_completed` is emitted via the
durable outbox.

`AnalyticsRollupService` is a placeholder for back-fill / snapshot
capture jobs and stays disabled by default (`analytics.rollup.enabled=false`).

---

## 5. KPI catalog

Seeded in `V016__analytics_module.sql` (table `analytics_metrics`):

| code | type | category |
|---|---|---|
| customer.registrations | COUNTER | CUSTOMER |
| customer.logins | COUNTER | CUSTOMER |
| customer.active_users | GAUGE | CUSTOMER |
| vendor.applications | COUNTER | VENDOR |
| vendor.approvals | COUNTER | VENDOR |
| vendor.active | GAUGE | VENDOR |
| catalog.products_created | COUNTER | CATALOG |
| catalog.products_approved | COUNTER | CATALOG |
| catalog.product_views | COUNTER | CATALOG |
| checkout.started | COUNTER | CHECKOUT |
| checkout.completed | COUNTER | CHECKOUT |
| checkout.conversion | RATIO | CHECKOUT |
| order.created / order.delivered / order.cancelled | COUNTER | ORDER |
| order.gmv | SUM | ORDER |
| payment.captured.{count,amount} / payment.failed | COUNTER / SUM | PAYMENT |
| refund.requested / refund.completed.{count,amount} | COUNTER / SUM | REFUND |
| commission.accrued / settlement.released.amount | SUM | PAYOUT |
| payout.completed.{count,amount} | COUNTER / SUM | PAYOUT |
| notification.delivered / audit.records | COUNTER | SYSTEM |

Computed KPIs (`KpiService`):

- `checkoutConversion = checkout.completed / checkout.started`
- `refundRate = refund.completed.count / order.created`
- `aov = order.gmv / order.created`

All return `0` when the denominator is zero. All computations are pure
reads — `KpiService` never mutates state.

---

## 6. Dashboard metrics

Three scopes (`DashboardScope`):

| scope | scope_id | populated by |
|---|---|---|
| ADMIN | `null` | every classified event |
| VENDOR | `event.vendorId` | events carrying `vendorId` |
| CUSTOMER | `event.customerId` or `userId` | events carrying customer |

`dashboard_metrics` is a denormalised read model (`(scope, scope_id,
metric_code)` unique). All dashboards query it directly — no heavy
joins against raw events.

---

## 7. Database (V016)

| table | append-only | RLS |
|---|---|---|
| `analytics_events` | yes | admin only |
| `analytics_metrics` | catalog | read all authenticated |
| `analytics_aggregations` | upsert (count/sum) | admin OR owning vendor/customer |
| `analytics_snapshots` | yes | admin OR owning vendor/customer |
| `dashboard_metrics` | upsert | admin OR owning vendor/customer |

`REVOKE INSERT, UPDATE, DELETE … FROM authenticated` on all five
tables — only `service_role` (the consumer + aggregator) can write.

---

## 8. API contracts

Admin (`hasRole('ADMIN')`):

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/admin/analytics/overview | Platform KPIs + computed conversion / refund / AOV |
| GET | /api/v1/admin/analytics/revenue?period=DAY&from=&to= | order.gmv series |
| GET | /api/v1/admin/analytics/orders?period=DAY&from=&to= | order.created series |
| GET | /api/v1/admin/analytics/vendors?period=DAY&from=&to= | vendor.approvals series |

Vendor (`hasRole('VENDOR')`, scoped to own `userId`):

| Method | Path | Description |
|---|---|---|
| GET | /api/v1/vendor/analytics/overview | Vendor KPIs |
| GET | /api/v1/vendor/analytics/orders?period=DAY | Vendor order count series |
| GET | /api/v1/vendor/analytics/revenue?period=DAY | Vendor revenue series |

Default range is `now - 30d`. Ownership is enforced both at the
controller (filters by `vendorId = currentUser`) and the database
(RLS policy `analytics_agg_read`).

---

## 9. Domain events

Emitted via durable outbox (`AnalyticsEvents`):

| event_type | aggregate | when |
|---|---|---|
| `analytics.event_recorded` | ANALYTICS | every raw event ingested |
| `analytics.aggregation_completed` | ANALYTICS | LIFETIME bucket finalised |
| `analytics.dashboard_updated` | ANALYTICS | dashboard_metrics upsert |

These never feed back into analytics (`event_type.startsWith("analytics.")`
is short-circuited in the consumer).

---

## 10. Retention

Declared in `AnalyticsRetentionPolicy` (no purge job in Phase 8.4):

| data | default | financial categories |
|---|---|---|
| Raw events | 1 year | 7 years (ORDER, PAYMENT, REFUND, PAYOUT) |
| Aggregations | 5 years | — |
| Snapshots | 7 years | — |

---

## 11. Final validation

- Analytics consumes outbox events only.
- Analytics failures never impact business flows (REQUIRES_NEW + try/catch).
- Dashboard reads are O(1) — `(scope, scope_id, metric_code)` unique lookup.
- KPI calculations are deterministic, division-by-zero safe.
- Ownership enforced at controller AND row-level security.
- No direct service coupling — classifier + outbox are the only entry point.