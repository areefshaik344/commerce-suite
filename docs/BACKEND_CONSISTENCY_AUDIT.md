# Backend Consistency Audit Report

**Audited:** `/dev-server/backend` vs `/dev-server/docs/` (frozen specs)  
**Date:** 2025-07-10  
**Auditor:** Automated architectural consistency audit  
**Scope:** All modules under `com.commercesuite.*`; migrations V001–V010

---

## Executive Summary

| Severity | Count |
|----------|-------|
| BLOCKER  | 7     |
| HIGH     | 10    |
| MEDIUM   | 11    |
| LOW      | 7     |
| **Total**| **35**|

> **Verdict (original, 2025-07-10): 🔴 FIX BEFORE PHASE 7**
>
> **Phase 6.5 update (2026-06-07): 🟢 SAFE FOR PHASE 7** — see
> `docs/BLOCKER_RESOLUTION_REPORT.md` and
> `docs/BACKEND_CONSISTENCY_AUDIT_DELTA.md`. All BLOCKERs resolved; all
> HIGH findings closed or downgraded to MEDIUM with concrete Phase-7
> follow-ups.
>
> Seven blockers must be resolved before any production traffic: the `idempotency_keys` table is entirely absent (PAYMENT_IDEMPOTENCY.md §2), Order FSM state names diverge from the frozen spec across both DB and Java, the coupon global-limit check has a TOCTOU race under concurrent load, financial entities carry `@SQLDelete` making audit trails defeatable, the DB FSM-assertion trigger (`fn_assert_child_transition`, `trg_rollup_parent_order`) is never created, event publication fires *inside* transactions instead of AFTER_COMMIT, and the `users` package contains orphan stubs that shadow the canonical `user` package.

---

## BLOCKER Findings

---

### B-01 · orders · Order FSM — `PENDING_PAYMENT` and `COMPLETED` states missing from Java enum and DB migration

**Files:**
- `orders/entity/VendorOrderStatus.java` lines 4–5
- `orders/entity/OrderStatus.java` lines 4–6
- `db/migration/V010__orders_shipping_returns_refunds.sql` lines (CREATE TYPE order_status / vendor_order_status)

**Description:**  
`ORDER_FSM.md §2` defines the authoritative child-order FSM as starting at `PENDING_PAYMENT` and terminating at `COMPLETED`. The Java enum `VendorOrderStatus` uses `CREATED` as the initial state and `CLOSED` as a terminal alias instead of `COMPLETED`. Likewise, the parent `OrderStatus` enum omits `PENDING_PAYMENT` (replacing it with `CREATED`) and uses `CLOSED` in place of the spec's `COMPLETED`. The PG `order_status` and `vendor_order_status` types in V010 match the Java enums (both wrong). Every `ORDER_FSM.md` rollup table references `PENDING_PAYMENT` and `COMPLETED` as unambiguous states.

Additionally, `OrderStatus` is missing `PARTIALLY_DELIVERED` (defined in rollup table row 6 of ORDER_FSM.md §3). The rollup service incorrectly maps `anyDelivered → PARTIALLY_SHIPPED` instead of `PARTIALLY_DELIVERED`.

**Impact:** Child orders are created with state `CREATED`, which is not a recognised state in the frozen FSM spec. Any cross-team integration (payment gateway callbacks, analytics, webhook consumers) using the spec's `PENDING_PAYMENT` constant will be broken at runtime. The `COMPLETED` → `CLOSED` rename breaks the rollup rule "All children `COMPLETED` or `REFUNDED`".

**Severity: BLOCKER**

**Recommendation:** Rename Java enum values and PG enum values: `CREATED→PENDING_PAYMENT` for child orders at creation time (or introduce `PENDING_PAYMENT` before `CONFIRMED`); rename `CLOSED→COMPLETED` in both `VendorOrderStatus` and `OrderStatus`; add `PARTIALLY_DELIVERED` to `OrderStatus`; add a Flyway ALTER TYPE migration. Update `VendorOrderStatus.isCancellable()` and `OrderRollupService.rollup()` to reference the corrected names.

---

### B-02 · orders · DB FSM trigger `fn_assert_child_transition` / `trg_rollup_parent_order` never created

**Files:**
- `db/migration/V010__orders_shipping_returns_refunds.sql` (entire file — no CREATE FUNCTION / CREATE TRIGGER)
- `docs/ORDER_FSM.md` §5 ("DB CHECK constraint: status transitions must be in the `child_order_fsm` allowed-set, enforced by `fn_assert_child_transition()` trigger") and §3 ("Rollup is recomputed by a DB trigger `trg_rollup_parent_order` AFTER every `child_orders` UPDATE of `status`")

**Description:**  
The spec mandates two database-level safeguards: (a) a trigger `fn_assert_child_transition` that rejects illegal FSM transitions at the DB layer, and (b) a trigger `trg_rollup_parent_order` that recomputes parent order status after every child status update. Neither trigger function nor trigger binding appears anywhere in V010 or any other migration. `OrderRollupService.rollup()` is only invoked from a handful of service call-sites; a direct DB write (e.g. via admin tooling, migration data fix, or a future microservice) will silently skip the rollup.

**Impact:** The DB is the last-resort safety net per the spec. Without these triggers, illegal state transitions can be committed and parent rollup can become stale with no compensating mechanism.

**Severity: BLOCKER**

