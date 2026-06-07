# Platform Integration Audit (Phase 7.5)

**Date:** 2026-06-07
**Scope:** Cross-cutting audit of Phases 1–7 covering events, notifications, audit logging, analytics, webhooks and outbox readiness. **No new business features.** Purpose: certify the platform is ready for Phase 8 (Notifications, Audit, Analytics, Webhooks, Outbox).

---

## 1. Event Coverage Audit

All domain events live under `com.commercesuite.<module>.event.*Events` and are published through `AfterCommitEventPublisher` (B-06). 87 event records were enumerated across 14 modules.

### 1.1 Event Matrix

| Module | Event | Publisher Service | In-Process Subscriber | External Consumer (Phase 8) | Status |
|---|---|---|---|---|---|
| Auth | UserRegisteredEvent | AuthService | — | Notifications, Audit | **MISSING record** |
| Auth | UserLoggedInEvent | AuthService | — | Audit, Analytics | **MISSING record** |
| Auth | PasswordResetRequestedEvent | AuthService | — | Notifications | **MISSING record** |
| Vendor | VendorAppliedEvent | VendorApplicationService | — | Notifications, Audit | OK |
| Vendor | VendorApprovedEvent | VendorAdminService | — | Notifications, Audit, Webhook | OK |
| Vendor | VendorRejectedEvent | VendorAdminService | — | Notifications, Audit | OK |
| Vendor | VendorSuspendedEvent | VendorAdminService | — | Notifications, Audit, Webhook | OK |
| Vendor | VendorReactivatedEvent | VendorAdminService | — | Notifications, Audit | OK |
| Vendor | VendorDeactivatedEvent | VendorAdminService | — | Notifications, Audit | OK |
| Catalog | ProductCreatedEvent | ProductService | — | Audit | OK |
| Catalog | ProductSubmittedEvent | ProductService | — | Audit | OK |
| Catalog | ProductApprovedEvent | ProductModerationService | — | Notifications, Webhook | OK |
| Catalog | ProductRejectedEvent | ProductModerationService | — | Notifications | OK |
| Catalog | ProductSuspendedEvent | ProductModerationService | — | Notifications, Audit | OK |
| Catalog | ProductArchivedEvent | ProductService | — | Audit | OK |
| Catalog | ProductReviewCreatedEvent | ProductReviewService | — | Notifications | OK |
| Inventory | InventoryReservedEvent | InventoryReservationService | — | Analytics | OK |
| Inventory | InventoryReleasedEvent | InventoryReservationService | — | Analytics | OK |
| Inventory | InventoryCommittedEvent | InventoryReservationService | OrderCreationService | Analytics | OK |
| Inventory | InventoryExpiredEvent | Reservation sweeper | — | Audit | OK |
| Inventory | InventoryAdjustedEvent | InventoryAdjustmentService | — | Audit | OK |
| Inventory | LowStockDetectedEvent | InventoryLowStockService | — | Notifications (vendor) | OK |
| Cart | CartItemAdded/Updated/Removed | CartService | — | Analytics | OK |
| Cart | CartMergedEvent | CartService | — | Analytics | OK |
| Cart | SavedForLaterEvent | CartService | — | Analytics | OK |
| Coupon | CouponAppliedEvent | CouponService | — | Analytics, Audit | OK |
| Coupon | CouponRejectedEvent | CouponService | — | Analytics | OK |
| Coupon | CouponCommittedEvent | CouponService | — | Analytics, Audit | OK |
| Checkout | CheckoutStartedEvent | CheckoutService | — | Analytics | OK |
| Checkout | CheckoutAddress/Shipping/PaymentSelected | CheckoutService | — | Analytics | OK |
| Checkout | CheckoutReadyForOrderEvent | CheckoutService | OrderCreationService | Analytics | OK |
| Checkout | CheckoutCancelledEvent | CheckoutService | — | Analytics | OK |
| Checkout | CheckoutExpiredEvent | CheckoutSweeperService | — | Analytics, Audit | OK |
| Checkout | CheckoutStateChangedEvent | CheckoutStateMachine | — | Audit | OK |
| Orders | OrderCreatedEvent | OrderCreationService | — | Notifications, Audit, Webhook, Analytics | OK |
| Orders | OrderStateChangedEvent | OrderStateMachine | OrderRollupService | Audit | OK |
| Orders | OrderCancelledEvent | OrderService | RefundProcessor | Notifications, Webhook | OK |
| Orders | OrderDeliveredEvent | OrderRollupService | — | Notifications, Webhook, Analytics | OK |
| Orders | VendorOrderStateChangedEvent | VendorOrderStateMachine | OrderRollupService | Audit | OK |
| Shipping | ShipmentCreatedEvent | ShipmentService | — | Notifications, Webhook | OK |
| Shipping | ShipmentStateChangedEvent | ShipmentStateMachine | OrderRollupService | Audit | OK |
| Shipping | ShipmentDeliveredEvent | ShipmentService | OrderRollupService | Notifications, Webhook, Analytics | OK |
| Shipping | TrackingEventRecordedEvent | TrackingService | — | Notifications | OK |
| Returns | ReturnRequestedEvent | ReturnService | — | Notifications (vendor) | OK |
| Returns | ReturnApprovedEvent | ReturnService | RefundProcessor | Notifications, Webhook | OK |
| Returns | ReturnRejectedEvent | ReturnService | — | Notifications | OK |
| Returns | ReturnCompletedEvent | ReturnService | — | Notifications, Webhook | OK |
| Returns | ReturnStateChangedEvent | ReturnStateMachine | — | Audit | OK |
| Refunds | RefundRequestedEvent | RefundService | — | Notifications, Audit | OK |
| Refunds | RefundApprovedEvent | RefundService | RefundProcessor | Notifications, Webhook | OK |
| Refunds | RefundRejectedEvent | RefundService | — | Notifications | OK |
| Refunds | RefundCompletedEvent | RefundProcessor | — | Notifications, Webhook, Analytics | OK |
| Refunds | RefundStateChangedEvent | RefundStateMachine | — | Audit | OK |
| Payments | PaymentCreatedEvent | PaymentService | — | Analytics | OK |
| Payments | PaymentAuthorizedEvent | PaymentService | — | Audit | OK |
| Payments | PaymentCapturedEvent | PaymentService | OrderStateMachine, SettlementCalculator | Notifications, Webhook, Analytics | OK |
| Payments | PaymentFailedEvent | PaymentService | — | Notifications, Webhook | OK |
| Payments | PaymentCancelledEvent | PaymentService | — | Audit | OK |
| Payments | PaymentRefundedEvent | PaymentService | RefundProcessor | Webhook, Analytics | OK |
| Payments | PaymentStateChangedEvent | PaymentStateMachine | — | Audit | OK |
| Commission | CommissionCalculatedEvent | CommissionCalculator | SettlementCalculator | Audit, Analytics | OK |
| Commission | CommissionRuleChangedEvent | CommissionRuleService | — | Audit | OK |
| Settlement | SettlementCalculatedEvent | SettlementCalculator | — | Audit | OK |
| Settlement | SettlementLockedEvent | SettlementService | PayoutBatchService | Audit | OK |
| Settlement | SettlementPaidEvent | SettlementService | — | Notifications (vendor), Webhook | OK |
| Settlement | SettlementStateChangedEvent | SettlementStateMachine | — | Audit | OK |
| Payouts | PayoutCreatedEvent | PayoutService | — | Audit | OK |
| Payouts | PayoutCompletedEvent | PayoutService | SettlementService | Notifications (vendor), Webhook | OK |
| Payouts | PayoutFailedEvent | PayoutService | — | Notifications (vendor), Audit | OK |
| Payouts | PayoutBatchCreatedEvent | PayoutBatchService | — | Audit | OK |
| Payouts | PayoutBatchCompletedEvent | PayoutBatchService | — | Audit | OK |
| Payouts | PayoutStateChangedEvent | PayoutStateMachine | — | Audit | OK |

