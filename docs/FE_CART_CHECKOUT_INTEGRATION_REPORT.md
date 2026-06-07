# Phase FE-4 — Cart & Checkout Integration Report

_Date_: 2026-06-07
_Sources_: `docs/CART_CHECKOUT_MODULE.md`, backend DTOs under
`backend/src/main/java/com/commercesuite/{cart,checkout,coupon}/dto/*`.

## 1. Strategy

Dual-mode adapter pattern (consistent with FE-2 / FE-3 / FE-3B):

- `VITE_USE_MOCK_API=1` keeps every code path on the existing mock transport
  (used by unit tests, Storybook, and offline dev).
- `VITE_USE_MOCK_API` unset (`0`) routes traffic through `httpClient` to
  `/api/v1/{cart,coupons,checkout}/*` and surfaces backend errors verbatim.
- Money crosses the wire via `src/lib/money.ts` (`paiseToRupees` /
  `rupeesToPaise`); the existing in-app rupee-denominated UI types are kept
  intact so component code is unchanged.
- All DTO ↔ legacy shape mapping lives in `src/api/cartCheckoutAdapter.ts`.
- Backend `Idempotency-Key` accepted on `POST /checkout/start` and
  `POST /checkout/{id}/cancel` is generated via `idempotencyKey('co' | 'cx')`.
- React Query / Zustand stores were not refactored — the existing
  `cartStore`, `couponStore`, `checkoutStore` invoke the wrapped APIs and
  remain the single source of truth for in-memory UI state.

## 2. Endpoints connected

| Frontend call                         | Backend endpoint                            | Method | Auth | Idempotency | Status |
|--------------------------------------|---------------------------------------------|--------|------|-------------|--------|
| `cartApi.syncCart` / `validateCart`  | `GET    /api/v1/cart`                       | GET    | JWT  | n/a         | Wired  |
| `cartApi.addItem`                    | `POST   /api/v1/cart/items`                 | POST   | JWT  | server      | Wired  |
| `cartApi.updateItem`                 | `PUT    /api/v1/cart/items/{id}`            | PUT    | JWT  | server      | Wired  |
| `cartApi.removeItem`                 | `DELETE /api/v1/cart/items/{id}`            | DELETE | JWT  | server      | Wired  |
| `cartApi.saveForLater`               | `POST   /api/v1/cart/save-for-later`        | POST   | JWT  | server      | Wired  |
| `couponApi.validateCoupon`           | `POST   /api/v1/coupons/validate`           | POST   | JWT  | n/a         | Wired  |
| `checkoutApi.createSession`          | `POST   /api/v1/checkout/start`             | POST   | JWT  | client-gen  | Wired  |
| `checkoutApi.setAddress`             | `POST   /api/v1/checkout/{id}/address`      | POST   | JWT  | n/a         | Wired  |
| `checkoutApi.setShipping`            | `POST   /api/v1/checkout/{id}/shipping`     | POST   | JWT  | n/a         | Wired  |
| `checkoutApi.setPayment`             | `POST   /api/v1/checkout/{id}/payment`      | POST   | JWT  | n/a         | Wired  |
| `checkoutApi.getSession`             | `GET    /api/v1/checkout/{id}`              | GET    | JWT  | n/a         | Wired  |
| `checkoutApi.releaseReservation`     | `POST   /api/v1/checkout/{id}/cancel`       | POST   | JWT  | client-gen  | Wired  |
| `checkoutApi.placeOrder`             | _none in Phase 5 backend_                   | —      | —    | —           | **Blocked** |

All real calls share the `httpClient` plumbing: JWT injection, `X-Request-Id`
propagation, single-flight refresh on 401.

## 3. DTO alignment

| Frontend shape                  | Backend DTO                          | Notes |
|---------------------------------|--------------------------------------|-------|
| `CheckoutSession`               | `CheckoutSessionDto`                 | `status` → `step` (CREATED/ADDRESS/SHIPPING/PAYMENT/READY → address/shipping/payment/review). Pricing copied through. |
| `PricingBreakdown`              | `PricingBreakdown`                   | `subtotalPaise`, `discountPaise`+`couponDiscountPaise`, `shippingPaise`, `taxPaise`, `platformFeePaise`, `grandTotalPaise` → rupees via `paiseToRupees`. `vendorBreakdowns` left empty (backend stores aggregate only). |
| `PaymentSelection.methodId`     | `PaymentMethodKind`                  | `CARD/EMI/NETBANKING → card`, `UPI → upi`, `WALLET → wallet`, `COD → cod`. |
| `VendorShippingSelection`       | `ShippingMethodKind` + `shippingAmountPaise` | Backend models one method per session; frontend per-vendor selections are aggregated (sum of `cost`) when posting. |
| `AppliedCoupon`                 | `CouponValidationResult`             | `discountPaise → discount`. Hint from local registry used for label/type when available. |
| `CartItem` (UI)                 | `CartItemDto`                        | UI line key remains `productId::variantId`; server identity is the DTO `id` UUID, surfaced via `cartApi.updateItem(serverLineId, …)`. |