**Recommendation:** Add a V011 migration that creates `fn_assert_child_transition()` (using the allowed-set from the spec) and `trg_rollup_parent_order()`. Alternatively, add a CHECK constraint referencing a lookup table for allowed transitions.

---

### B-03 · checkout / orders · `idempotency_keys` table absent — PAYMENT_IDEMPOTENCY.md §2 fully unimplemented

**Files:**
- `db/migration/V009__cart_checkout_module.sql` — no `idempotency_keys` table
- `db/migration/V010__orders_shipping_returns_refunds.sql` — no `idempotency_keys` table
- `common/util/IdempotencyKey.java` lines 1–9 — only validates format, never persists
- `checkout/service/CheckoutService.java` lines 52–56 — replay check reads `checkout_sessions.idempotency_key`, not a central table
- `orders/controller/OrderController.java` — no `Idempotency-Key` header accepted on `POST /api/v1/orders`

**Description:**  
`PAYMENT_IDEMPOTENCY.md §2` mandates a dedicated `idempotency_keys` table scoped to `(actor_id, endpoint, idempotency_key)` with `request_hash`, `response_status`, `response_body`, and `expires_at`. The spec also requires advisory-lock dedup, `Idempotent-Replay: true` header on replays, and `409 IDEMPOTENCY_KEY_CONFLICT` on hash mismatch. None of this infrastructure exists. The checkout service stores the key only on the `checkout_sessions` row (a partial, checkout-only workaround). `POST /orders`, `POST /payments/intents`, `POST /refunds`, and `POST /payouts` have no idempotency enforcement at all.

**Impact:** Duplicate payment intents, duplicate orders, and duplicate refunds under network retries — all listed as P0 business risks in `PAYMENT_IDEMPOTENCY.md §7`.

**Severity: BLOCKER**

**Recommendation:** Add V011 migration for `idempotency_keys` table per spec. Implement an `IdempotencyFilter` or AOP interceptor that intercepts all listed endpoints, computes SHA-256 of the request body, performs advisory-lock dedup, and persists response. Wire `Idempotency-Key` header extraction on `POST /api/v1/orders`, `POST /api/v1/refunds`, and `POST /api/v1/payments/intents`.

---

### B-04 · coupon · Global and per-user usage limits are not serialised — TOCTOU race

**Files:**
- `coupon/service/CouponService.java` lines 40–46 (`resolve` method)
- `coupon/repository/CouponUsageRepository.java` — no `@Lock` annotation

