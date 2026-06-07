# Commerce Suite — Business Rules Specification

**Date:** June 7, 2026
**Source of truth:** current codebase (`src/**`), `docs/ARCHITECTURE.md`,
`docs/GAP_ANALYSIS.md`, `docs/GAP_ANALYSIS_DELTA.md`, `docs/BACKEND_READINESS.md`.
**Scope:** every business rule that is implemented, contractually defined, or
materially affects domain behaviour. Rules flagged **CONTRACT** are defined as
DTO/FSM contracts and awaiting backend enforcement.

**Legend**
- Status: `IMPLEMENTED` · `CONTRACT` · `PARTIAL` · `MISSING` · `AMBIGUOUS` · `CONFLICT`
- Roles: C = Customer · V = Vendor · A = Admin · S = System
- Money: integer ₹ (rupees). Paise only at gateway boundary.

---

## 1. Authentication

### BR-AUTH-001 — Email/Password sign-in
| Field | Value |
|-------|-------|
| Domain | Auth |
| Description | User authenticates with email + password; receives JWT access (15min) + refresh (7d). |
| Preconditions | Account exists and is not suspended; email verified for non-demo users. |
| Validation | Email RFC-5322; password min 8 chars (see BR-AUTH-005). |
| Permissions | Public. |
| Ownership | n/a |
| Transitions | Session: `ANONYMOUS → AUTHENTICATED`. |
| Failures | Invalid credentials, account locked, account deleted, email unverified. |
| Backend | Re-issue tokens; rate-limit 5/min/IP; account-lock after 10 fails. |
| Source | `src/api/authApi.ts`, `src/store/authStore.ts`, `src/lib/tokenStorage.ts`, `src/pages/auth/LoginPage.tsx` |
| Status | IMPLEMENTED |

### BR-AUTH-002 — Phone OTP sign-in
| Field | Value |
|-------|-------|
| Domain | Auth |
| Description | 6-digit OTP delivered by SMS, valid 5 minutes, single-use. |
| Validation | E.164 phone; OTP exactly 6 digits; max 5 verify attempts per OTP. |
| Permissions | Public. |
| Failures | OTP expired, OTP wrong, throttled (>3 sends/15min/number). |
| Backend | SMS provider integration pending. |
| Source | `src/api/authApi.ts`, `src/components/auth/OtpInput.tsx`, `src/hooks/useOtpTimer.ts` |
| Status | PARTIAL (frontend complete; provider pending) |

### BR-AUTH-003 — Token lifecycle
| Field | Value |
|-------|-------|
| Description | Access TTL 15min, refresh TTL 7d, skew 60s. Refresh token rotates on use. |
| Preconditions | Valid refresh token not expired/revoked. |
| Permissions | Self only. |
| Transitions | Access: `VALID → EXPIRED → REFRESHED \| REVOKED`. |
| Failures | Refresh expired ⇒ emit `sessionExpired` event ⇒ `SessionExpiredDialog`. |
| Backend | Refresh token in httpOnly+Secure cookie (planned); current storage in `localStorage`. |
| Source | `src/lib/tokenStorage.ts`, `src/lib/authEvents.ts`, `src/components/auth/SessionExpiredDialog.tsx`, `src/api/apiClient.ts` |
| Status | IMPLEMENTED (cookie swap is CONTRACT) |

### BR-AUTH-004 — Cross-tab session sync
| Description | Login/logout/refresh in one tab broadcasts to all open tabs via `storage` event on key `mh.sync`. |
| Source | `src/lib/tokenStorage.ts` (`broadcast`, `SYNC_KEY`) |
| Status | IMPLEMENTED |