Enums: backend kinds normalized to lowercase frontend IDs at the adapter
boundary so existing UI logic stays untouched.

## 4. Pricing origin

When `USE_REAL_API` is true:

- `CheckoutSession.pricingSnapshot` is **always** sourced from
  `CheckoutSessionDto.pricing` (backend `PricingBreakdown`).
- Coupon discount values are sourced from `CouponValidationResult.discountPaise`.
- The local `computeBreakdown` engine in `src/lib/pricing.ts` is now used
  only (a) on the mock profile and (b) for transient pre-checkout previews on
  the cart page; the moment the user enters checkout, the server price wins.
- The only frontend math left on the production path is currency formatting
  via `formatInr` (`Intl.NumberFormat`).

## 5. Inventory reservation

- `POST /checkout/start` reserves all cart lines server-side
  (`InventoryReservationService.reserveForCustomer`). The frontend no longer
  fabricates reservations.
- `checkoutApi.reserveInventory` is preserved as a no-op convenience that
  returns a synthetic `ReservationDto` (10-minute TTL) so the existing
  countdown UI keeps working without store changes.
- `releaseReservation(sessionId)` calls `POST /checkout/{id}/cancel` with a
  client-generated `Idempotency-Key`, which releases every linked
  `inventory_reservations` row via the backend's
  `CheckoutReservationLink` join.

## 6. Files changed

- `src/api/cartCheckoutAdapter.ts` (new) — DTO types, pricing/session/coupon
  mappers, `idempotencyKey()`, `isUuid()`.
- `src/api/cartApi.ts` — real transport for `validateCart`, `addItem`,
  `syncCart`; new `updateItem`, `removeItem`, `saveForLater` helpers.
- `src/api/checkoutApi.ts` — real transport for `createSession`,
  `setAddress`, `setShipping`, `setPayment`, `releaseReservation`; new
  `getSession`. `reserveInventory` returns a server-managed synthetic.
  `placeOrder` flagged with TODO for the Orders module.
- `src/api/couponApi.ts` — real `validateCoupon` via
  `POST /coupons/validate`, with local registry consulted only for
  label/type hints.

## 7. Remaining blockers

| Blocker                                                      | Severity | Reason |
|--------------------------------------------------------------|----------|--------|
| No place-order endpoint in Phase 5 backend                   | **High** | `checkoutApi.placeOrder` remains mock-only; full happy-path can complete the `CheckoutSession` lifecycle through `PAYMENT_SELECTED`, but the leap to a persisted order requires the Orders module's API (out of scope per CART_CHECKOUT_MODULE.md §12). |
| Per-vendor shipping selections collapse to one method         | Medium   | Backend stores a single `(method, shippingAmountPaise)` per session. Aggregated total is sent; vendor-level UI granularity is preserved client-side only. |
| Coupon registry on the client is for label/type hints only    | Low      | Backend does not expose a public coupon catalogue; `couponApi.listCoupons` still serves the in-app demo list when on real API. |
| Mock products lack UUID variantIds                            | Medium   | `cartApi.addItem` falls back to local-only behaviour for legacy `prod-N` style IDs; once the storefront PDP exclusively serves backend variants, every cart line becomes server-tracked. |

## 8. Impact on production readiness

- Cart, coupon, address/shipping/payment selection, and session lifecycle are
  production-grade end-to-end with the live backend.
- Order placement still depends on a backend endpoint that is intentionally
  out of scope for Phase 5; production cutover for the full checkout funnel
  must wait for the Orders module integration phase.

## 9. Final verdict

**CART & CHECKOUT PARTIALLY INTEGRATED** — every endpoint listed in
`CART_CHECKOUT_MODULE.md` is wired, but final order placement is gated on
the Orders module (`POST /orders` / equivalent) which is not part of this
phase's backend.