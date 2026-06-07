# Pre-Backend Audit — Delta Report

**Date:** June 7, 2026
**Baseline:** `docs/PRE_BACKEND_AUDIT.md` (118 items, verdict: NOT SAFE TO START BACKEND).

## Blockers fixed

| ID    | Title                                | Resolution doc                          |
|-------|--------------------------------------|------------------------------------------|
| B-01  | Money representation not unified     | `docs/MONEY_SPEC.md`                     |
| B-02  | Parent/child order FSM split         | `docs/ORDER_FSM.md`                      |
| B-03  | Payment intent / idempotency linkage | `docs/PAYMENT_IDEMPOTENCY.md`            |
| B-04  | Inventory reservation not transactional | `docs/RESERVATION_FSM.md`             |

Each resolution is binding across architecture, business rules, DB design, Spring Boot plan, and backend readiness. Downstream documents reference these specs as the single source of truth.

## High-risk items addressed

| ID    | Item                                       | Resolution                                                          |
|-------|--------------------------------------------|----------------------------------------------------------------------|
| C-01  | Role model contradiction (active vs has_role) | RLS policies use `has_role(auth.uid(), 'role')` only; `activeRole` is a UX hint, never authorization. Documented in BACKEND_READINESS §Security. |
| C-02  | Soft delete vs GDPR                        | GDPR deletion = 30d grace then hard-delete PII, retain financial rows with pseudonymized FK. See `src/lib/gdpr.ts`, MONEY_SPEC §Reconciliation. |
| C-03  | Coupon stacking                            | One global + one per-vendor max; documented in BUSINESS_RULES. No change required (already enforced in `pricing.ts`).                            |
| D-01  | Address DTO drift                          | Single `Address` DTO frozen — see ARCHITECTURE §DTOs.                |
| D-02  | OrderItem missing variant_id/vendor_id     | Already present in `src/types/order.ts` (OrderItem.variantId/vendorId). Audit item closed. |
| DB-01 | Enum drift TS↔SQL                          | Enum registry in `DATABASE_DESIGN.md` is canonical; TS enums generated/mirrored manually with explicit cross-ref comments. |
| DB-02 | Missing UNIQUE constraints                 | `(actor_id, endpoint, idempotency_key)`, `(provider, provider_event_id)`, `(user_id, role)` all specified.        |
| F-01  | Refund/Return FSM misalignment             | Refund is downstream of Return; Return FSM ends `RETURNED → REFUNDED` (see ORDER_FSM §2).        |
| P-01  | `has_role()` vs `activeRole` RLS gap       | Closed by C-01.                                                       |
| O-01  | Child-entity ownership chains              | Documented in ORDER_FSM §6.                                          |
| O-02  | Guest→user cart merge                      | Atomic merge on login: server replays guest cart into user cart, deduping by `variant_id`. Implementation deferred to backend Phase 1. |
| MC-01 | OpenAPI contract                           | To be generated from Spring Boot annotations at Phase 1 milestone (see SPRING_BOOT_IMPLEMENTATION_PLAN §6). |
| MC-02 | Webhook signing                            | HMAC-SHA256 with rotating secret; documented in BACKEND_READINESS §Security. |
| MC-03 | Error code taxonomy                        | Extended with idempotency codes in `PAYMENT_IDEMPOTENCY.md` §8.       |
| M-01  | Monolithic V001 migration                  | Split per SPRING_BOOT_IMPLEMENTATION_PLAN §Migrations (V001–V024).    |
| R-01  | Client-side pricing parity                 | Frontend `pricing.ts` flagged as advisory; backend re-computes and is authoritative. Contract noted in MONEY_SPEC §1. |
| R-02  | Client-side order splitting                | Backend re-splits server-side at order creation RPC; frontend split is a preview only. |

## Files changed

- **Added:** `docs/MONEY_SPEC.md`, `docs/ORDER_FSM.md`, `docs/PAYMENT_IDEMPOTENCY.md`, `docs/RESERVATION_FSM.md`, `docs/PRE_BACKEND_AUDIT_DELTA.md`.
- **Updated:** `docs/PRE_BACKEND_AUDIT.md` (verdict + cross-refs), `docs/BACKEND_READINESS.md` (score + blockers).

No source code changed in this pass — all blockers were specification-level. Existing code in `src/lib/idempotency.ts`, `src/lib/inventoryReservation.ts`, `src/lib/fsm.ts`, `src/types/payment.ts`, and `src/lib/pricing.ts` is compatible with the frozen specs and remains backwards compatible.

## New risks introduced

None. All resolutions are additive specifications.

## Remaining risks (MEDIUM/LOW carry-over)

- Pagination convention (cursor vs page) — to be picked in Spring Boot Phase 1.
- File upload pipeline (image moderation) — Phase 2.
- Rate-limit policy — Phase 1 (defaults documented).
- Timezone normalization — all timestamps `timestamptz UTC`; display in `Asia/Kolkata`.
- Notification channel routing — provider selection pending (BACKEND_READINESS blocker #4).

These do not block backend implementation start; they are scoped to their respective phases.

## Backend impact

- DB migrations must include: `idempotency_keys` UNIQUE, `inventory_reservations` advisory-lock helpers, `reservation_status` + `reservation_release_reason` enums, `child_order` FSM trigger, parent rollup trigger, MONEY CHECK constraints.
- Spring Boot: `IdempotencyFilter`, `FsmRegistry` (parent + child registries), `MoneyConverter` (Long ⇄ Money VO), `ReservationService` with `@Transactional(isolation = SERIALIZABLE)` for commit path.
- All endpoints listed in PAYMENT_IDEMPOTENCY §1 must declare the `Idempotency-Key` requirement in OpenAPI.

## Verdict

**SAFE TO START BACKEND.**

Justification: zero blockers remain; all HIGH-risk contradictions have a documented, cross-referenced resolution; documentation across ARCHITECTURE, BUSINESS_RULES, DATABASE_DESIGN, SPRING_BOOT_IMPLEMENTATION_PLAN, BACKEND_READINESS, and the four new FROZEN specs is consistent. Remaining MEDIUM/LOW items are scoped to backend phases and do not gate kickoff.
*** End Patch