### BR-AUTH-005 — Password policy
| Description | Min 8 chars, mixed case, ≥1 digit, ≥1 symbol; HIBP check at backend. |
| Validation | `passwordStrength.ts` rates Weak/Fair/Good/Strong; signup blocks Weak. |
| Backend | Enable Supabase HIBP toggle. |
| Source | `src/lib/passwordStrength.ts`, `src/components/auth/PasswordStrengthMeter.tsx` |
| Status | IMPLEMENTED (HIBP is CONTRACT) |

### BR-AUTH-006 — Password reset
| Description | `resetPasswordForEmail` ⇒ user lands on `/reset-password` ⇒ `updateUser({password})`. |
| Permissions | Public. |
| Source | `src/pages/auth/ForgotPasswordPage.tsx`, `src/pages/auth/ResetPasswordPage.tsx` |
| Status | PARTIAL (frontend ready; backend wiring pending) |

### BR-AUTH-007 — Email verification gate
| Description | Customers can browse without verification; checkout & reviews require verified email. |
| Status | AMBIGUOUS — gate not consistently enforced across pages. |

---

## 2. RBAC & Permissions

### BR-RBAC-001 — Single canonical permission registry
| Description | All capability strings centralised in `permissions.ts`; UI uses `<Can perm=…>` and `usePermissions()`. |
| Source | `src/lib/permissions.ts`, `src/hooks/usePermissions.ts`, `src/components/auth/Can.tsx` |
| Status | IMPLEMENTED |

### BR-RBAC-002 — Ownership-scoped permissions
| Description | `<Can perm ownerId={x}>` short-circuits unless user is owner OR holds an override role. |
| Backend | `has_role(uuid, app_role)` + RLS `auth.uid() = owner_id OR has_role(...)`. |
| Status | IMPLEMENTED frontend / CONTRACT backend |

### BR-RBAC-003 — Multi-role users
| Description | A user may hold multiple roles; `ActorContext.activeRole` selects the operating role per request. |
| Source | `src/types/actor.ts` |
| Status | CONTRACT |

### BR-RBAC-004 — User roles stored separately
| Description | Roles MUST live in `public.user_roles`, never on `profiles`. Prevents privilege escalation. |
| Backend | Security-definer `has_role()` with `search_path = public`. |
| Status | CONTRACT |

### BR-RBAC-005 — Route gating
| Description | `ProtectedRoute`, `PermissionRoute`, `RoleRoute`, `PublicRoute` enforce auth + role at router level. |
| Source | `src/routes/*` |
| Status | IMPLEMENTED |

### BR-RBAC-006 — Frontend gate is advisory
| Description | Every mutation MUST be re-validated server-side; UI gates are UX, not security. |
| Status | CONTRACT |

---

## 3. Users & Profiles

### BR-USER-001 — Profile required for checkout
| Description | `name`, `phone`, ≥1 saved address required before placing an order. |
| Source | `src/components/profile/ProfileCompletionCard.tsx`, `src/lib/profileValidation.ts` |
| Status | IMPLEMENTED |

### BR-USER-002 — Address limits
| Description | Max 10 saved addresses; exactly one default. |
| Source | `src/components/profile/AddressManager.tsx` |
| Status | PARTIAL (cap is AMBIGUOUS — not enforced uniformly) |

### BR-USER-003 — Account deletion (GDPR)
| Description | 30-day grace period; user may cancel during grace; hard-delete after `scheduledFor`. |
| Validation | Reason ∈ enum; user must re-authenticate. |
| Backend | Anonymise orders/reviews (`'[deleted]'`); preserve audit log. |
| Source | `src/lib/gdpr.ts` |
| Status | CONTRACT |

### BR-USER-004 — Data export (GDPR)
| Description | User-triggered export of selected domains; pre-signed URL valid 24h. |
| Source | `src/lib/gdpr.ts` |
| Status | CONTRACT |

---

## 4. Vendor