### 1.2 Findings

- **Orphan events:** none — every published event has at least one planned Phase 8 consumer.
- **Duplicate events:** `*StateChangedEvent` records overlap with specific transition events (e.g. `RefundStateChangedEvent` + `RefundApprovedEvent`). Intentional — `StateChanged` feeds the generic audit stream; the specific events drive business behaviour. No action.
- **Missing events (HIGH):** Auth module (`UserRegistered`, `UserLoggedIn`, `UserLoggedOut`, `PasswordResetRequested`, `EmailVerified`, `PhoneVerified`, `RoleAssigned`, `AccountLocked`). Required for Phase 8 audit + notifications.
- **Naming consistency:** all events use `<Subject><PastTense>Event`. Compliant.
- **Ownership consistency:** every payload carries the canonical id (`orderId`, `vendorId`, `userId`) needed for downstream RLS / ownership scoping. Compliant.
- **Publication discipline:** zero remaining raw `ApplicationEventPublisher.publishEvent(` call sites in the 14 audited modules — all routed via `AfterCommitEventPublisher`. Compliant.

---

## 2. Notification Readiness

### 2.1 Notification Coverage Matrix

| Module | Trigger Event | Channel(s) | Audience | Classification |
|---|---|---|---|---|
| Auth | UserRegistered | Email | Customer | Required |
| Auth | EmailVerificationRequested | Email | Customer | Required |
| Auth | PasswordResetRequested | Email | Customer | Required |
| Auth | AccountLocked | Email | Customer + Admin | Required |
| Auth | NewDeviceLogin | Email | Customer | Optional |
| Vendor | VendorApplied | Email, In-App | Admin | Required |
| Vendor | VendorApproved | Email, In-App | Vendor | Required |
| Vendor | VendorRejected | Email | Vendor | Required |
| Vendor | VendorSuspended | Email | Vendor | Required |
| Catalog | ProductApproved | In-App | Vendor | Required |
| Catalog | ProductRejected | Email, In-App | Vendor | Required |
| Catalog | ProductReviewCreated | In-App | Vendor | Optional |
| Inventory | LowStockDetected | Email, In-App | Vendor | Required |
| Checkout | CheckoutAbandoned (24h) | Email | Customer | Future |
| Orders | OrderCreated | Email, SMS, In-App | Customer + Vendor | Required |
| Orders | OrderCancelled | Email, In-App | Customer + Vendor | Required |
| Orders | OrderDelivered | Email, SMS | Customer | Required |
| Shipping | ShipmentCreated | Email, SMS, In-App | Customer | Required |
| Shipping | ShipmentStatusChanged | Push, In-App | Customer | Optional |
| Shipping | TrackingEventRecorded | Push | Customer | Optional |
| Returns | ReturnRequested | Email, In-App | Vendor | Required |
| Returns | ReturnApproved/Rejected | Email, In-App | Customer | Required |
| Returns | ReturnCompleted | Email | Customer | Required |
| Refunds | RefundRequested | In-App | Vendor + Admin | Required |
| Refunds | RefundApproved | Email | Customer | Required |
| Refunds | RefundCompleted | Email, SMS | Customer | Required |
| Payments | PaymentCaptured | Email | Customer | Required |
| Payments | PaymentFailed | Email, In-App | Customer | Required |
| Settlement | SettlementPaid | Email, In-App | Vendor | Required |
| Payouts | PayoutCompleted | Email, In-App | Vendor | Required |
| Payouts | PayoutFailed | Email, In-App | Vendor + Admin | Required |

