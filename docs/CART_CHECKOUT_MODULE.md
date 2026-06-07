# Cart + Checkout Foundation — Phase 5

**Status:** Implemented June 7, 2026.

This module owns the customer-facing flow from "add to cart" through
"ready to place an order". It explicitly does **not** create orders,
payments, shipping, or notifications — those are later phases.

## 1. Entities

| Entity | Table | Notes |
|---|---|---|
| `Cart` | `carts` | One active cart per user (unique partial index). Supports guest carts via `guest_token`. Status FSM: `ACTIVE -> MERGED | ABANDONED | CONVERTED`. |
| `CartItem` | `cart_items` | Variant-based line. Unique `(cart_id, variant_id)`. Money in paise. |
| `SavedForLaterItem` | `saved_for_later_items` | Per-user wish-style stash, unique `(user_id, variant_id)`. |
| `Coupon` | `coupons` | Active window, usage/per-user limits, scope (GLOBAL/VENDOR/CATEGORY), type (PERCENTAGE/FIXED_AMOUNT/FREE_SHIPPING). |
| `CouponUsage` | `coupon_usage` | Records applications (committed=false during checkout, true after order placement). |
| `CheckoutSession` | `checkout_sessions` | Tracks selections + denormalised pricing + TTL + idempotency key. |
| `CheckoutReservationLink` | `checkout_reservation_links` | Joins checkout to inventory reservation IDs. |

All entities extend `AuditableEntity` (createdAt/updatedAt/by, soft delete via `deleted_at`).

## 2. Cart validation rules

Every cart mutation goes through `CartValidationService`:

- Variant exists and `active=true`.
- Product exists and `status=APPROVED`, not archived, not suspended.
- `qty > 0` and `qty <= (on_hand_qty - reserved_qty)` from inventory.

Invalid variants, rejected/archived/suspended products, and deleted rows
are blocked. Errors use `CONFLICT` so the frontend can surface a clear
message and trigger a refresh.

## 3. Coupon engine

Rules enforced by `CouponService.resolve(...)`:

- Coupon must be `active=true` and within `[starts_at, ends_at]`.
- `subtotal >= min_order_paise`.
- Total usage `< usage_limit_total`.
- Per-user usage `< usage_limit_per_user`.
- `VENDOR` scope requires at least one cart line for that vendor.
- `CATEGORY` scope hook is reserved (lookup deferred to product → category).

Discount math is performed in **integer paise**:

```
PERCENTAGE  : discount = round_half_up(subtotal * percent_off / 100), capped by max_discount_paise
FIXED_AMOUNT: discount = min(amount_off_paise, subtotal)
FREE_SHIPPING: shipping = 0; discount = 0
```

All math uses `BigDecimal` then converts to `long` paise exactly once
(MONEY_SPEC.md §3).

## 4. Pricing engine

`PricingEngine.calculate(items, coupon, shippingPaise, taxBps)` returns
a `PricingBreakdown`:

```
subtotal           = Σ unit_price_paise * qty
coupon_discount    = computed per type (see above)
discount           = coupon_discount  (room for line promos later)
shipping           = FREE_SHIPPING ? 0 : shippingPaise
tax                = round_half_up((subtotal − discount) * taxBps / 10000)
platform_fee       = round_half_up((subtotal − discount) * platformFeeBps / 10000)
grand_total        = (subtotal − discount) + shipping + tax + platform_fee
```

- Integer paise only; no `double` / `float` anywhere.
- `Math.addExact` / `Math.multiplyExact` guard overflow.
- Deterministic: identical inputs always produce identical outputs (covered by `PricingEngineTest`).

## 5. Checkout finite state machine

```
CREATED ──► ADDRESS_SELECTED ──► SHIPPING_SELECTED ──► PAYMENT_SELECTED ──► READY_FOR_ORDER ──► CONVERTED
    │                │                  │                    │                 │
    └──────────────► EXPIRED  /  CANCELLED  (terminal)  ◄────┴─────────────────┘
```

