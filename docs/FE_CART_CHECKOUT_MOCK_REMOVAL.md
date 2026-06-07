# Phase FE-4 — Cart & Checkout Mock Removal

_Date_: 2026-06-07

## Policy

Production-path traffic (`VITE_USE_MOCK_API` unset or `0`) no longer reads
cart, coupon, checkout pricing, or session state from in-memory fixtures.
Mock fixtures remain checked in **only** for the unit-test profile and the
offline dev profile (`VITE_USE_MOCK_API=1`).

## Removed from production paths (when `USE_REAL_API` is true)

| Module                          | Mock dependency before                                | After |
|---------------------------------|-------------------------------------------------------|-------|
| `cartApi.validateCart`          | In-memory product `inStock` / `stockCount` check      | `GET  /cart` — server qtys diffed against local lines |
| `cartApi.addItem`               | `buildItem(req)` local only                           | `POST /cart/items` (server cart authoritative) |
| `cartApi.syncCart`              | `{ ...cart, updatedAt: Date.now() }`                  | `GET  /cart` (server snapshot) |
| `cartApi.updateItem` (new)      | n/a                                                   | `PUT  /cart/items/{id}` |
| `cartApi.removeItem` (new)      | n/a                                                   | `DELETE /cart/items/{id}` |
| `cartApi.saveForLater` (new)    | n/a                                                   | `POST /cart/save-for-later` |
| `couponApi.validateCoupon`      | Static `REGISTRY` + local `computeCouponDiscount`    | `POST /coupons/validate` (server enforces window/limit/scope/min-order/per-user usage) |
| `checkoutApi.createSession`     | Fabricated `CS-${ts}` session object                  | `POST /checkout/start` with `Idempotency-Key` |
| `checkoutApi.setAddress`        | Echo-only                                             | `POST /checkout/{id}/address` |
| `checkoutApi.setShipping`       | Echo-only                                             | `POST /checkout/{id}/shipping` (aggregated `shippingAmountPaise`) |
| `checkoutApi.setPayment`        | Echo-only                                             | `POST /checkout/{id}/payment` |
| `checkoutApi.getSession` (new)  | n/a                                                   | `GET  /checkout/{id}` |
| `checkoutApi.releaseReservation`| In-memory `released: true`                            | `POST /checkout/{id}/cancel` with `Idempotency-Key` |
| Pricing snapshot                | `computeBreakdown(items, coupons, …)` client-side     | `CheckoutSessionDto.pricing` (subtotal, discount, shipping, tax, platform fee, grand total all server-computed in paise) |

## Production-path mocks retained

| Mock                                                 | Reason |
|------------------------------------------------------|--------|
| `couponApi.listCoupons` / `getCoupon` registry       | Backend exposes no public coupon catalogue; registry is a label/type hint only and is never used to compute discounts when `USE_REAL_API`. |
| `checkoutApi.placeOrder` random-failure mock         | No `POST /orders` (Orders module out of scope per CART_CHECKOUT_MODULE.md §12). |
| `checkoutApi.reserveInventory` synthetic ReservationDto | Backend reserves inventory implicitly inside `/checkout/start`; the synthetic preserves countdown UX without a store rewrite. |
| `checkoutApi.buildOrderDraft`                        | Pure client-side helper for the review screen; consumed only at the order-create boundary (still mocked). |
| Vendor-level `VendorShippingSelection` UI fan-out    | Backend models one shipping method per session; UI granularity is collapsed when posting and re-projected from session for display. |

## Verification matrix

| Surface                       | Mock data at runtime when `USE_REAL_API`? |
|-------------------------------|-------------------------------------------|
| Cart line add/update/remove   | No |
| Save for Later                | No |
| Coupon validation             | No (label/type hint is metadata-only) |
| Checkout start                | No |
| Address selection             | No |
| Shipping selection            | No (aggregate total) |
| Payment selection             | No |
| Pricing summary (subtotal/discount/coupon/shipping/tax/platform/grand) | No — all from `CheckoutSessionDto.pricing` |
| Checkout cancel / release     | No |
| Order placement               | **Yes** (Orders module not in this phase) |
| Vendor shipping breakdown UI  | Partially (client-projected; server has aggregate) |

## Conclusion

Every customer-facing money value on the cart and checkout pages originates
from the backend when running against the live API. Frontend math is
restricted to currency formatting. Residual mock usage is confined to (a)
the unit-test/offline profile and (b) the order-create boundary that the
Phase 5 backend intentionally does not yet expose.