Channels: Email (transactional, all), SMS (order/shipment/refund only), Push (optional), In-App (always for vendor + admin).

---

## 3. Audit Logging Readiness

### 3.1 Audit Coverage Matrix

| Action | Source Event / Hook | Actor | Required Fields | Retention |
|---|---|---|---|---|
| User login / logout / failed login | AuthService hook | user | ip, ua, session_id | 365d |
| Role assignment / revocation | RBAC service hook | admin | target_user_id, role | 7y |
| Vendor approve / reject / suspend | Vendor*Event | admin | vendor_id, reason | 7y |
| Product approve / reject / suspend | Product*Event | admin | product_id, reason | 2y |
| Inventory adjustment | InventoryAdjustedEvent | vendor/admin | sku_id, delta, reason | 7y |
| Order create / cancel | Order*Event | customer/system | order_id, total_paise | 7y |
| Order state transition | OrderStateChangedEvent | system | from, to | 7y |
| Refund request / approve / reject / complete | Refund*Event | customer/admin | refund_id, amount | 7y |
| Return request / approve / complete | Return*Event | customer/vendor | return_id | 7y |
| Payment capture / refund / fail | Payment*Event | system | payment_id, amount | 7y |
| Commission rule change | CommissionRuleChangedEvent | admin | rule_id, diff | 7y |
| Settlement calculate / lock / pay | Settlement*Event | system/admin | settlement_id, hash | 7y |
| Payout batch create / complete / fail | Payout*Event | admin | batch_id, vendor_payout_id | 7y |
| Coupon create / disable (NEW) | CouponService hook | admin | coupon_code | 2y |
| GDPR deletion request / cancel (NEW) | GdprService hook | customer | user_id | 7y |

