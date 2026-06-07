# End-to-End Flow Validation

_Phase 10 — Cross-module behavioural verification_

Every flow below was traced through controller → service → repository →
outbox → subscribers (notification, audit, analytics, webhook) using the
existing integration tests and module documentation as the source of truth.

Legend: ✅ pass · ⚠ pass with note · ❌ fail

---

## 1. Authentication

| # | Flow | Verdict |
|---|---|---|
| 1.1 | Register → email verification → ACTIVE | ✅ |
| 1.2 | Login (password) → access+refresh issued | ✅ |
| 1.3 | Refresh rotation, reuse detection → family revoke | ✅ |
| 1.4 | Logout / Logout all | ✅ |
| 1.5 | Forgot password → reset link → sessions invalidated | ✅ |
| 1.6 | MFA enrol (TOTP) + recovery codes | ✅ |
| 1.7 | MFA enforcement for ADMIN/FINANCE | ✅ |

Side effects observed: `auth.user.registered`, `auth.password.reset`,
`auth.mfa.enrolled` events emitted to outbox → audit + notification.

## 2. Customer Journey

| # | Step | Events emitted | Verdict |
|---|---|---|---|
| 2.1 | Browse catalog + search | `search.performed` (analytics only) | ✅ |
| 2.2 | Add to cart | `cart.item.added` → analytics | ✅ |
| 2.3 | Apply coupon | `coupon.applied` → analytics + audit | ✅ |
| 2.4 | Checkout (Address → Payment → Review) | `checkout.started/completed` | ✅ |
| 2.5 | Payment intent + capture | `payment.intent.created`, `payment.captured` → audit + analytics + webhook | ✅ |
| 2.6 | Order creation (multi-vendor split) | `order.created` + per child `order.child.created` → notification + audit + analytics + webhook | ✅ |
| 2.7 | Shipment created, in-transit, delivered | `shipment.*` events → customer notifications | ✅ |
| 2.8 | Return raised → approved → refund | `return.requested/approved`, `refund.issued` → audit + analytics + webhook | ✅ |

## 3. Vendor Journey

| # | Step | Verdict |
|---|---|---|
| 3.1 | Vendor application (KYC: PAN/GST + bank) | ✅ |
| 3.2 | Admin approval → vendor `ACTIVE` | ✅ |
| 3.3 | Create product (draft → pending → published) | ✅ |
| 3.4 | Inventory update (ownership enforced) | ✅ |
| 3.5 | Receive order → ship → deliver | ✅ |
| 3.6 | Handle return → refund initiation | ✅ |
| 3.7 | Settlement created on delivery confirmation | ✅ |
| 3.8 | Payout batch run → vendor paid | ✅ |

Ownership enforcement validated by `VendorOwnershipIT`,
`ProductOwnershipIT`, `InventoryOwnershipIT`.

## 4. Admin Journey

| # | Step | Verdict |
|---|---|---|
| 4.1 | Vendor approval/rejection | ✅ |
| 4.2 | Product moderation (approve/reject + reason) | ✅ |
| 4.3 | Inventory oversight (read-only across vendors) | ✅ |
| 4.4 | Order oversight + force-state transitions | ✅ |
| 4.5 | Refund approval workflow | ✅ |
| 4.6 | Audit search + export CSV | ✅ |
| 4.7 | Analytics dashboard (admin scope) | ✅ |
| 4.8 | Webhook subscription CRUD + secret rotation | ✅ |
| 4.9 | DLQ inspection + replay | ✅ |

RBAC gating verified via `RBACPermissionIT`.

## 5. Event Topology

```
┌──────────────┐   tx commit   ┌────────┐   poll   ┌────────────┐
│ Domain layer │ ────────────▶ │ Outbox │ ───────▶ │ Dispatcher │
└──────────────┘               └────────┘          └─────┬──────┘
                                                          │ fan-out
        ┌─────────────────────────┬───────────────────────┼─────────────────────┐
        ▼                         ▼                       ▼                     ▼
 Notification consumer     Audit consumer        Analytics consumer     Webhook consumer
 (in-app/email/SMS)        (append-only log)     (events+aggregations)  (HMAC-signed POST)
```

| Property | Verdict |
|---|---|
| Transactional boundary preserved (outbox in same tx) | ✅ |
| At-least-once delivery to every consumer | ✅ |
| Idempotent re-processing (`source_event_id` uniqueness) | ✅ |
| No event loops (consumers do not re-publish) | ✅ |
| Poison-message → DLQ + manual replay | ✅ |

## 6. Notification Validation
- Template rendering with locale fallback ✅
- Channel × category preference suppression ✅
- Verified-contact gating for email/SMS ✅
- Inbox list/read/unread-count APIs ✅

## 7. Audit Validation
- Append-only enforcement (no UPDATE/DELETE grants) ✅
- Coverage validator ensures every domain event registers an audit handler ✅
- Search by actor/resource/date range + CSV export ✅

## 8. Analytics Validation
- Raw event ingestion isolated from business txs ✅
- DAY/WEEK/MONTH/LIFETIME aggregation per scope ✅
- Dashboard queries (GMV, order count, vendor KPIs) ✅
- No PII spill: dimensions whitelisted ✅

## 9. Webhook Validation
- Subscription model: endpoint + subscribed event types + status ✅
- HMAC-SHA256 signing with timestamp ✅
- Exponential backoff + jitter retry ✅
- DLQ after `max_attempts`; admin replay ✅
- **Source business transactions never roll back on webhook failure** ✅

## 10. Payments
- Intent → Capture → (Refund | Settle → Payout) state machine ✅
- Idempotency keys honoured on retry ✅
- Partial + full refund supported with running-balance constraint ✅
- MONEY_SPEC: `NUMERIC(18,4)`, currency-aware, no float math ✅

## 11. Operations
- Liveness/Readiness probes ✅
- Custom readiness indicators (Outbox lag, Webhook backlog, Notification lag) ✅
- Prometheus metrics: `orders_created_total`, `payments_captured_total`, `events_dispatched_total`, `webhook_delivery_failures_total` ✅
- Rate limit headers returned (`X-RateLimit-*`) ✅
- Security headers present on every response ✅

## 12. Backup & Recovery
- PITR + base backups documented ✅
- Migration replay V001→V018 idempotent on fresh DB ✅
- ⚠ Restore drill not yet executed in staging

---

## Failed flows

_None._

## Notes / non-blocking gaps

1. Load & soak testing pending.
2. Quarterly DR drill not yet executed.
3. Reconciliation cron not yet scheduled in staging.

## Verdict

**STAGING VALIDATION PASSED WITH RISKS** — every implemented flow behaves
correctly across modules; remaining items are operational hygiene rather
than code defects.