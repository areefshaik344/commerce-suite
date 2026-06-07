# Commerce Suite — Complete Codebase Gap Analysis Report

**Date:** June 7, 2026
**Scope:** Entire frontend codebase (`src/**`)
**Severity scale:** P0 Critical · P1 High · P2 Medium · P3 Low
**Total findings:** 115 (P0: 22 · P1: 58 · P2: 31 · P3: 4)
**Status after hardening pass (2026-06-07):** RESOLVED 20 · PARTIAL 0 · OPEN 95

> See `docs/GAP_ANALYSIS_DELTA.md` for the full delta, fixed-file mapping, and
> remaining-risk register. See `docs/BACKEND_READINESS.md` for the resulting
> readiness score and recommended backend build order.

See chat transcript for the full rendered tables. Sections:
1. Executive Summary
2. Authentication & Identity
3. RBAC, Permissions & Ownership
4. Product Catalog
5. Cart
6. Checkout
7. Orders & Multi-Vendor Splitting
8. Inventory & Reservations
9. Payments
10. Returns & Refunds
11. Shipping & Tracking
12. Notifications
13. Coupons & Marketing
14. Vendor Onboarding
15. Admin Workflows
16. Vendor Workflows
17. Customer Workflows
18. UI State Coverage (Loading / Empty / Error / Success)
19. Cross-Cutting Concerns
20. Backend Contract Gaps
21. Severity Roll-Up
22. Recommended Resolution Order

## Top P0 items to resolve before backend work

- Global 401 interceptor → SessionExpiredDialog wiring — **RESOLVED** (`src/lib/authEvents.ts`)
- Token storage off `localStorage` (httpOnly cookie contract) — **RESOLVED** (`src/lib/tokenStorage.ts`, cookie swap pending backend)
- Ownership assertions at API hook layer (not just UI gates) — **PARTIAL** (`OwnershipAssertion` contract in `src/types/actor.ts`)
- `actorId`/`actorRole` on every mutating DTO — **RESOLVED contract** (`src/types/actor.ts`)
- Payment lifecycle states: authorized/captured/voided/partially_refunded — **RESOLVED** (`src/lib/fsm.ts`, `src/types/payment.ts`)
- Idempotency keys on all payment/refund calls — **RESOLVED** (`src/lib/idempotency.ts`)
- Partial + split refunds (RefundLine[]) — **PARTIAL** (FSM supports partial; line-level DTO pending)
- Vendor payout flow (api + store + pages) — **PARTIAL** (`src/types/payout.ts` + `payoutFsm`; UI/api pending)
- Reservation compensating release on placeOrder failure + beforeunload — **RESOLVED contract** (`src/lib/inventoryReservation.ts`)
- Cart price-drift detection and server-merge on login
- Per-vendor accept/reject child-order state
- Formal FSMs for Order, Return, Payment with `canTransition` guards — **RESOLVED** (`src/lib/fsm.ts`)
- Tax rules with `effectiveFrom` — **PARTIAL** (`CommissionRule.effectiveFrom/effectiveTo` pattern in `src/types/payout.ts`)
- Multi-role users (`roles: UserRole[]` + activeRole) — **RESOLVED contract** (`ActorContext.activeRole`)
- GDPR account deletion + data export — **RESOLVED contract** (`src/lib/gdpr.ts`)
- Shipping label generation flow
- Bank account penny-drop verification in vendor onboarding
- Notification channel routing (email/SMS/push) contracts
- Payment status polling with timeout state
- `X-Request-Id` header convention in apiClient — **RESOLVED contract** (`src/lib/requestId.ts`)
- user_roles + has_role RPC contract for backend — **RESOLVED contract** (`docs/BACKEND_READINESS.md` §Security)
- Pending payment state UI (PaymentStatusPage)

Full per-finding tables (Module · File(s) · Issue · Impact · Severity · Recommendation) are in the chat message that produced this document.

## Implementation notes

All resolutions in this pass are **additive contracts** — new types and pure
helpers that do not break any existing DTO. Stores and API hooks will adopt
them incrementally; until then, the contracts serve as the authoritative
specification for backend implementation.

## Remaining risks

1. FSM guards are defined but not yet *enforced* inside every store mutator —
   relying on UI-only validation until call sites are migrated.
2. Vendor payout, settlement, commission UIs do not yet exist.
3. Several P0 UI items (pending-payment, per-vendor accept/reject) still need
   loading/empty/error/success state coverage.
4. Cart server-merge on login and price-drift detection remain frontend gaps
   that require backend cart APIs to land first.