### BR-VENDOR-001 — Vendor onboarding KYC
| Description | Multi-step form: business → PAN → GST (optional <₹40L turnover) → bank → review. |
| Validation | PAN regex `[A-Z]{5}[0-9]{4}[A-Z]`; GST 15-char state-prefix; IFSC 11-char. |
| Permissions | Public (creates pending vendor application). |
| Transitions | `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED \| REJECTED \| INFO_REQUESTED`. |
| Failures | Duplicate PAN, invalid IFSC, missing docs. |
| Source | `src/pages/auth/VendorRegisterPage.tsx`, `src/store/vendorOnboardingStore.ts`, `src/pages/vendor/VendorOnboarding.tsx` |
| Status | IMPLEMENTED frontend / penny-drop is MISSING |

### BR-VENDOR-002 — Vendor approval required to list
| Description | Vendor cannot publish products until application status = `APPROVED`. |
| Permissions | Only Admin transitions to APPROVED. |
| Source | `src/pages/admin/AdminVendorApplications.tsx`, `src/pages/admin/AdminVendorDetail.tsx` |
| Status | IMPLEMENTED |

### BR-VENDOR-003 — Vendor suspension
| Description | Admin may suspend a vendor; suspended vendors keep order history but cannot list, edit, or fulfil. |
| Transitions | `APPROVED → SUSPENDED → APPROVED \| TERMINATED`. |
| Source | `src/pages/admin/AdminVendorDetail.tsx`, `src/components/auth/VendorStatusBadge.tsx` |
| Status | PARTIAL (UI present; downstream blockers AMBIGUOUS) |

### BR-VENDOR-004 — Vendor public store
| Description | Each approved vendor gets `/store/:slug` with profile, banner, catalog, ratings. |
| Source | `src/pages/customer/VendorStorePage.tsx`, `src/pages/vendor/VendorStoreCustomization.tsx` |
| Status | IMPLEMENTED |

---

## 5. Catalog

### BR-CAT-001 — Product ownership
| Description | A product belongs to exactly one vendor; only owning vendor or Admin may edit/delete. |
| Source | `src/lib/productOwnership.ts`, `src/api/productApi.ts` |
| Status | IMPLEMENTED frontend / CONTRACT backend (RLS) |

### BR-CAT-002 — Product moderation
| Description | New/edited products enter `PENDING_REVIEW`; Admin approves to publish. |
| Transitions | `DRAFT → PENDING_REVIEW → APPROVED \| REJECTED → ARCHIVED`. |
| Source | `src/pages/admin/AdminProducts.tsx`, `src/pages/admin/AdminProductDetail.tsx` |
| Status | IMPLEMENTED (server-side image moderation MISSING) |

### BR-CAT-003 — Variant integrity
| Description | Each variant unique by `(productId, options)`; SKU globally unique per vendor. |
| Status | IMPLEMENTED frontend / unique constraint is CONTRACT |

### BR-CAT-004 — Pricing fields
| Description | `mrp ≥ price`; `discount% = round((mrp-price)/mrp*100)`. |
| Source | `src/lib/pricing.ts`, `src/components/product/ProductPriceBlock.tsx` |
| Status | IMPLEMENTED |

### BR-CAT-005 — Category attributes
| Description | Category-level attribute schema drives PDP spec table and filters. |
| Source | `src/config/categoryAttributes.ts` |
| Status | IMPLEMENTED |

---

## 6. Inventory

### BR-INV-001 — Stock buckets
| Description | Per variant: `onHand`, `reserved`, `available = onHand - reserved`. UI badges: In stock / Low (≤5) / Out. |
| Source | `src/components/product/InventoryBadge.tsx`, `src/pages/vendor/VendorInventory.tsx` |
| Status | IMPLEMENTED |

### BR-INV-002 — Reservation on cart-to-checkout
| Description | When user enters `/checkout`, inventory is reserved for 15 min (`RESERVATION_TTL_MINUTES`). |
| Failures | Insufficient stock blocks reservation; user returned to cart with diff dialog. |
| Status | PARTIAL (timer in `ReservationTimer.tsx`; server sweep CONTRACT) |