**Description:**  
`CouponService.resolve()` reads the usage count via `countByCouponId` / `countByCouponIdAndUserId` (plain `SELECT COUNT`) and then, in a later `@Transactional` call, inserts a `CouponUsage` row. There is no `SELECT … FOR UPDATE` on the `coupons` row, no `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the count query, and no unique constraint in V009 on `(coupon_id)` that would enforce the global cap atomically. Under concurrent requests two users can simultaneously read count=N, both pass the cap check, and both record usage, exceeding `usage_limit_total`.

**Impact:** Coupon budget overruns; direct financial loss. BUSINESS_RULES.md BR-CHK-004 classifies per-user and global caps as hard limits.

**Severity: BLOCKER**

**Recommendation:** Add a `SELECT … FOR UPDATE` on the `coupons` row at the start of `resolve()` (or use optimistic locking with `@Version` on `Coupon.usedCount`). Add a DB-level partial unique constraint or a CHECK trigger. Consider a Redis-based atomic counter for high-throughput scenarios.

---

### B-05 · orders / refunds / returns · Financial entities carry `@SQLDelete` — audit trail defeatable

**Files:**
- `orders/entity/Order.java` line 14: `@SQLDelete`
- `orders/entity/VendorOrder.java` line 13: `@SQLDelete`
- `orders/entity/OrderItem.java` line 13: `@SQLDelete`
- `orders/entity/OrderStatusHistory.java` line 14: `@SQLDelete`
- `refunds/entity/RefundRequest.java` line 14: `@SQLDelete`
- `refunds/entity/RefundTransaction.java` line 14: `@SQLDelete`
- `refunds/entity/RefundItem.java` line 13: `@SQLDelete`
- `returns/entity/ReturnRequest.java` line 14: `@SQLDelete`
- `returns/entity/ReturnItem.java` line 13: `@SQLDelete`

**Description:**  
`BUSINESS_RULES.md BR-USER-003` states that on GDPR deletion the audit log must be *preserved*. Financial records (orders, vendor orders, order items, refund requests, refund transactions, return requests) are immutable records of financial transactions; soft-deleting them via `@SQLDelete` means any code path that calls `repository.delete(entity)` silently marks them as deleted, making them invisible to `@SQLRestriction`-filtered queries. `ORDER_FSM.md` and `MONEY_SPEC.md §4` treat orders as append-only entities with ledger semantics — no delete operation should ever exist.

**Impact:** Accidental or malicious call to `repository.delete()` on an order or refund makes the record disappear from all standard queries. The reconciliation invariant in `MONEY_SPEC.md §4` cannot be verified against hidden rows. Regulatory non-compliance.

**Severity: BLOCKER**

**Recommendation:** Remove `@SQLDelete` and `@SQLRestriction` from all financial entities (`Order`, `VendorOrder`, `OrderItem`, `OrderStatusHistory`, `RefundRequest`, `RefundTransaction`, `RefundItem`, `ReturnRequest`, `ReturnItem`). These entities must never be deleted — not soft, not hard. Add REVOKE DELETE grants in migrations.

---

### B-06 · all modules · Events published inside transactions — no `@TransactionalEventListener(AFTER_COMMIT)`

**Files:**
- `orders/service/OrderCreationService.java` line 149: `events.publishEvent(new OrderCreatedEvent(...))`
- `checkout/service/CheckoutService.java` lines ~88, ~110, ~130: `events.publishEvent(...)`
- `inventory/service/InventoryReservationService.java` lines ~76, ~113, ~147, ~176
- `coupon/service/CouponService.java` line 77
- (all other service files using `ApplicationEventPublisher`)
- Zero files match `@TransactionalEventListener` or `@EventListener` (confirmed by grep)

**Description:**  
All domain events (`OrderCreatedEvent`, `InventoryReservedEvent`, `CheckoutStartedEvent`, etc.) are published via `events.publishEvent(...)` inside `@Transactional` methods. Spring's default `ApplicationEventPublisher` fires listeners synchronously within the same transaction. If the transaction later rolls back (e.g. a constraint violation after the `publishEvent` call), the event has already been observed by any synchronous listener, potentially triggering notifications, webhooks, or audit entries for transactions that never committed. The spec (`ARCHITECTURE.md §key-decision-8`) requires at-least-once delivery with AFTER_COMMIT semantics.

**Impact:** Ghost events for rolled-back transactions; duplicate side-effects on retry; outbox webhook consumers acting on non-committed state.

**Severity: BLOCKER**

**Recommendation:** Annotate all `@EventListener` handlers (once implemented) with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. Or use Spring's `ApplicationEventPublisher` with `TransactionalApplicationEventMulticaster`. Consider a transactional outbox table for cross-process reliability.

---

### B-07 · users · Orphan `users` package partially duplicates `user` package

**Files:**
- `users/` package: contains `users/address/` subdirectory with DTOs — confirmed by `ls`
- `user/` package: canonical implementation with `AddressController`, `AddressService`, `Address` entity, full DTO set
- `users/address/` contains address DTOs that shadow or partially duplicate `user/dto/AddressDto.java` and `user/dto/UpsertAddressRequest.java`

**Description:**  
Two packages exist: `com.commercesuite.user` (canonical, full implementation) and `com.commercesuite.users` (stub with `users/address/` DTO classes). The `users` package is not referenced by any controller, service, or Spring component scan configuration found in the audit. However, its presence creates ambiguity: future developers may add code to `users` believing it is the active package. The divergence in package naming also means any Spring component scan that uses `com.commercesuite.users` instead of `com.commercesuite.user` will silently produce a shadow bean.

**Impact:** Build-time confusion, potential component-scan misconfiguration in future modules, duplicated DTO maintenance.

**Severity: BLOCKER** (classification: structural integrity; will silently break if referenced in a Spring `@ComponentScan` override)

**Recommendation:** Delete `com.commercesuite.users` entirely. If the `users/address/` DTOs contain intentional additions, merge them into `com.commercesuite.user.dto`.

---

## HIGH Findings

---

### H-01 · all controllers · No `@PreAuthorize` on any controller — RBAC enforcement relies solely on AOP aspect

**Files:**
- Every controller file — confirmed `grep -rL @PreAuthorize` returns all 21 controller files
- `rbac/service/PermissionAspect.java` lines 16–24
- `rbac/service/RequiresPermission.java`

**Description:**  
Zero controllers use Spring Security's `@PreAuthorize`. RBAC enforcement is delegated entirely to a custom `@RequiresPermission` + `PermissionAspect`. While the aspect is functionally equivalent for annotated methods, controllers such as `CartController`, `ProfileController`, `AddressController`, `VendorController`, `CategoryController`, and `BrandController` have *no `@RequiresPermission` annotation either*, relying only on JWT filter authentication. Unauthenticated or wrong-role callers can reach any endpoint in those controllers.

**Impact:** Broken access control on cart, profile, address, vendor, and catalog admin endpoints. OWASP A01 violation. ARCHITECTURE.md §8 mandates server-side re-validation of every permission.

**Severity: HIGH**

**Recommendation:** Apply `@RequiresPermission` (or migrate to `@PreAuthorize("hasAuthority('PERMISSION')")`) to every mutating endpoint. Enumerate at minimum: all `POST/PUT/DELETE` endpoints on `CartController`, `ProfileController`, `AddressController`, `AdminVendorController`, `AdminProductController`, `AdminInventoryController`, `CouponController`.

---

### H-02 · orders · `OrderRollupService` — missing `PARTIALLY_DELIVERED` and wrong `COMPLETED` mapping

**Files:**
- `orders/service/OrderRollupService.java` lines 33–51
- `docs/ORDER_FSM.md` §3 rollup table

**Description:**  
(1) The spec defines `PARTIALLY_DELIVERED` (≥1 child `DELIVERED`, ≥1 child not yet `DELIVERED` and not cancelled). The rollup service maps `anyDelivered + not-allDelivered` to `PARTIALLY_SHIPPED` (line 47), which is incorrect — `PARTIALLY_SHIPPED` is for ≥1 child in transit.  
(2) The spec `COMPLETED` terminal state (all children `COMPLETED` or `REFUNDED`) is mapped in rollup to `RETURNED` because `VendorOrderStatus.REFUNDED` is included in the `allReturned` check (lines 35–36). There is no rollup path that produces `COMPLETED`/`CLOSED` correctly when all children are `REFUNDED`.  
(3) `allReturned && anyReturned` is logically redundant — if `allReturned` is true, `anyReturned` is always true. The guard collapses and `RETURNED` fires prematurely when only some children are `CANCELLED`.

**Impact:** Parent order shows wrong status to customers; analytics, notifications, and settlement triggers that listen to `PARTIALLY_DELIVERED` or `COMPLETED` events never fire.

**Severity: HIGH**

**Recommendation:** Rewrite rollup logic directly from the spec table in ORDER_FSM.md §3. Use non-cancelled children as the denominator for "all" predicates. Map `anyDelivered + not-allDelivered-non-cancelled → PARTIALLY_DELIVERED`.

---

### H-03 · checkout · Checkout cancel idempotency key not stored — replay semantics broken

**Files:**
- `checkout/service/CheckoutService.java` `cancel()` method lines ~124–138
- `checkout/repository/CheckoutSessionRepository.java` line 16 — only `findByUserIdAndIdempotencyKey` for `start`

**Description:**  
`POST /checkout/{id}/cancel` accepts `Idempotency-Key` header (correctly) but the service only uses the terminal-state early-return as a dedup mechanism (`if s.getStatus().isTerminal() return ...`). The idempotency key for the cancel call is never persisted. A client that retries cancel with the same key but a different body hash would receive a silently different response rather than a `409 IDEMPOTENCY_KEY_CONFLICT` per spec. Moreover the cancel key is not stored to `checkout_sessions` (only the start key is).

**Impact:** Idempotency contract broken for cancel path; potential for double-release of inventory on retry storms.

**Severity: HIGH**

**Recommendation:** Once the central `idempotency_keys` table (B-03) exists, route both `start` and `cancel` through the same idempotency middleware.

---

### H-04 · orders · `POST /api/v1/orders` has no `Idempotency-Key` enforcement

**Files:**
- `orders/controller/OrderController.java` lines 25–30 (`create` endpoint)
- `orders/service/OrderCreationService.java` lines ~63–67 (existence check only)
- `docs/PAYMENT_IDEMPOTENCY.md` §1 — `POST /orders` is listed as a required idempotency endpoint

**Description:**  
The order creation endpoint accepts no `Idempotency-Key` header. The service provides a weak dedup check: `orderRepo.findByCheckoutId(checkoutId)` returns an existing order if one already exists for the same checkout. However, this is not equivalent to the spec's idempotency contract: (a) it is not keyed on the actor's explicit idempotency key, (b) it stores no `request_hash` to detect payload divergence, (c) it returns no `Idempotent-Replay: true` header, (d) it provides no `409` on hash mismatch.

**Impact:** Order duplication under concurrent retries that hit different DB replicas before the first order is committed; no replay header for client dedup.

**Severity: HIGH**

**Recommendation:** Accept `Idempotency-Key` header on `POST /orders`; route through central idempotency table.

---

### H-05 · inventory · `reserveForCustomer` bypasses `InventoryOwnershipGuard` without explicit ownership check

**Files:**
- `inventory/service/InventoryReservationService.java` lines 188–232 (`reserveForCustomer`)
- `inventory/service/InventoryOwnershipGuard.java` (referenced in `reserve()` line ~55 but not in `reserveForCustomer`)

**Description:**  
`reserveForCustomer` skips `ownership.requireOwnedVariant(variantId, actor)` and instead derives `vendorId` from `product.getVendorId()`. While this is correct for the customer use-case, there is no check that the `Product` is in `APPROVED`/`ACTIVE` status before reservation. A customer can reserve inventory for a `DRAFT`, `PENDING_REVIEW`, or `REJECTED` product. There is also no check that the variant belongs to the product (the variant's `productId` is retrieved but the relationship between `variantId` and `productId` is not cross-verified beyond `v.getProductId()`).

**Impact:** Customers can place reservations (and potentially orders) against products not yet approved for sale.

**Severity: HIGH**

**Recommendation:** Add `ProductStatus.APPROVED` check before calling `allocator.acquireVariantLock` in `reserveForCustomer`. Verify `variant.productId == product.id` explicitly.

---

### H-06 · checkout · `CheckoutService.recompute()` uses `CartItem.getUnitPricePaise()` — no price-drift detection

**Files:**
- `checkout/service/CheckoutService.java` `recompute()` method (lines ~165–185)
- `docs/BUSINESS_RULES.md` BR-CART-004 ("prices/availability re-validated against catalog; mismatches surface a diff dialog")

**Description:**  
`recompute()` uses `CartItem.unitPricePaise` (the price at the time the item was added to the cart) without re-fetching the current variant price from `ProductVariant`. This means a price change between cart-add and checkout start is silently ignored. BR-CART-004 classifies this as a P0 missing feature.

**Impact:** Customer charged the wrong price; potential regulatory issue under consumer protection law.

**Severity: HIGH**

**Recommendation:** During `start()` and `recompute()`, fetch `ProductVariant.pricePaise` for all cart items and compare against `CartItem.unitPricePaise`. If any diverge, throw a structured error or surface a diff.

---

### H-07 · inventory · `RESERVATION_FSM.md §3` requires per-variant advisory lock via SQL; code uses an in-process `InventoryAllocator`

**Files:**
- `inventory/service/InventoryReservationService.java` line ~58: `allocator.acquireVariantLock(variantId)`
- `docs/RESERVATION_FSM.md §3`: `SELECT pg_advisory_xact_lock(hashtext('variant:' || :variant_id))`

**Description:**  
The spec mandates `pg_advisory_xact_lock` (a PostgreSQL session-level advisory lock) to serialise concurrent reservations for the same variant. The code uses `InventoryAllocator.acquireVariantLock()` — an in-process mechanism. In a multi-instance deployment (horizontal scaling), two JVM instances can both acquire their local lock for the same variant simultaneously, defeating the oversell protection.

**Impact:** Oversell race condition in production under horizontal scale. The `FOR UPDATE` row lock on `inventory_items` provides partial protection but does not prevent phantom reads on non-row-locked paths.

**Severity: HIGH**

**Recommendation:** Replace `allocator.acquireVariantLock` with a `nativeQuery` call to `SELECT pg_advisory_xact_lock(hashtext('variant:' || :variantId::text))` inside the same transaction. The existing `PESSIMISTIC_WRITE` on `inventory_items` is a necessary second layer but cannot replace the advisory lock.

---

### H-08 · orders · `OrderCreationService` — vendor order `shippingPaise`, `discountPaise`, `taxPaise` hard-coded to `0L`

**Files:**
- `orders/service/OrderCreationService.java` lines ~101–107

**Description:**  
When building `VendorOrder`, all per-vendor financial fields (`discountPaise`, `shippingPaise`, `taxPaise`) are set to `0L`. The `MONEY_SPEC.md §4` reconciliation invariant requires `child_order.total = subtotal − discount + shipping + tax + surcharge`. The actual discount (coupon, if vendor-scoped) and shipping (per-vendor from checkout session) are captured on the parent checkout but never allocated down to vendor orders. The invariant `order.grand_total = Σ child_order.total` will fail immediately for any order with shipping or discount.

**Impact:** Financial reconciliation broken from day one. Settlement payouts computed from vendor order totals will be incorrect.

**Severity: HIGH**

**Recommendation:** Implement largest-remainder allocation (MONEY_SPEC.md §3) to distribute `discountPaise`, `shippingPaise`, and `taxPaise` from the checkout session across vendor orders proportional to subtotal share.

---

### H-09 · security · `PermissionAspect` is not applied to `@Transactional` proxy — aspect ordering risk

**Files:**
- `rbac/service/PermissionAspect.java`
- All service classes annotated with both `@Transactional` and `@RequiresPermission` (indirectly via service calls)

**Description:**  
`@RequiresPermission` is only placed on controller methods. The `PermissionAspect` uses `@Before("@annotation(rp)")` which intercepts at the controller layer. However, if any service method is ever annotated with `@RequiresPermission` (a natural future step), Spring's proxy ordering may apply the transaction aspect before the permission aspect, meaning the permission check runs inside an open transaction. This is a latent risk today and an actual risk when services are annotated.

**Impact:** Latent: transaction opened before auth check fails, wasting DB connections under DoS. Actual if services are annotated.

**Severity: HIGH**

**Recommendation:** Set `@Order(Ordered.HIGHEST_PRECEDENCE)` on `PermissionAspect` to ensure it fires before `@Transactional`. Document the ordering contract.

---

### H-10 · catalog · `ProductOwnershipGuard` not invoked in `AdminProductController` — admin bypass unchecked

**Files:**
- `catalog/controller/AdminProductController.java` — no `@RequiresPermission` and no ownership guard call
- `catalog/service/ProductOwnershipGuard.java` — exists and is used in `ProductService`

**Description:**  
`AdminProductController` has no `@RequiresPermission` annotation (confirmed: it appears in the no-PreAuthorize list). Any authenticated user (including CUSTOMER role) can call admin moderation endpoints. Combined with H-01 this means the moderation pipeline is entirely unprotected at the HTTP layer.

**Impact:** Any customer can approve/reject/archive products. CATALOG_MODULE.md requires `MODERATE_PRODUCTS` permission.

**Severity: HIGH**

**Recommendation:** Add `@RequiresPermission(Permissions.MODERATE_PRODUCTS)` to all methods in `AdminProductController`.

---

## MEDIUM Findings

---

### M-01 · orders · `OrderStatus` missing `PARTIALLY_DELIVERED` — rollup produces wrong parent state

**Files:** `orders/entity/OrderStatus.java` (see B-01); `orders/service/OrderRollupService.java` lines 45–47  
**Description:** Covered in B-01 and H-02 but noted separately: `PARTIALLY_DELIVERED` is a visible customer-facing state referenced in ORDER_FSM.md rollup table and ORDERS_SHIPPING_MODULE.md. Its absence means the parent order skips directly from `PARTIALLY_SHIPPED` to `DELIVERED`, showing no intermediate partially-delivered state.  
**Severity: MEDIUM** (customer UX / downstream analytics, not a data-integrity blocker by itself once B-01/H-02 are fixed)

---

### M-02 · checkout · `CheckoutSession` idempotency key stored on session row — dedup window not enforced

**Files:** `checkout/entity/CheckoutSession.java` line 58; `checkout/service/CheckoutService.java` lines 52–56  
**Description:** The spec requires a 24-hour dedup window (`PAYMENT_IDEMPOTENCY.md §3`). The current checkout start dedup reads from `checkout_sessions` — which has no TTL enforcement for the idempotency key itself. A session that has been `CONVERTED` or `CANCELLED` will not replay correctly because `findByUserIdAndIdempotencyKey` returns the old session and `CheckoutSessionDto.from()` is returned unconditionally, even if the session was cancelled days ago.  
**Severity: MEDIUM**

---

### M-03 · coupon · `CouponService.resolve()` is `@Transactional(readOnly=true)` but `recordApplication` opens a separate write TX

**Files:** `coupon/service/CouponService.java` lines 35, 66  
**Description:** `resolve()` and `recordApplication()` run in separate transactions. Any code that calls `resolve()` then `recordApplication()` (e.g. `CheckoutService`) does so across a gap where another TX can interleave. This is a secondary concurrency window on top of the TOCTOU race in B-04.  
**Severity: MEDIUM** (partially subsumed by B-04)

---

### M-04 · inventory · Sweeper processes reservations one-by-one in separate transactions — not batched per spec

**Files:** `inventory/service/InventoryReservationSweeper.java` lines 26–34  
**Description:** `RESERVATION_FSM.md §5` specifies a batch SQL `UPDATE … FROM due` in a single transaction for up to 500 rows. The sweeper calls `reservations.expire(r.getId())` in a loop, each opening a new transaction. Under load this produces 500 serial transactions instead of one batch, and the lack of `FOR UPDATE SKIP LOCKED` on the `findExpired` query means two sweeper instances can attempt to expire the same reservation.  
**Severity: MEDIUM**

---

### M-05 · all modules · `ApplicationEventPublisher.publishEvent` called twice for `READY_FOR_ORDER` events

**Files:** `checkout/service/CheckoutService.java` `selectPayment()` lines ~108–118  
**Description:** When all selections are present, `selectPayment()` transitions to `PAYMENT_SELECTED`, saves, publishes `CheckoutPaymentSelectedEvent`, then immediately transitions to `READY_FOR_ORDER`, saves again, and publishes `CheckoutReadyForOrderEvent` — all inside one `@Transactional` method. This results in two saves and two events. If the second save fails, the first event is already published (B-06). Additionally, two `sessionRepo.save(s)` calls on the same entity within one transaction may cause version-conflict on the optimistic lock.  
**Severity: MEDIUM**

---

### M-06 · orders · `OrderService.toDto()` is not `@Transactional(readOnly=true)` — called from non-transactional context

**Files:** `orders/service/OrderService.java` lines 87–96  
**Description:** `toDto()` is a public method with no `@Transactional` annotation. It calls `vendorOrderRepo.findByOrderId()` and `orderItemRepo.findByVendorOrderId()` — lazy associations within a method that may be called from a context where the session is closed. If called from a non-transactional context (e.g. event handler or test) this will throw `LazyInitializationException`.  
**Severity: MEDIUM**

---

### M-07 · vendor / catalog · `VendorController` and `AdminVendorController` have no `@RequiresPermission` — any authenticated user can onboard/approve

**Files:** `vendor/controller/VendorController.java` (no annotation); `vendor/controller/AdminVendorController.java` (no annotation)  
**Description:** VENDOR_MODULE.md requires `APPLY_AS_VENDOR` for onboarding and `MANAGE_VENDORS` / `APPROVE_VENDOR_APPLICATIONS` for admin actions. Neither controller carries any permission guard.  
**Severity: MEDIUM** (would be HIGH but vendor onboarding is gated by email-verification implicitly; admin approval is the critical path)

---

### M-08 · catalog · `CatalogSlug.java` — uniqueness of generated slug not enforced with DB unique constraint

**Files:** `catalog/service/CatalogSlug.java`; `db/migration/V007__catalog_module.sql`  
**Description:** V007 does not add a `UNIQUE` constraint on `products.slug`. `CatalogSlug` generates slugs from title but duplicate slugs under concurrent product creation (same vendor with same title variant) will silently succeed with non-unique slugs, breaking `GET /api/v1/products/{slug}` which presumably looks up by slug.  
**Severity: MEDIUM**

---

### M-09 · inventory · `InventoryReservation` has `@SQLDelete` — expired/released reservations should be append-only

**Files:** `inventory/entity/InventoryReservation.java` line 19  
**Description:** Reservation records are the authoritative FSM audit trail (`RESERVATION_FSM.md §2`). Soft-deleting them via `repository.delete()` would hide reservation history. There is no current code path that calls delete, but the annotation's presence is a trap for future developers. The `inventory_reservation_history` table is the correct audit store.  
**Severity: MEDIUM**

---

### M-10 · rbac · `Permissions.java` constant `PLACE_ORDER` used in `OrderController` but absent from ARCHITECTURE.md §8 permissions table

**Files:** `rbac/service/Permissions.java` line 6; `orders/controller/OrderController.java` line 28  
**Description:** `PLACE_ORDER` does not appear in the ARCHITECTURE.md RBAC matrix or in `docs/BUSINESS_RULES.md`. The closest mapped permission is implicit customer checkout. If this constant is not seeded into the `user_roles` / permission assignment tables, all customers will be denied order creation.  
**Severity: MEDIUM**

---

### M-11 · migration V009 · `coupon_usage` table has no unique constraint preventing duplicate uncommitted rows

**Files:** `db/migration/V009__cart_checkout_module.sql` (`coupon_usage` table definition)  
**Description:** There is no unique constraint on `(coupon_id, user_id, checkout_id)` in `coupon_usage`. Multiple un-committed coupon usage rows for the same user/checkout can accumulate (e.g. if the user applies and re-applies the same coupon). The `committed=false` rows are never cleaned up on checkout cancellation in the current code.  
**Severity: MEDIUM**

---

## LOW Findings

---

### L-01 · user · `Profile.java` — `@SQLDelete` SQL uses two bind params (`user_id = ? AND version = ?`) but `Profile` uses `userId` as `@Id`, not a surrogate `id`

**Files:** `user/entity/Profile.java` lines 24–25  
**Description:** Hibernate's `@SQLDelete` bind order is `[id, version]`. For `Profile` the `@Id` is `userId` (UUID). This is correct. However the `@SQLDelete` clause `WHERE user_id = ? AND version = ?` is correct only if the column name exactly matches. If JPA remaps `userId` differently this could silently fail. Low risk but worth verifying Hibernate's generated parameter order matches.  
**Severity: LOW**

---

### L-02 · shipping · `TrackingEvent` has `@SQLDelete` — tracking events should be append-only

**Files:** `shipping/entity/TrackingEvent.java` line 14  
**Description:** Tracking events are an immutable audit of package movement. Soft-deleting them defeats the tracking timeline. Same rationale as B-05 but lower risk because no current code path deletes tracking events.  
**Severity: LOW**

---

### L-03 · orders · `OrderStatusHistory` has `@SQLDelete` — FSM history must be append-only

**Files:** `orders/entity/OrderStatusHistory.java` line 14  
**Description:** ORDER_FSM.md §2 requires every transition to write an `order_status_transitions` row. Soft-deleting history rows defeats this audit requirement.  
**Severity: LOW**

---

### L-04 · checkout · `CheckoutSession` expiry check in `OrderCreationService` uses wall-clock `Instant.now()` — not injected `Clock`

**Files:** `orders/service/OrderCreationService.java` line ~62: `s.getExpiresAt().isBefore(Instant.now(clock))`  
**Description:** Actually uses the injected `Clock` correctly here. However `CheckoutService.selectPayment()` uses `Instant.now(clock)` for event timestamps only; the expiry check in `loadOwnedActive` → `fsm.requireActive` may use a hardcoded clock. Low risk but verify `CheckoutStateMachine.requireActive()` uses the injected `Clock`.  
**Severity: LOW**

---

### L-05 · catalog · `ProductService` — `productSnapshot` in `OrderSnapshotService` uses manual string concatenation with minimal escaping

**Files:** `orders/service/OrderSnapshotService.java` lines 33–38  
**Description:** Snapshot JSON is built via `"\"key\":" + json(value)` string concatenation. The `json()` helper only escapes `\` and `"`. Characters like `\n`, `\t`, `\r`, or Unicode control characters in product titles or vendor names will produce malformed JSON stored in `JSONB` columns, potentially causing parse errors when the snapshot is read back.  
**Severity: LOW** (fix: use `ObjectMapper` for snapshot serialisation)