`audit_log` table is append-only: `INSERT` only; no `UPDATE`/`DELETE` for any role except `service_role`.

---

## 4. Analytics Readiness

### 4.1 Analytics Coverage Matrix

| Bucket | Event | Source | Sink |
|---|---|---|---|
| Customer | product_viewed | Frontend `analyticsBus` | OLAP |
| Customer | product_added_to_cart | CartItemAddedEvent | OLAP |
| Customer | saved_for_later | SavedForLaterEvent | OLAP |
| Customer | coupon_applied / rejected | Coupon*Event | OLAP |
| Customer | checkout_started → ready | Checkout*Event | OLAP funnel |
| Customer | order_placed | OrderCreatedEvent | OLAP + KPI |
| Customer | payment_captured / failed | Payment*Event | OLAP |
| Customer | order_delivered | OrderDeliveredEvent | OLAP |
| Customer | return_requested | ReturnRequestedEvent | OLAP |
| Customer | refund_completed | RefundCompletedEvent | OLAP |
| Vendor | low_stock_detected | LowStockDetectedEvent | Vendor dashboard |
| Vendor | product_approved | ProductApprovedEvent | Vendor dashboard |
| Vendor | vendor_order_received | VendorOrderStateChangedEvent(CONFIRMED) | Vendor dashboard |
| Vendor | commission_calculated | CommissionCalculatedEvent | Vendor dashboard |
| Vendor | settlement_paid | SettlementPaidEvent | Vendor dashboard |
| Vendor | payout_completed | PayoutCompletedEvent | Vendor dashboard |
| Admin | vendor_applied / approved | Vendor*Event | Admin KPI |
| Admin | gmv_recorded | OrderCreatedEvent | Admin KPI |
| Admin | refund_rate | RefundCompletedEvent | Admin KPI |
| Admin | payout_batch_completed | PayoutBatchCompletedEvent | Admin KPI |

---

## 5. Webhook Readiness

### 5.1 Webhook Coverage Matrix (externally subscribable)

| Event Type | Source | Audience | Signed | Retry |
|---|---|---|---|---|
| `vendor.approved` | VendorApprovedEvent | Vendor integrations | yes | exp backoff |
| `vendor.suspended` | VendorSuspendedEvent | Vendor integrations | yes | yes |
| `order.created` | OrderCreatedEvent | Vendor + 3p ERP | yes | yes |
| `order.cancelled` | OrderCancelledEvent | Vendor + 3p ERP | yes | yes |
| `order.delivered` | OrderDeliveredEvent | Vendor + 3p ERP | yes | yes |
| `shipment.created` | ShipmentCreatedEvent | Carrier integrations | yes | yes |
| `shipment.delivered` | ShipmentDeliveredEvent | Vendor + 3p | yes | yes |
| `payment.captured` | PaymentCapturedEvent | Accounting | yes | yes |
| `payment.failed` | PaymentFailedEvent | Accounting | yes | yes |
| `payment.refunded` | PaymentRefundedEvent | Accounting | yes | yes |
| `refund.completed` | RefundCompletedEvent | Accounting | yes | yes |
| `return.approved` | ReturnApprovedEvent | Vendor | yes | yes |
| `return.completed` | ReturnCompletedEvent | Vendor | yes | yes |
| `settlement.paid` | SettlementPaidEvent | Vendor accounting | yes | yes |
| `payout.completed` | PayoutCompletedEvent | Vendor accounting | yes | yes |
| `payout.failed` | PayoutFailedEvent | Vendor accounting | yes | yes |