### BR-INV-003 — Compensating release
| Description | Release reservation on: ABANDONED, PAYMENT_FAILED, PAYMENT_CANCELLED, TTL_EXPIRED, EXPLICIT_RELEASE, USER_LOGOUT. |
| Source | `src/lib/inventoryReservation.ts` |
| Status | CONTRACT |

### BR-INV-004 — Low-stock alert
| Description | Vendor receives notification when `available ≤ 5`. |
| Source | `src/pages/vendor/VendorLowStockAlerts.tsx` |
| Status | IMPLEMENTED |

### BR-INV-005 — Stock decrement on capture
| Description | `onHand` decremented atomically when payment captured; reservation released. |
| Status | CONTRACT |

---

## 7. Cart

### BR-CART-001 — Vendor grouping
| Description | Items grouped by vendor; each vendor shows its own subtotal and shipping. |
| Source | `src/components/cart/CartSummary.tsx`, `src/store/cartStore.ts` |
| Status | IMPLEMENTED |

### BR-CART-002 — Quantity bounds
| Description | Per line: `1 ≤ qty ≤ min(variant.maxPerOrder ?? 10, variant.available)`. |
| Source | `src/components/cart/QuantitySelector.tsx` |
| Status | IMPLEMENTED |

### BR-CART-003 — Save for Later
| Description | User can move cart line ↔ SFL list; SFL items are not priced into totals. |
| Source | `src/components/cart/SavedForLaterSection.tsx` |
| Status | IMPLEMENTED |

### BR-CART-004 — Cart price-drift detection
| Description | On checkout entry, prices/availability re-validated against catalog; mismatches surface a diff dialog. |
| Status | MISSING (P0) |

### BR-CART-005 — Server merge on login
| Description | Anonymous cart merges into authenticated cart on login (qty additive, dedup by variant). |
| Status | MISSING (P0, backend-bound) |

---

## 8. Checkout

### BR-CHK-001 — 3-step flow
| Description | Address → Payment → Review. Each step validated before next. |
| Source | `src/pages/customer/CheckoutPage.tsx`, `src/components/checkout/CheckoutStepper.tsx` |
| Status | IMPLEMENTED |

### BR-CHK-002 — Shipping method per vendor
| Description | Each vendor offers its own shipping methods; total shipping = Σ(vendor shipping). |
| Source | `src/components/checkout/ShippingMethodSelector.tsx` |
| Status | IMPLEMENTED |

### BR-CHK-003 — Tax computation
| Description | GST applied per item using rate effective on order placement timestamp. |
| Source | `src/lib/pricing.ts` |
| Status | PARTIAL (effective-from CONTRACT in `CommissionRule`, mirror to tax rules pending) |

### BR-CHK-004 — Coupon application
| Description | At most one coupon at a time; applies per vendor or globally depending on coupon scope. |
| Validation | Min order value, valid date range, usage cap, per-user cap, allowed categories/vendors. |
| Source | `src/components/cart/CouponInput.tsx`, `src/store/couponStore.ts` |
| Status | IMPLEMENTED |

### BR-CHK-005 — Pricing breakdown invariant
| Description | `total = Σitem.total + Σshipping + tax − discount`. Reconciled in `PriceBreakdown.tsx`. |
| Source | `src/lib/pricing.ts`, `src/components/checkout/PriceBreakdown.tsx` |
| Status | IMPLEMENTED |

### BR-CHK-006 — COD eligibility
| Description | COD blocked when total > ₹50,000 or destination pincode not COD-serviceable. |
| Status | AMBIGUOUS — threshold not centralised. |

---

## 9. Orders

### BR-ORD-001 — Parent/child split
| Description | One parent order per checkout; one child order per vendor. Each child has its own status/shipment. |
| Source | `src/lib/orderFactory.ts`, `src/components/orders/VendorOrderGroup.tsx` |
| Status | IMPLEMENTED |