---

### L-06 · migration V006-V008 · GRANT statements use `authenticated` role without schema qualification on V006/V007/V008

**Files:** `db/migration/V006__vendor_module.sql` GRANT section; `db/migration/V007__catalog_module.sql`; `db/migration/V008__inventory_module.sql`  
**Description:** V009 and V010 correctly use `public.table_name` in GRANT statements. V006–V008 use unqualified table names in GRANTs. If `search_path` is not set to `public` at migration time (e.g. running in a schema-isolated environment) the GRANTs may silently fail or apply to the wrong schema.  
**Severity: LOW**

---

### L-07 · common · `ApiResponse` missing `requestId` field — ARCHITECTURE.md requires `X-Request-Id` correlation

**Files:** `common/api/ApiResponse.java`  
**Description:** ARCHITECTURE.md §key-decision-8 specifies `requestId` as a mandatory correlation field in every response. The current `ApiResponse<T>` record carries only `{ success, data, message, timestamp }`. Absence of `requestId` makes distributed tracing across the API gateway impossible.  
**Severity: LOW**

---

## Cross-Cutting Issues

### CC-01 · Event Infrastructure — No event listener implementations exist

Confirmed by grep: there are zero `@EventListener` or `@TransactionalEventListener` implementations in the entire codebase. All `ApplicationEventPublisher.publishEvent(...)` calls go nowhere. The entire event-driven notification, audit, analytics, and webhook outbox pipeline described in ARCHITECTURE.md §in-process-event-bus is non-functional.