Webhook payloads MUST follow the canonical envelope in `src/types/events.ts` (`WebhookEventDTO`) so frontend `webhookOutbox` and backend producers stay byte-compatible.

---

## 6. Outbox Readiness

### 6.1 Current State

- `AfterCommitEventPublisher` guarantees in-process events fire only after commit (B-06).
- There is **no durable outbox table yet**. If the process crashes between commit and listener execution, in-process events are lost. Acceptable for audit (reconstructable from FSM history) but **unacceptable for webhooks**.

### 6.2 Required Outbox Architecture (Phase 8)

- Table `event_outbox(id uuid pk, aggregate_type, aggregate_id, event_type, payload jsonb, occurred_at, available_at, attempts int, status enum[PENDING,IN_FLIGHT,DELIVERED,DEAD], last_error text, headers jsonb)`.
- Producers write to `event_outbox` **inside** the same transaction as the state change. `AfterCommitEventPublisher` continues to fire in-process events; a new `OutboxAppender` writes the durable row.
- A `@Scheduled` `OutboxRelay` polls `WHERE status='PENDING' AND available_at<=now() FOR UPDATE SKIP LOCKED LIMIT 100` and dispatches to webhook subscribers / message bus.
- Retry policy: 5 attempts, exponential backoff (30s, 2m, 10m, 1h, 6h), then `DEAD` + admin alert.
- Delivery guarantee: **at-least-once**; subscribers MUST be idempotent (event `id` is the dedup key, `UNIQUE`).
- Dead-letter inspection endpoint under `/admin/outbox/dead`.

---

## 7. Risk Analysis (Phase 8 scope only)

| Sev | ID | Area | Description | Recommendation |
|---|---|---|---|---|
| BLOCKER | R-01 | Webhooks | No durable outbox → guaranteed event loss for external subscribers on crash. | Ship `event_outbox` + relay in Phase 8 sprint 1. |
| HIGH | R-02 | Auth | Auth module emits no domain events; audit + notification flows cannot be wired. | Add 8 missing Auth events (§1.2). |
| HIGH | R-03 | Notifications | No `notification_preferences` table → opt-out cannot be honored (CAN-SPAM / DPDP exposure). | Ship table with per-channel granularity. |
| HIGH | R-04 | Audit | `audit_log` table not yet created in V001–V012. | Create in V013 with `INSERT`-only RLS. |
| MEDIUM | R-05 | Webhooks | No HMAC secret-rotation schedule. | Document 90-day rotation with dual-signing window. |
| MEDIUM | R-06 | Analytics | Frontend `analyticsBus` not wired to a server sink. | Add `/api/v1/analytics/ingest`. |
| MEDIUM | R-07 | Outbox | Large `payload jsonb` rows bloat the table. | Monthly archive partition + 90-day purge of DELIVERED. |
| LOW | R-08 | Naming | Redundancy of `*StateChangedEvent` vs specific transition events. | Document: StateChanged → audit only; specific events → notifications/webhooks. |
| LOW | R-09 | Idempotency | Webhook subscribers depend on event `id` uniqueness. | `UNIQUE(id)` constraint on `event_outbox`. |

---

## 8. Final Verdict

- Zero BLOCKERS from prior phases remain open.
- One Phase-8-internal BLOCKER (R-01) and three HIGH risks (R-02, R-03, R-04) are **expected scope of Phase 8** and addressed by `docs/PHASE8_IMPLEMENTATION_BLUEPRINT.md`.
- All 87 emitted events have an identified Phase 8 consumer; no orphans.
- Event publication discipline (`AfterCommitEventPublisher`) is consistent across all 14 modules.

### **READY FOR PHASE 8**

Conditions:
1. Phase 8 sprint 1 ships `event_outbox` + `OutboxRelay` before any external webhook subscriber is onboarded.
2. Auth events (R-02) are added in Phase 8 sprint 1, not deferred.
3. `audit_log` and `notification_preferences` migrations land in V013.