### BR-ORD-002 — Immutable snapshots
| Description | `ProductSnapshot`, `VendorSnapshot`, `PricingSnapshot` captured at placement; never re-derived. |
| Source | `src/types/order.ts` |
| Status | IMPLEMENTED |

### BR-ORD-003 — Order FSM
| Description | Transitions governed by `orderFsm`. Illegal transitions throw `InvalidTransitionError`. |
| Permissions per transition | See `src/lib/fsm.ts` (role guard column). |
| Source | `src/lib/fsm.ts`, `src/lib/orderStatus.ts` |
| Status | IMPLEMENTED (call-site enforcement PARTIAL) |

### BR-ORD-004 — Customer cancellation window
| Description | Customer may cancel a child order only while status ∈ {CREATED, CONFIRMED}. |
| Source | `src/components/orders/CancellationDialog.tsx` |
| Status | IMPLEMENTED |

### BR-ORD-005 — Per-vendor accept/reject
| Description | Each vendor accepts or rejects its child order within 24h; auto-cancel on timeout. |
| Status | MISSING (P0) |

### BR-ORD-006 — Invoice generation
| Description | Per child order, generated on `CONFIRMED`; immutable thereafter. |
| Source | `src/api/invoiceApi.ts`, `src/store/invoiceStore.ts` |
| Status | PARTIAL (PDF generation CONTRACT) |

---

## 10. Shipping

### BR-SHP-001 — Shipment FSM
| Description | `PACKING → READY_TO_SHIP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED \| FAILED_DELIVERY`. |
| Source | `src/lib/fsm.ts` (`shipmentFsm`) |
| Status | IMPLEMENTED |

### BR-SHP-002 — Tracking events ordered
| Description | Tracking events stored chronologically; UI renders via `TrackingTimeline`. |
| Source | `src/components/shipping/TrackingTimeline.tsx` |
| Status | IMPLEMENTED |

### BR-SHP-003 — Pincode serviceability
| Description | Delivery ETA per pincode; ineligible pincodes blocked at PDP and checkout. |
| Source | `src/components/shared/PincodeChecker.tsx` |
| Status | IMPLEMENTED (data source mocked) |

### BR-SHP-004 — Label generation
| Description | Vendor triggers label print on `READY_TO_SHIP`; provider integration required. |
| Status | MISSING (P0) |

---

## 11. Payments

### BR-PAY-001 — PaymentIntent lifecycle
| Description | `CREATED → (REQUIRES_ACTION) → AUTHORIZED → CAPTURED → (PARTIALLY_REFUNDED) → REFUNDED`. Failure paths to FAILED/CANCELLED. |
| Source | `src/lib/fsm.ts` (`paymentFsm`), `src/types/payment.ts` |
| Status | IMPLEMENTED (model) / CONTRACT (gateway) |

### BR-PAY-002 — Idempotency
| Description | Every payment, refund, payout, order-placement call carries `Idempotency-Key`; backend dedupes for 24h. |
| Source | `src/lib/idempotency.ts` |
| Status | CONTRACT |

### BR-PAY-003 — Retry policy
| Description | Max `PAYMENT_RETRY_LIMIT = 3` attempts per intent; new intent after exhaustion. |
| Source | `src/types/payment.ts`, `src/store/paymentStore.ts` |
| Status | IMPLEMENTED |

### BR-PAY-004 — Intent TTL
| Description | Intent expires after `PAYMENT_INTENT_TTL_MINUTES = 15`. Polling required for async gateways. |
| Source | `src/types/payment.ts` |
| Status | IMPLEMENTED model / polling UI MISSING |

### BR-PAY-005 — Method catalog
| Description | UPI, CARD, WALLET, NETBANKING (prepaid) + COD (postpaid). Frontend branches on `PaymentMethodKind`, never gateway name. |
| Source | `src/types/payment.ts` |
| Status | IMPLEMENTED |