### CC-02 · `@Transactional` placement — Services correctly annotated; no `@Transactional` on controllers

All controller files are free of `@Transactional` (correct). All mutating service methods are correctly annotated. `@Transactional(readOnly=true)` is used on read methods in most services (correct).

### CC-03 · MONEY_SPEC compliance — No `double`/`float` found anywhere

All monetary arithmetic uses `long` for paise storage and `BigDecimal` for intermediate percentage math with explicit `RoundingMode.HALF_UP`. BPS (basis-point) arithmetic is used in `PricingEngine.applyBps()`. This is fully compliant with MONEY_SPEC.md.

### CC-04 · Soft-delete consistency — Non-financial entities correctly annotated

All non-financial entities (`Product`, `ProductVariant`, `Cart`, `CartItem`, `Vendor`, `User`, `Profile`, `Address`, `Coupon`, etc.) have `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")` which is correct per the spec. Only financial entities need remediation (B-05).

### CC-05 · Auditing consistency — `AuditingEntityListener` uniformly applied

All entities inspected carry `@EntityListeners(AuditingEntityListener.class)` with `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` — fully compliant with DATABASE_DESIGN.md auditing requirements.

### CC-06 · Optimistic locking — `@Version long version` present on all entities

Every entity inspected carries `@Version private long version` — compliant with the concurrency spec.

