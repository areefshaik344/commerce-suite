# Pre-Backend Implementation Audit

Date: 2026-06-07
Scope: Final cross-document audit before initiating Spring Boot backend implementation.
Inputs reviewed:
- docs/ARCHITECTURE.md
- docs/BUSINESS_RULES.md
- docs/DATABASE_DESIGN.md
- docs/SPRING_BOOT_IMPLEMENTATION_PLAN.md
- docs/BACKEND_READINESS.md
- docs/GAP_ANALYSIS.md / GAP_ANALYSIS_DELTA.md
- src/types/*, src/lib/* (DTOs, FSM, ownership, idempotency, reservations)

Classification legend: **BLOCKER** must be fixed before backend work starts. **HIGH RISK** must be fixed before the affected module is implemented. **MEDIUM RISK** should be resolved during the relevant phase. **LOW RISK** can be tracked as known debt.

---

## 1. Backend Blockers

### B-01 — Money representation is not uniformly defined (BLOCKER)
- Where: `src/lib/pricing.ts`, `src/types/order.ts`, `src/types/payment.ts`, `DATABASE_DESIGN.md` (currency = integer rupees), `BUSINESS_RULES.md` (BR-CHK-*, BR-PAY-*).
- Issue: Frontend DTOs mix `number` rupees and floating subtotal/tax computations; DB design specifies integer minor units but does not state paise vs. rupees consistently across tables (`orders.total`, `settlement_ledger.amount`, `payouts.amount`). Plan does not pin a single `Money` value object.
- Impact: Off-by-100 errors, rounding drift across tax/commission/coupon/shipping, broken reconciliation between `child_orders` and parent `orders`.
- Recommendation: Freeze a single rule (integer paise everywhere across DB + API + DTO), update DTOs and pricing helpers to BigDecimal-equivalent, and add reconciliation invariant `sum(child_orders.total) == orders.total` enforced by trigger.

### B-02 — Order vs. Child Order FSM split is under-specified (BLOCKER)
- Where: `src/lib/fsm.ts`, `src/lib/orderStatus.ts`, BUSINESS_RULES BR-ORD-*, DATABASE_DESIGN `orders` + `child_orders` + `order_status_transitions`.
- Issue: FSM registry defines a single `order` machine. Architecture and DB clearly separate parent order from per-vendor child orders, each with its own lifecycle (vendor accept/reject/ship/deliver vs. customer-visible aggregate). Rules CR-01 / CR-02 reference both but no machine exists for `child_order`. Parent rollup rules ("all children delivered ⇒ parent delivered", "any child cancelled ⇒ partial cancel") are not codified.
- Impact: Backend cannot enforce transitions; ambiguous source of truth for status; vendor actions could desync from customer view.
- Recommendation: Add explicit `child_order` FSM, define rollup function, persist transitions per entity in `order_status_transitions(entity_type, entity_id, ...)`.

### B-03 — Payment intent ↔ Order ↔ Idempotency linkage incomplete (BLOCKER)
- Where: `src/lib/idempotency.ts`, `src/types/payment.ts`, DATABASE_DESIGN `payment_intents`, `idempotency_keys`, `orders`.
- Issue: BUSINESS_RULES BR-PAY-002 requires idempotent capture, but DB schema does not show a UNIQUE constraint `(actor_id, endpoint, key)` paired with `response_hash`; `payment_intents.order_id` nullability is not stated. No explicit rule for what happens if the same idempotency key replays with a *different* payload.
- Impact: Duplicate captures / double charges, orphan intents, replay attacks.
- Recommendation: Pin UNIQUE constraint, mandate request-hash check (409 on mismatch), declare `payment_intents.order_id NOT NULL` after attach, define replay TTL.

### B-04 — Inventory reservation lifecycle is not transactional with checkout (BLOCKER)
- Where: `src/lib/inventoryReservation.ts`, DATABASE_DESIGN `inventory_reservations`, `stock_movements`, BR-INV-*, BR-CHK-*.
- Issue: Reservation TTL exists, but rules do not define: (a) atomic reserve+price-lock at checkout step transition, (b) release on payment failure vs. expiry, (c) conversion of reservation → committed stock movement on payment success. Sweeper job exists in plan but no row-level lock strategy (`SELECT … FOR UPDATE`) is specified.
- Impact: Oversell, negative stock, lost reservations, race conditions on concurrent checkout.
- Recommendation: Document state machine `reserved → committed | released | expired`, specify advisory lock per variant, require sweeper to be idempotent.

---

## 2. Contradictions Between Documents

### C-01 — Role model (HIGH RISK)
- ARCHITECTURE references `ActorContext.activeRole` (single active role) while BUSINESS_RULES BR-RBAC speaks of "roles[]" and DATABASE_DESIGN uses `user_roles` many-to-many. The plan's `JwtAuthFilter` issues a token with `roles` claim but no `activeRole`.
- Fix before Phase 1: decide whether active-role is server-tracked (session/claim) or purely a client hint, and document.

### C-02 — Soft delete scope (MEDIUM RISK)
- DATABASE_DESIGN says financial tables are append-only (no soft delete), but BUSINESS_RULES BR-GDPR-001 requires erasure. Plan does not reconcile GDPR erasure against immutable financial records.
- Fix: define PII tombstoning (null PII columns, keep financial keys) explicitly.

### C-03 — Coupon stacking rules (MEDIUM RISK)
- BUSINESS_RULES says "one coupon per order"; ARCHITECTURE marketing section mentions multi-level coupons (vendor + platform). DB schema permits multiple `order_coupons` rows.
- Fix: pin a single rule and constrain DB accordingly.

### C-04 — COD threshold (LOW RISK, tracked as AR-03)
- BUSINESS_RULES AR-03 already flags it; plan still references hardcoded threshold in Checkout module.

---

## 3. DTO Inconsistencies

### D-01 — Address shape drift (HIGH RISK)
- `src/types/order.ts` (`ShippingAddress`) vs. `src/types/checkout.ts` vs. profile address book carry slightly different fields (`landmark?`, `phone` vs `mobile`, `pincode` vs `postalCode`). DB `addresses` table standardises one shape.
- Impact: Mapping layer will need lossy conversions; backend validation will reject otherwise-valid frontend payloads.
- Fix: unify on the DB shape, deprecate aliases in DTOs before API contract freeze.

### D-02 — Order line item DTO lacks `variant_id` and `vendor_id` consistently (HIGH RISK)
- `OrderItem` carries `productId` + `sku` but not always `variantId` and `vendorId`; DB requires both for FK integrity and per-vendor split.
- Fix: add required fields to DTO; update factories.

### D-03 — `PaymentIntent` DTO missing `client_secret`, `provider`, `next_action` fields (MEDIUM RISK)
- Required for any real PSP integration; plan assumes them.

### D-04 — `ActorContext` DTO missing `requestId` propagation field (LOW RISK)
- `src/lib/requestId.ts` produces ids but `ActorContext` type does not include it; plan expects it in every request.

---

## 4. Database Inconsistencies

### DB-01 — Enum drift (HIGH RISK)
- DATABASE_DESIGN lists 14 enums; FSM registry in `src/lib/fsm.ts` and `orderStatus.ts` use slightly different label sets (e.g., `RETURN_REQUESTED` vs `return_requested`, presence of `PARTIALLY_REFUNDED`).
- Fix: generate enums from a single source (TS ↔ SQL) before Flyway V001.

### DB-02 — Missing UNIQUE constraints called for by business rules (HIGH RISK)
- `idempotency_keys (actor_id, endpoint, key)` — not explicitly UNIQUE in schema doc.
- `user_roles (user_id, role)` — present.
- `product_variants (product_id, sku)` — referenced but not stated.
- `payouts (vendor_id, period_start, period_end)` — needed for one payout per period.
- Fix: add to V001–V005.

### DB-03 — Audit log granularity (MEDIUM RISK)
- `audit_log` has `actor_id`, `entity`, `action` but no `request_id` column. Plan mandates request correlation.
- Fix: add `request_id uuid` column + index.

### DB-04 — Settlement ledger lacks signed entries / double-entry constraint (MEDIUM RISK)
- Schema stores amounts but no `direction` (`debit`/`credit`) check, making reconciliation ambiguous.

### DB-05 — Webhook outbox missing dedupe key (LOW RISK)
- No `dedupe_key UNIQUE` column on `webhook_outbox`; replays possible.

---

## 5. FSM Inconsistencies

### F-01 — Refund FSM not aligned with Return FSM (HIGH RISK, mirrors CR-03)
- `fsm.ts` defines refund states but transitions reference return states that don't map 1:1 (e.g., `RETURN_PICKED_UP` has no corresponding refund precondition).
- Fix: explicit cross-machine guard table.

### F-02 — Payment FSM missing terminal `VOIDED` transitions (MEDIUM RISK)
- Authorized → voided path is referenced in rules but not in the registry.

### F-03 — Vendor approval FSM missing `SUSPENDED → REINSTATED` (MEDIUM RISK)
- Only forward suspend path is defined.

---

## 6. Permission Inconsistencies

### P-01 — `has_role()` semantics vs. `activeRole` (HIGH RISK, ties to C-01)
- RLS policies in DB doc use `has_role(auth.uid(), 'admin')` — global role check — but BUSINESS_RULES expect actions to be gated by *active* role to prevent privilege bleed in multi-role users.
- Fix: either drop active-role concept or add `current_active_role()` SQL helper and use it in RLS.

### P-02 — Vendor scope on read endpoints (MEDIUM RISK)
- Plan exposes `/vendor/orders` but RLS policy on `child_orders` is described as "vendor sees own"; corresponding policy on `order_items` / `shipments` not stated.

### P-03 — Admin override audit (MEDIUM RISK)
- Admin can force state transitions per rules; no requirement that overrides write `audit_log` with reason. Should be a hard backend constraint.

---

## 7. Ownership Inconsistencies

### O-01 — `productOwnership.ts` checks vendor_id on product, but variants/images/inventory ownership is implicit (HIGH RISK)
- Backend `OwnershipGuard` must traverse `variant → product → vendor`; not documented.
- Fix: declare ownership chains for every child entity (variants, images, stock_movements, reservations, shipments, returns, refunds).

### O-02 — Cart ownership across guest → authenticated merge (HIGH RISK, MR-01)
- No defined ownership transfer rule for guest cart → user cart on login. Backend will need a deterministic merge contract.

### O-03 — Address ownership on order snapshot (LOW RISK)
- Orders store address by reference vs. snapshot is unclear; if by reference, deleting an address breaks history. DB design implies snapshot but DTO uses reference.

---

## 8. Missing Backend Contracts

- **MC-01 (HIGH):** No OpenAPI/JSON-Schema artifact committed. Plan references contract tests (Pact) but the contract source is missing.
- **MC-02 (HIGH):** Webhook signing contract (HMAC header name, timestamp tolerance, replay window) not specified.
- **MC-03 (HIGH):** Error envelope: RFC-7807 mentioned, but error `code` taxonomy (machine codes used by the frontend) is not enumerated.
- **MC-04 (MEDIUM):** Pagination/sort contract not standardised (`?page=` is used in UI; backend cursor vs. offset undecided).
- **MC-05 (MEDIUM):** File upload contract for product images / KYC docs (size limits, MIME allowlist, virus scan) undefined.
- **MC-06 (MEDIUM):** Rate-limit headers and 429 retry semantics undefined.
- **MC-07 (LOW):** Health/readiness endpoints not specified.

---

## 9. Migration Risks

- **M-01 (HIGH):** Flyway V001 creates 40+ tables in one go per plan; should be split per domain to allow rollback windows.
- **M-02 (HIGH):** Enum values added later require `ALTER TYPE … ADD VALUE` which is non-transactional in Postgres — needs a documented policy.
- **M-03 (MEDIUM):** No seed-data migration for `categories`, `tax_rules`, `app_role`. Without seed, RLS will lock everyone out on first deploy.
- **M-04 (MEDIUM):** Mock data in frontend uses string IDs ("p-1"); backend will issue UUIDs. Need a contract/test fixture migration plan or the UI breaks at cutover.
- **M-05 (LOW):** Timezone: DB uses `timestamptz`, some DTOs serialize as local ISO strings without offset.

---

## 10. Future Rewrite Risks

- **R-01 (HIGH):** Pricing logic currently lives in client (`src/lib/pricing.ts`). If not re-implemented identically server-side at cutover, totals will diverge. Plan must include a parity test suite.
- **R-02 (HIGH):** Order splitting (`orderFactory.ts`) is client-side; server must own this from day one of Checkout module — frontend must be refactored to consume server-split orders, not produce them.
- **R-03 (MEDIUM):** Coupon evaluation engine is client-side; needs server reimplementation with identical precedence rules to avoid user-visible discount drift.
- **R-04 (MEDIUM):** Notification fan-out is currently a Zustand bus; server move to outbox + websocket/SSE will require DTO change to include `delivery_id` and `read_at` server timestamps.
- **R-05 (MEDIUM):** Analytics events are emitted client-side via `analyticsBus`; server-side validation/dedupe will change event shape (add `server_received_at`, `request_id`).
- **R-06 (LOW):** Recently-viewed / comparison stored in localStorage; if promoted to server, schema is undefined.

---

## 11. Summary Table

| ID | Class | Title |
|----|-------|-------|
| B-01 | BLOCKER | Money representation not unified |
| B-02 | BLOCKER | Parent/child order FSM split unspecified |
| B-03 | BLOCKER | PaymentIntent/idempotency linkage incomplete |
| B-04 | BLOCKER | Inventory reservation not transactional |
| C-01 | HIGH | activeRole vs roles[] contradiction |
| D-01 | HIGH | Address DTO drift |
| D-02 | HIGH | OrderItem missing variant_id/vendor_id |
| DB-01 | HIGH | Enum drift TS ↔ SQL |
| DB-02 | HIGH | Missing UNIQUE constraints |
| F-01 | HIGH | Refund/Return FSM misalignment |
| P-01 | HIGH | has_role vs activeRole RLS gap |
| O-01 | HIGH | Child-entity ownership chains undocumented |
| O-02 | HIGH | Guest→user cart merge undefined |
| MC-01 | HIGH | No OpenAPI contract artifact |
| MC-02 | HIGH | Webhook signing contract missing |
| MC-03 | HIGH | Error code taxonomy missing |
| M-01 | HIGH | Monolithic V001 migration |
| M-02 | HIGH | Enum-evolution policy missing |
| R-01 | HIGH | Client-side pricing parity risk |
| R-02 | HIGH | Client-side order splitting |
| C-02..C-04 | MEDIUM | Soft delete / coupon stacking / COD threshold |
| D-03, D-04 | MEDIUM/LOW | PaymentIntent / ActorContext DTO gaps |
| DB-03..DB-05 | MEDIUM/LOW | Audit, ledger, outbox |
| F-02, F-03 | MEDIUM | Payment void & vendor reinstate FSM |
| P-02, P-03 | MEDIUM | Vendor RLS scope, admin override audit |
| O-03 | LOW | Address snapshot vs reference |
| MC-04..MC-07 | MEDIUM/LOW | Pagination, uploads, rate-limit, health |
| M-03..M-05 | MEDIUM/LOW | Seed data, ID format, timezone |
| R-03..R-06 | MEDIUM/LOW | Coupons, notifications, analytics, local storage |

---

## 12. Final Verdict

**NOT SAFE TO START BACKEND.**

Four blockers (B-01 through B-04) and a cluster of HIGH-risk contract/ownership/FSM gaps must be resolved first. They are concentrated in the foundational layer (money, order FSM, payments idempotency, inventory reservations, role model, DTO contracts) and would propagate through every subsequent phase of the Spring Boot plan if implementation began now.

Recommended unblock sequence before Phase 1 kickoff:
1. Resolve B-01 (Money) and DB-01 (enum drift) — touches every module.
2. Resolve B-02 + F-01 (FSM split & refund/return alignment).
3. Resolve B-03 + MC-02 + MC-03 (payments, webhooks, error taxonomy).
4. Resolve B-04 + O-01/O-02 (reservations + ownership chains).
5. Publish OpenAPI artifact (MC-01) and split V001 migrations (M-01).

Once the above are closed, re-run this audit; remaining MEDIUM/LOW items can be tracked inside their respective phases.