---

## 12. Returns

### BR-RET-001 — Return window
| Description | Customer may raise a return within 7 days of `DELIVERED` (configurable per category). |
| Source | `src/components/orders/ReturnRequestDialog.tsx` |
| Status | PARTIAL (window AMBIGUOUS — not per-category) |

### BR-RET-002 — Return FSM
| Description | `REQUESTED → APPROVED \| REJECTED → PICKED_UP → REFUNDED`. Vendor approves; System advances to picked-up; Admin/System refunds. |
| Source | `src/lib/fsm.ts` (`returnFsm`) |
| Status | IMPLEMENTED |

### BR-RET-003 — Reason taxonomy
| Description | Customer selects from fixed reason set: WRONG_ITEM, DAMAGED, NOT_AS_DESCRIBED, QUALITY_ISSUE, NO_LONGER_NEEDED, OTHER. |
| Source | `src/components/orders/ReturnRequestDialog.tsx` |
| Status | IMPLEMENTED |

### BR-RET-004 — Non-returnable items
| Description | Categories flagged `nonReturnable=true` (e.g. innerwear) cannot raise returns. |
| Status | AMBIGUOUS — flag exists but not consistently enforced. |

---

## 13. Refunds

### BR-REF-001 — Refund FSM
| Description | `PENDING → PROCESSING → COMPLETED \| FAILED`. Admin may re-queue FAILED → PENDING. |
| Source | `src/lib/fsm.ts` (`refundFsm`) |
| Status | IMPLEMENTED |

### BR-REF-002 — Partial refunds
| Description | Σ refunds ≤ captured amount; intent becomes `PARTIALLY_REFUNDED` until total reaches captured. |
| Source | `src/types/payment.ts`, `src/lib/fsm.ts` |
| Status | IMPLEMENTED model / RefundLine[] DTO MISSING |

### BR-REF-003 — Refund source attribution
| Description | Every refund carries `sourceType ∈ {CANCELLATION, RETURN, ADJUSTMENT}` + `sourceId`. |
| Source | `src/types/payment.ts` |
| Status | IMPLEMENTED |

### BR-REF-004 — SLA
| Description | Refunds initiated within 24h of approval; settled within 5–7 working days. |
| Status | AMBIGUOUS — SLA not enforced or surfaced. |

---

## 14. Notifications

### BR-NOT-001 — Unified activity feed
| Description | Single store powers in-app feed across all portals; unread badge reflects per-user count. |
| Source | `src/store/notificationStore.ts`, `src/components/notifications/*` |
| Status | IMPLEMENTED |

### BR-NOT-002 — Channel preferences
| Description | User selects per-category channels (email/SMS/push/in-app). |
| Source | `src/components/notifications/NotificationPreferences.tsx` |
| Status | PARTIAL (UI only) |

### BR-NOT-003 — Channel router
| Description | Backend selects provider per channel; templated payloads. |
| Status | MISSING (P0) |

### BR-NOT-004 — Transactional vs marketing
| Description | User cannot unsubscribe from transactional (order/payment/security); marketing opt-out respected. |
| Status | AMBIGUOUS — distinction not encoded. |

---

## 15. Audit

### BR-AUD-001 — Event envelope
| Description | All domain events flow through `eventBus` with typed envelope; `auditSubscriber` persists destructive actions. |
| Source | `src/lib/eventBus.ts`, `src/lib/subscribers/auditSubscriber.ts` |
| Status | IMPLEMENTED |

### BR-AUD-002 — Request correlation
| Description | Every mutation carries `requestId`; echoed in audit row, webhook payload, analytics event. |
| Source | `src/lib/requestId.ts`, `src/types/actor.ts` |
| Status | CONTRACT |

### BR-AUD-003 — Append-only
| Description | `audit_log` table is INSERT-only; no role except `service_role` may UPDATE/DELETE. |
| Status | CONTRACT |

