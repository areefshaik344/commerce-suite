# Phase 6.5 — Blocker Resolution Report

Date: 2026-06-07 · Scope: BLOCKERs + HIGH findings that gate Phase 7.

## B-01 · Order FSM divergence
- **Root cause:** Java enums and PG `order_status` / `vendor_order_status` types used `CREATED` and `CLOSED` instead of spec-mandated `PENDING_PAYMENT` and `COMPLETED`; `PARTIALLY_DELIVERED` was missing entirely.
- **Files:** `orders/entity/OrderStatus.java`, `orders/entity/VendorOrderStatus.java`, `orders/service/OrderRollupService.java`, `test/.../OrderStateMachineTest.java`.
- **Migration:** `V011__phase6_5_blocker_resolution.sql` — `ALTER TYPE ... ADD VALUE` for `PENDING_PAYMENT`, `PARTIALLY_DELIVERED`, `COMPLETED` on both enums. Legacy `CREATED`/`CLOSED` retained as transitional aliases (back-compat for V010 rows and the existing FE constants) until Phase 7 payments rewires creation.
- **Code changes:** Added `PENDING_PAYMENT`, `PARTIALLY_DELIVERED`, `COMPLETED` enum values; updated `ALLOWED` transition map; updated `isTerminal()` and `isCancellable()` to include new states.
- **Status:** ✅ RESOLVED.

## B-02 · DB-level FSM enforcement
- **Root cause:** Service-layer FSM (`OrderStateMachine`, `VendorOrderStateMachine`) was the only safeguard; direct DB writes could bypass it.
- **Migration:** `V011` adds `fn_assert_child_transition()` (BEFORE UPDATE OF status on `vendor_orders`) and `fn_rollup_parent_order()` (AFTER UPDATE OF status on `vendor_orders`), per `ORDER_FSM.md §3` / §5. Transition allowed-set mirrors `VendorOrderStatus.ALLOWED`.
- **Status:** ✅ RESOLVED.

## B-03 · Idempotency infrastructure
- **Root cause:** No `idempotency_keys` table; `CheckoutService` stored the key on the session row only; `POST /orders`, `/refunds`, `/payments` had no enforcement.
- **Migration:** `V011` creates `public.idempotency_keys` with `UNIQUE (actor_id, endpoint, idempotency_key)`, `expires_at`, and grants.
- **Code:** New package `common/idempotency/` — `IdempotencyRecord` (entity), `IdempotencyRecordRepository` (with `deleteExpired`), `IdempotencyService` (`replayOrExecute` performs SHA-256 hash + replay + 409 on payload mismatch + race-loser recovery, plus `sweepExpired`).
- **Phase-7 wiring:** Payments, payouts, and `POST /orders` controllers will call `IdempotencyService.replayOrExecute(...)`. Checkout retains its existing dedup until that wiring lands.
- **Status:** ✅ INFRASTRUCTURE RESOLVED; per-endpoint adoption deferred to Phase 7 controllers (which do not yet exist).

## B-04 · Coupon concurrency / TOCTOU
- **Root cause:** `resolve()` was `@Transactional(readOnly=true)` and used `findByCodeIgnoreCase` + plain `COUNT`; concurrent applies could exceed usage caps.
- **Code:** `CouponRepository.findByCodeForUpdate` (`@Lock(PESSIMISTIC_WRITE)`); `CouponService.resolve` promoted to write `@Transactional` and now uses the locked finder.
- **Migration:** `V011` adds `uq_coupon_usage_open` partial unique index on `(coupon_id, user_id, checkout_id) WHERE committed=false` (also closes M-11) and a `coupons.used_count` counter column for future Phase-7 redis-based fast-path counters.
- **Status:** ✅ RESOLVED.

## B-05 · Financial soft-delete
- **Root cause:** `@SQLDelete` / `@SQLRestriction` on financial and audit entities allowed `repository.delete()` to silently hide records.
- **Code:** Removed from `Order`, `VendorOrder`, `OrderItem`, `OrderStatusHistory`, `RefundRequest`, `RefundTransaction`, `RefundItem`, `ReturnRequest`, `ReturnItem`, `InventoryReservation`, `TrackingEvent`.
- **Migration:** `V011` `REVOKE DELETE` on the same tables from the `authenticated` role.
- **Status:** ✅ RESOLVED.

## B-06 · Event publication safety
- **Root cause:** Domain events were published inside `@Transactional` methods, risking ghost events on rollback.
- **Approach (B):** *After-commit event publication.* New `common/event/AfterCommitEventPublisher` defers `publishEvent` via `TransactionSynchronizationManager.registerSynchronization(...).afterCommit`. All listeners authored from Phase 7 onward MUST use `@TransactionalEventListener(phase = AFTER_COMMIT)` — currently zero listeners exist (CC-01), so the runtime risk is theoretical, but the safety mechanism is in place.
- **Documentation:** Added to `BACKEND_READINESS.md` and `ARCHITECTURE.md` event-bus subsection (decision: after-commit, not outbox; outbox is a Phase-8 candidate).
- **Status:** ✅ INFRASTRUCTURE RESOLVED; migrate existing emit sites to `AfterCommitEventPublisher` as listeners come online in Phase 7.

## B-07 · Orphan `users` package
- **Code:** `backend/src/main/java/com/commercesuite/users/` directory deleted. Canonical `user/` package retained.
- **Status:** ✅ RESOLVED.

## HIGH findings addressed
- **H-02 OrderRollupService:** Rewritten per `ORDER_FSM.md §3` rollup table; uses non-cancelled denominator; emits `PARTIALLY_DELIVERED` and `COMPLETED`/`RETURNED` correctly.
- **L-02, L-03, M-09:** Closed alongside B-05 (`TrackingEvent`, `OrderStatusHistory`, `InventoryReservation` are now append-only).
- **M-11:** Closed by `uq_coupon_usage_open` partial unique index in V011.
- **H-03, H-04 (idempotency-dependent):** Infrastructure delivered (B-03). Controller-level adoption is a Phase-7 task because the order-placement / payment endpoints undergo redesign there.
- **H-01, H-09, H-10 (RBAC):** Tracked for Phase 7 hardening; these affect Cart/Profile/Address/AdminProduct controllers and do not gate the Payment module. Recorded in the audit delta.
- **H-05, H-06, H-07, H-08:** Tracked for Phase 7; explicitly deferred because they touch checkout pricing, inventory locking, and per-vendor allocation that Phase 7 will refactor when payments + payouts wire in. See delta.

## Tests
- `OrderStateMachineTest` extended to cover `PENDING_PAYMENT`, `PARTIALLY_DELIVERED`, `COMPLETED`.
- `IdempotencyServiceTest` covers TTL constant + key-format validator.
- Backend `compileJava` + `compileTestJava` clean; targeted unit suites (`*StateMachineTest`, `*IdempotencyServiceTest`, `*PricingEngineTest`) pass.
- Integration tests requiring Testcontainers remain skipped in the sandbox (no Docker); they are unchanged by this phase.