- Backstepping is allowed among `ADDRESS_SELECTED` / `SHIPPING_SELECTED` / `PAYMENT_SELECTED`.
- `CONVERTED` / `EXPIRED` / `CANCELLED` are terminal.
- Illegal transitions raise `409 CONFLICT`. Enforced by `CheckoutStateMachine`.

## 6. Reservation integration (RESERVATION_FSM.md)

- `POST /checkout/start` reserves every cart line via `InventoryReservationService.reserveForCustomer(...)`.
  Each reservation is linked through `CheckoutReservationLink` (active=true).
- `POST /checkout/cancel` releases all linked reservations with reason `EXPLICIT_RELEASE`.
- `CheckoutSweeperService` expires stale sessions and releases reservations with reason `ABANDONED`.
- Successful payment is **out of scope for this phase**. Reservations
  remain `RESERVED` after `READY_FOR_ORDER`; order creation later will
  call `commit(...)`.

## 7. Idempotency

`POST /checkout/start` and `POST /checkout/cancel` accept an
`Idempotency-Key` header (validated by `IdempotencyKey.isValid`).
The session table holds the key in
`(user_id, idempotency_key)` UNIQUE. Replays of `start` return the
existing session.

## 8. API contracts

| Method | Path | Auth | Description |
|---|---|---|---|
| GET    | `/api/v1/cart`                       | user | Active cart |
| POST   | `/api/v1/cart/items`                 | user | Add a cart item |
| PUT    | `/api/v1/cart/items/{id}`            | user | Update qty |
| DELETE | `/api/v1/cart/items/{id}`            | user | Remove line |
| POST   | `/api/v1/cart/save-for-later`        | user | Stash a cart item |
| GET    | `/api/v1/cart/save-for-later`        | user | List saved items |
| POST   | `/api/v1/coupons/validate`           | user | Preview coupon discount |
| POST   | `/api/v1/checkout/start`             | user | Start session, reserve inventory |
| POST   | `/api/v1/checkout/{id}/address`      | user | Select address |
| POST   | `/api/v1/checkout/{id}/shipping`     | user | Select method + shipping paise |
| POST   | `/api/v1/checkout/{id}/payment`      | user | Select method (+ coupon) |
| GET    | `/api/v1/checkout/{id}`              | user | Fetch session |
| POST   | `/api/v1/checkout/{id}/cancel`       | user | Cancel + release reservations |

Standard response envelope: `{ success, data, message, timestamp }`.

## 9. Events

- `CartItemAddedEvent` / `CartItemUpdatedEvent` / `CartItemRemovedEvent` / `SavedForLaterEvent` / `CartMergedEvent`
- `CouponAppliedEvent` / `CouponRejectedEvent` / `CouponCommittedEvent`
- `CheckoutStartedEvent` / `CheckoutAddressSelectedEvent` / `CheckoutShippingSelectedEvent`
  / `CheckoutPaymentSelectedEvent` / `CheckoutReadyForOrderEvent`
  / `CheckoutCancelledEvent` / `CheckoutExpiredEvent` / `CheckoutStateChangedEvent`

Published via Spring `ApplicationEventPublisher`; consumers will subscribe in later phases (analytics, audit, notifications).

## 10. Database migration

`V009__cart_checkout_module.sql` adds:

- 7 tables: `carts`, `cart_items`, `saved_for_later_items`, `coupons`,
  `coupon_usage`, `checkout_sessions`, `checkout_reservation_links`.
- 5 enums: `cart_status`, `coupon_type`, `coupon_scope`,
  `checkout_status`, `shipping_method_kind`, `payment_method_kind`.
- Unique partial indexes for "one active cart per user/guest".
- `GRANT`s for `authenticated` and `service_role`; RLS enabled by default.
- Money columns are `BIGINT` with non-negative `CHECK` constraints.

## 11. Tests

- `CheckoutStateMachineTest` — FSM transitions and terminal guarantees.
- `PricingEngineTest` — coupon math, BPS tax, free shipping, determinism.
- `CheckoutIT` — Testcontainers Postgres context-load smoke.

## 12. Out of scope for this phase

Orders, shipping providers, payment processing, notifications, audit, analytics. These will consume Phase 5 events and reservations in subsequent phases.