### BR-AUD-004 — Destructive-action coverage
| Description | MUST audit: user delete, vendor suspend, product delist, order cancel, refund issue, payout reverse, role change, address delete. |
| Status | PARTIAL — subscriber present, taxonomy AMBIGUOUS. |

---

## 16. Analytics

### BR-ANL-001 — Event taxonomy
| Description | Typed analytics events emitted via `analyticsBus`; consumed by `analyticsSubscriber`. |
| Source | `src/types/analyticsEvents.ts`, `src/lib/analyticsBus.ts` |
| Status | IMPLEMENTED |

### BR-ANL-002 — Dashboards
| Description | GMV, AOV, conversion, vendor performance rendered via Recharts on admin/vendor dashboards. |
| Source | `src/pages/admin/AdminAnalytics.tsx`, `src/pages/vendor/VendorAnalytics.tsx` |
| Status | IMPLEMENTED |

### BR-ANL-003 — PII scrubbing
| Description | Events MUST NOT contain PII (email, phone, address) — only IDs. |
| Status | AMBIGUOUS — not enforced by a schema validator. |

---

## 17. Payouts

### BR-PYT-001 — Settlement period
| Description | Weekly close on Sundays 23:59 IST; period = `[periodStart, periodEnd)`. |
| Source | `src/types/payout.ts` |
| Status | CONTRACT |

### BR-PYT-002 — Commission rule resolution
| Description | Priority: vendor-specific override → category rule → global default. Rate effective on `periodEnd`. |
| Source | `src/types/payout.ts` (`CommissionRule`) |
| Status | CONTRACT |

### BR-PYT-003 — Net payable formula
| Description | `netPayable = gmv − commission + shippingReimbursement − refundAdjustment − tds`. |
| Source | `src/types/payout.ts` |
| Status | CONTRACT |

### BR-PYT-004 — Payout FSM
| Description | `ACCRUED → SCHEDULED → PROCESSING → PAID \| FAILED`; `ON_HOLD` and `REVERSED` admin-only branches. |
| Source | `src/lib/fsm.ts` (`payoutFsm`) |
| Status | IMPLEMENTED model |

### BR-PYT-005 — Hold on disputes
| Description | Open disputes/returns above threshold move payout to `ON_HOLD`. |
| Status | AMBIGUOUS — threshold undefined. |

### BR-PYT-006 — Vendor visibility
| Description | Vendor sees: available, in-flight, paid lifetime, last payout. |
| Source | `src/pages/vendor/VendorPayoutHistory.tsx`, `src/pages/vendor/VendorFinancials.tsx` |
| Status | PARTIAL (pages stubbed; data model CONTRACT) |

---

## 18. Missing Business Rules

| ID | Domain | Gap |
|----|--------|-----|
| MR-01 | Cart | Server merge on login (anonymous → authenticated). |
| MR-02 | Cart | Price-drift detection at checkout entry. |
| MR-03 | Orders | Per-vendor accept/reject 24h window + auto-cancel. |
| MR-04 | Shipping | Carrier label provider integration. |
| MR-05 | Payments | Pending-payment polling UI with timeout. |
| MR-06 | Notifications | Channel router (email/SMS/push providers). |
| MR-07 | Refunds | Line-level `RefundLine[]` DTO. |
| MR-08 | Payouts | Admin payout console; settlement sweeper job. |
| MR-09 | Compliance | Surface GDPR delete/export flows in Profile UI. |
| MR-10 | Catalog | Server-side image moderation (NSFW/IP). |
| MR-11 | Vendor | Bank account penny-drop verification. |
| MR-12 | Audit | Destructive-action taxonomy schema. |

---

## 19. Ambiguous Business Rules

