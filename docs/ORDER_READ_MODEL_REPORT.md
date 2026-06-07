# Phase BE-RM-2 — Order Read Model Report

## Verdict
**READY FOR FE-5 COMPLETION**

The customer storefront now has aggregated, enriched read endpoints that
supply every datum required by the order list, order detail, timeline,
shipment-tracking, return, and refund screens. All cross-domain joins are
performed server-side; the frontend no longer needs to call multiple
modules or fan out per-row.

## DTOs Added

All located under `com.commercesuite.orders.dto.storefront`.

| DTO | Purpose |
|-----|---------|
| `MoneyDto` | `{ amountPaise, currency }` — uniform money envelope. |
| `OrderCardDto` | Compact list-tile: number, date, status, total, primary image, product/vendor counts, action flags. |
| `OrderDetailDto` | Full order page: pricing, items, payment, shipments, returns, refunds, timeline. |
| `OrderLineItemDto` | Line with product title/slug/image, vendor name, qty, unit and line totals. |
| `AddressSnapshotDto` | Raw JSON address snapshot forwarded from the order. |
| `OrderPricingDto` | Subtotal, discount, coupon, shipping, tax, platform fee, grand total, coupon code. |
| `PaymentSummaryDto` | Method, status, amount, gateway reference, paid-at. |
| `ShipmentSummaryDto` | Vendor, status, carrier, tracking number, ETA, delivered-at, embedded tracking events. |
| `TrackingEventDto` | Type, description, location, occurred-at. |
| `ReturnSummaryDto` | Status, reason, note, refund amount, request/receive/resolve times. |
| `RefundSummaryDto` | Status, source, amount, reason, request/complete times. |
| `OrderTimelineDto` | Ordered list of `OrderTimelineEntryDto`. |
| `OrderTimelineEntryDto` | `{ code, label, occurredAt, reached }`. |

## Endpoints Added

Mounted under `/api/v1/storefront`, authenticated (JWT). Ownership enforced
by `OrderOwnershipGuard` so a customer only sees their own orders; ADMIN /
SUPER_ADMIN may read any.

| Method | Path | Returns |
|--------|------|---------|
| GET | `/orders` | `PageResponse<OrderCardDto>` |
| GET | `/orders/{id}` | `OrderDetailDto` |
| GET | `/orders/{id}/timeline` | `OrderTimelineDto` |
| GET | `/orders/{id}/shipments` | `List<ShipmentSummaryDto>` |
| GET | `/orders/{id}/returns` | `List<ReturnSummaryDto>` |
| GET | `/orders/{id}/refunds` | `List<RefundSummaryDto>` |
| GET | `/shipments/{id}` | `ShipmentSummaryDto` |

Security: `SecurityConfig` now matches `GET /api/v1/storefront/orders/**`
and `GET /api/v1/storefront/shipments/**` as `authenticated()` ahead of the
blanket storefront `permitAll()` rule.

## Frontend Blockers Removed

From `FE_ORDER_FLOW_VALIDATION.md` / `FE_ORDERS_INTEGRATION_REPORT.md`:

1. **Read-model enrichment** — product titles, slugs, primary images, vendor names are now resolved server-side.
2. **Order timeline** — `OrderTimelineDto` synthesises Created / Paid / Processing / Shipped / Delivered / Return Requested / Returned / Refunded / Cancelled milestones with timestamps, derived from `order_status_history`, shipments, returns, refunds, and payment intents.
3. **Customer-scoped refund and return reads** — `/orders/{id}/returns` and `/orders/{id}/refunds` available without admin tooling.
4. **Customer-scoped tracking** — `/orders/{id}/shipments` and `/shipments/{id}` return shipment metadata plus embedded tracking events.
5. **Order number** — every DTO now carries a stable `orderNumber` display string.
6. **Action affordances** — `cancellable` and `returnable` booleans are precomputed per order.

## Constraints Honoured

- No FSM, business rule, payment, or inventory logic changed.
- All DTOs are read-only projections; entities are untouched.
- No new tables, columns, or schema migrations introduced.
- All money values stay in paise; currency travels alongside.

## N+1 Mitigation

- `listForCustomer` fetches a single page of orders, then bulk-loads order items, products (`findAllById`), primary media, and vendor profiles in batches indexed by id.
- `detail` performs one read per related repository (items, vendor orders, shipments, returns, refunds, payments) and then bulk-resolves products/vendors.
- Tracking events are loaded once per shipment in the detail page (small fan-out bounded by shipment count, typically 1–3).

## Out of Scope (Future Phases)

- Tracking lookup by public tracking number (carrier-side webhook surface).
- Vendor-portal read models (different ownership rules; covered by upcoming vendor-portal phase).
- Multi-address checkout: backend currently models a single address snapshot per order; both `shippingAddress` and `billingAddress` reference it.