### CC-07 · API path convention — All controllers use `/api/v1/...` prefix

Confirmed correct. `ARCHITECTURE.md` and `SPRING_BOOT_IMPLEMENTATION_PLAN.md` require `/api/v1/` prefix; all `@RequestMapping` annotations follow this.

### CC-08 · `users` package is not empty — it contains `users/address/` DTOs

The `users/address/` subdirectory is confirmed to exist with DTO files. These are orphaned stubs (B-07). No services or controllers in the main codebase reference `com.commercesuite.users.*`.

---

## Summary Table

| ID   | Module           | Severity | Short description |
|------|------------------|----------|-------------------|
| B-01 | orders           | BLOCKER  | FSM state names `CREATED`/`CLOSED` diverge from spec `PENDING_PAYMENT`/`COMPLETED`; `PARTIALLY_DELIVERED` missing |
| B-02 | orders           | BLOCKER  | DB FSM triggers `fn_assert_child_transition` / `trg_rollup_parent_order` never created |
| B-03 | checkout/orders  | BLOCKER  | `idempotency_keys` table absent; PAYMENT_IDEMPOTENCY.md fully unimplemented |
| B-04 | coupon           | BLOCKER  | Global/per-user usage limit is a TOCTOU race — no SELECT FOR UPDATE |
| B-05 | orders/refunds   | BLOCKER  | Financial entities have `@SQLDelete` — audit trail defeatable |
| B-06 | all              | BLOCKER  | Events published inside transactions — no AFTER_COMMIT; no `@EventListener` implementations at all |
| B-07 | users            | BLOCKER  | Orphan `users` package with shadow DTOs |
| H-01 | all controllers  | HIGH     | Zero `@PreAuthorize`; many controllers have no permission guard at all |
| H-02 | orders           | HIGH     | `OrderRollupService` produces wrong parent states |
| H-03 | checkout         | HIGH     | Cancel idempotency key not stored; replay broken |
| H-04 | orders           | HIGH     | `POST /orders` has no `Idempotency-Key` header enforcement |
| H-05 | inventory        | HIGH     | `reserveForCustomer` does not check `APPROVED` product status |
| H-06 | checkout         | HIGH     | No price-drift detection on checkout start (BR-CART-004 P0) |
| H-07 | inventory        | HIGH     | In-process advisory lock not equivalent to `pg_advisory_xact_lock` under horizontal scale |
| H-08 | orders           | HIGH     | Vendor order `shippingPaise`/`discountPaise`/`taxPaise` hard-coded `0L`; reconciliation invariant broken |
| H-09 | security         | HIGH     | Aspect ordering risk — permission check may run inside open transaction |
| H-10 | catalog          | HIGH     | `AdminProductController` has no permission guard |
| M-01 | orders           | MEDIUM   | `PARTIALLY_DELIVERED` absent from `OrderStatus` enum |
| M-02 | checkout         | MEDIUM   | Session idempotency key dedup window not enforced |
| M-03 | coupon           | MEDIUM   | `resolve()` and `recordApplication()` in separate transactions |
| M-04 | inventory        | MEDIUM   | Sweeper not batched; no `SKIP LOCKED` — duplicate-expire risk |
| M-05 | checkout         | MEDIUM   | Double save + double event publish in `selectPayment()` |
| M-06 | orders           | MEDIUM   | `OrderService.toDto()` not `@Transactional` — LazyInit risk |
| M-07 | vendor           | MEDIUM   | `VendorController` / `AdminVendorController` have no permission guards |
| M-08 | catalog          | MEDIUM   | `products.slug` has no UNIQUE constraint in V007 |
| M-09 | inventory        | MEDIUM   | `InventoryReservation` has `@SQLDelete` — FSM audit trail defeatable |
| M-10 | rbac             | MEDIUM   | `PLACE_ORDER` permission not in spec permission matrix |
| M-11 | coupon           | MEDIUM   | `coupon_usage` has no unique constraint on `(coupon_id, user_id, checkout_id)` |
| L-01 | user             | LOW      | `Profile` `@SQLDelete` bind-parameter order edge case |
| L-02 | shipping         | LOW      | `TrackingEvent` has `@SQLDelete` |
| L-03 | orders           | LOW      | `OrderStatusHistory` has `@SQLDelete` |
| L-04 | checkout         | LOW      | Verify `CheckoutStateMachine.requireActive()` uses injected `Clock` |
| L-05 | orders           | LOW      | `OrderSnapshotService` uses string concat for JSON — escape incomplete |
| L-06 | migrations       | LOW      | V006–V008 GRANTs use unqualified table names |
| L-07 | common           | LOW      | `ApiResponse` missing `requestId` correlation field |

---

## Final Verdict

> ## 🔴 FIX BEFORE PHASE 7
>
> **7 BLOCKERs** must be resolved before any Phase 7 work begins:
>
> 1. **B-03** (idempotency table) and **B-04** (coupon race) represent direct financial loss vectors.
> 2. **B-01** (FSM state name divergence) will break every integration built against the frozen spec.
> 3. **B-02** (missing DB triggers) removes the last-resort safety net for illegal state transitions.
> 4. **B-05** (financial entity soft-delete) is a compliance and audit integrity issue.
> 5. **B-06** (event infrastructure absent) means the entire async pipeline (notifications, audit, webhooks) is non-functional.
> 6. **B-07** (orphan `users` package) is a structural land-mine.
>
> Additionally, **H-01** (zero RBAC enforcement on 12+ controllers) must be treated as a blocker from a security standpoint even though its discovery path is architectural rather than data-integrity.