| ID | Rule | Ambiguity |
|----|------|-----------|
| AR-01 | BR-AUTH-007 | When is email verification mandatory vs optional? |
| AR-02 | BR-USER-002 | Address cap (10?) — not enforced uniformly. |
| AR-03 | BR-CHK-006 | COD threshold not centralised. |
| AR-04 | BR-CHK-003 | Tax `effectiveFrom` defined for commission but not tax rules. |
| AR-05 | BR-RET-001 | Return window — global 7d vs per-category override. |
| AR-06 | BR-RET-004 | Non-returnable flag enforcement scope unclear. |
| AR-07 | BR-REF-004 | Refund SLA not encoded. |
| AR-08 | BR-NOT-004 | Transactional vs marketing classification missing. |
| AR-09 | BR-AUD-004 | Destructive-action list not exhaustively typed. |
| AR-10 | BR-PYT-005 | Dispute-hold threshold undefined. |
| AR-11 | BR-ANL-003 | PII scrub rule not enforced by validator. |

---

## 20. Conflicting Business Rules

| ID | Rules in conflict | Description | Resolution |
|----|-------------------|-------------|------------|
| CR-01 | BR-ORD-004 vs BR-ORD-005 | Customer can cancel until CONFIRMED, but vendor has 24h to accept/reject — overlap on CONFIRMED state. | Define precedence: customer cancel always wins until vendor explicitly moves to PROCESSING. |
| CR-02 | BR-PAY-001 vs `orderStatus.ts:PAYMENT_STATUS` | Order-level `PAYMENT_STATUS` lacks `AUTHORIZED`-to-`CANCELLED` direct path that `paymentFsm` allows. | Treat order-level enum as a projection of intent state; backend canonical source is `paymentFsm`. |
| CR-03 | BR-RET-002 vs BR-REF-001 | Return REFUNDED state implies refund COMPLETED, but refund FSM may still be PROCESSING. | Return moves to REFUNDED only on refund COMPLETED event; FSM call-sites must respect ordering. |
| CR-04 | BR-RBAC-002 vs BR-VENDOR-003 | Suspended vendor still passes `ownerId` ownership check. | Add `vendor.status === APPROVED` precondition to `canManage` for vendor-scoped resources. |
| CR-05 | BR-CART-002 vs BR-INV-001 | Frontend caps qty by `variant.available`, but inventory may drift between cart and checkout. | Authoritative cap re-applied on reservation (BR-INV-002). |

---

## 21. Backend Enforcement Summary

| Concern | Mechanism |
|---------|-----------|
| Identity | Supabase Auth JWT — backend derives `actorId`/`actorRole`. |
| Authorization | RLS + `public.has_role()` security-definer. |
| State transitions | Re-run `assertTransition()` server-side using `src/lib/fsm.ts` as spec. |
| Idempotency | `idempotency_keys` table, 24h TTL, scoped per actor. |
| Correlation | `X-Request-Id` echoed and persisted to `audit_log`. |
| Inventory | Server-side reservation TTL sweeper; atomic decrement on capture. |
| Pricing | Re-priced server-side at placement; client total advisory. |
| Audit | Append-only `audit_log` with INSERT-only RLS. |
| GDPR | 30-day grace job; data export worker; anonymisation routine. |
| Payouts | Settlement sweeper job; commission resolver; bank file generator. |

---

## 22. Cross-Reference

- Architecture decisions: `docs/ARCHITECTURE.md`
- Outstanding gaps: `docs/GAP_ANALYSIS.md`, `docs/GAP_ANALYSIS_DELTA.md`
- Backend prerequisites: `docs/BACKEND_READINESS.md`
- FSM source: `src/lib/fsm.ts`
- Actor / identity contract: `src/types/actor.ts`
- Idempotency + correlation: `src/lib/idempotency.ts`, `src/lib/requestId.ts`
- Reservation safety: `src/lib/inventoryReservation.ts`
- Payout domain: `src/types/payout.ts`
- GDPR: `src/lib/gdpr.ts`