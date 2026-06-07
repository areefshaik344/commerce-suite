# Commerce Suite — Complete Codebase Gap Analysis Report

**Date:** June 7, 2026
**Scope:** Entire frontend codebase (`src/**`)
**Severity scale:** P0 Critical · P1 High · P2 Medium · P3 Low
**Total findings:** 115 (P0: 22 · P1: 58 · P2: 31 · P3: 4)

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

- Global 401 interceptor → SessionExpiredDialog wiring
- Token storage off `localStorage` (httpOnly cookie contract)
- Ownership assertions at API hook layer (not just UI gates)
- `actorId`/`actorRole` on every mutating DTO
- Payment lifecycle states: authorized/captured/voided/partially_refunded
- Idempotency keys on all payment/refund calls
- Partial + split refunds (RefundLine[])
- Vendor payout flow (api + store + pages) — currently absent
- Reservation compensating release on placeOrder failure + beforeunload
- Cart price-drift detection and server-merge on login
- Per-vendor accept/reject child-order state
- Formal FSMs for Order, Return, Payment with `canTransition` guards
- Tax rules with `effectiveFrom`
- Multi-role users (`roles: UserRole[]` + activeRole)
- GDPR account deletion + data export
- Shipping label generation flow
- Bank account penny-drop verification in vendor onboarding
- Notification channel routing (email/SMS/push) contracts
- Payment status polling with timeout state
- `X-Request-Id` header convention in apiClient
- user_roles + has_role RPC contract for backend
- Pending payment state UI (PaymentStatusPage)

Full per-finding tables (Module · File(s) · Issue · Impact · Severity · Recommendation) are in the chat message that produced this document.
