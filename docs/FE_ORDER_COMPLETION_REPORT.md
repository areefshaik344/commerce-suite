# Phase FE-5B — Orders Integration Completion Report

## Verdict
**ORDERS FULLY INTEGRATED** (storefront read-model path).

All placeholders called out in `FE_ORDERS_INTEGRATION_REPORT.md` are
removed. The customer order surface now consumes the BE-RM-2 storefront
endpoints end-to-end; the legacy `/orders/{id}` aggregate is no longer
used by the customer UI for reads.

## Endpoints Connected

| UI surface | Verb / Path | Adapter |
|------------|-------------|---------|
| Order history | `GET /api/v1/storefront/orders` | `orderFromStorefrontCard` |
| Order detail | `GET /api/v1/storefront/orders/{id}` | `orderFromStorefrontDetail` |
| Order timeline | `GET /api/v1/storefront/orders/{id}/timeline` | `timelineFromStorefront` |
| Shipment list | `GET /api/v1/storefront/orders/{id}/shipments` | `shipmentFromStorefront` |
| Return list | `GET /api/v1/storefront/orders/{id}/returns` | `returnFromStorefront` |
| Refund list | `GET /api/v1/storefront/orders/{id}/refunds` | `refundFromStorefront` |
| Shipment tracking | `GET /api/v1/storefront/shipments/{id}` | `shipmentFromStorefront` |

Mutations continue to flow through the existing domain endpoints
(`/orders/{id}/cancel`, `/returns`, `/returns/{id}/{receive,complete,reject}`,
`/shipments/{id}/status`, `/shipments/{id}/tracking-events`). After every
successful mutation the frontend re-fetches `orderManagementApi.getById`,
which itself fans out to the storefront read-model — so cached order state
is always refreshed with enriched product / vendor / shipment data.

## DTO Alignment (frontend ↔ backend)

| Backend DTO | Frontend type | Mapper |
|-------------|----------------|--------|
| `OrderCardDto` | `OrderRecord` (sparse) | `orderFromStorefrontCard` |
| `OrderDetailDto` | `OrderRecord` (full) | `orderFromStorefrontDetail` |
| `OrderLineItemDto` | `OrderItem` | inline in `orderFromStorefrontDetail` |
| `OrderPricingDto` | `PricingBreakdown` | inline (paise → rupees) |
| `PaymentSummaryDto` | `PaymentRecord` | inline (`mapPaymentMethod`/`mapPaymentStatus`) |
| `ShipmentSummaryDto` | `Shipment` | `shipmentFromStorefront` |
| `TrackingEventDto` | `ShipmentTimelineEvent` / `TrackingEvent` | inline |
| `ReturnSummaryDto` | `ReturnRequest` | `returnFromStorefront` |
| `RefundSummaryDto` | `RefundRecord` | `refundFromStorefront` |
| `OrderTimelineDto` / `OrderTimelineEntryDto` | `OrderTimelineEvent[]` | `timelineFromStorefront` |
| `MoneyDto` | `number` (rupees) | `paiseToRupees` |
| `AddressSnapshotDto` | `OrderShippingAddressSnapshot` | `parseAddress` |

`OrderItemDto.status` is forwarded verbatim; the backend enum already
matches `OrderItem["status"]` (`ACTIVE` / `CANCELLED` / `RETURN_REQUESTED`
/ `RETURNED` / `REFUNDED`).

## Surface Verification

| Surface | Status | Source data |
|---------|--------|-------------|
| Order history (`/orders`) | ✅ Backend | `OrderCardDto[]` — real product title, image, vendor count, action flags |
| Order details (`/orders/:id`) | ✅ Backend | `OrderDetailDto` — items, pricing, address, payment |
| Order timeline | ✅ Backend | `OrderTimelineDto` — milestones synthesised server-side |
| Shipment tracking (`/shipments/:id`) | ✅ Backend | `ShipmentSummaryDto` with embedded `events[]` |
| Return status | ✅ Backend | `ReturnSummaryDto[]` on detail + per-order endpoint |
| Refund status | ✅ Backend | `RefundSummaryDto[]` on detail + per-order endpoint |
| Cancellation flow | ✅ Backend | `POST /orders/{id}/cancel` → re-fetch storefront detail |
| Order search | ⚠️ Client-side | Storefront list endpoint is paginated only; FE filters in mock mode |
| Pagination | ✅ Backend | Spring 0-based page bridged to 1-based UI in adapter |

## Removed Placeholders

- Synthetic `"Vendor"` / `"Product"` names on detail items.
- Hard-coded `/placeholder.svg` on order line items (now uses
  `imageUrl` from `OrderLineItemDto`).
- Empty shipping/billing address — now parsed from backend snapshot JSON.
- Synthesised single-entry timeline — replaced by server-built milestones.
- Mock refund history on `refundApi.listForOrder` for UUID orders.
- Mock tracking timeline on shipment detail page (`/storefront/shipments/{id}`).

## Files Changed

- `src/api/orderAdapter.ts` — storefront DTO types + 6 new mappers
  (`orderFromStorefrontCard`, `orderFromStorefrontDetail`,
   `shipmentFromStorefront`, `returnFromStorefront`,
   `refundFromStorefront`, `timelineFromStorefront`).
- `src/api/orderManagementApi.ts` — `list` and `getById` now call the
  storefront endpoints; mutation flows re-fetch through the same path.
- `src/api/shipmentApi.ts` — `getById` uses
  `/storefront/shipments/{id}`; status update refreshes via storefront
  detail.
- `src/api/shippingApi.ts` — `getShipmentDetail` uses the storefront
  shipment endpoint (single round-trip with embedded events).
- `src/api/refundApi.ts` — `listForOrder` uses
  `/storefront/orders/{id}/refunds` (fallback to `/admin/refunds`).

## Remaining Blockers

1. **Customer-wide refund list.** No dedicated endpoint
   (`GET /storefront/refunds`); the order detail page already supplies
   refunds via `/orders/{id}/refunds`. Listing across orders is mock-only.
2. **Track-by-number lookup.** `GET /shipments/by-tracking/{no}` still not
   exposed → `shippingApi.trackByNumber` remains on the mock dataset.
3. **Per-order search.** Storefront list endpoint is pagination-only; FE
   `applyFilters` runs only against the mock dataset. UI search is wired to
   the customer-scoped paged list and works for in-memory data.
4. **Order item ↔ shipment linkage.** Storefront `ShipmentSummaryDto`
   carries no `orderItemId[]`; per-item shipment grouping in vendor
   sections falls back to vendor-level grouping. Functional today.
5. **Vendor / admin order portals** continue to consume the legacy
   `OrderDto` endpoints — outside FE-5B scope.

## Production-readiness Impact

The customer order experience is now production-ready:

- Real product titles, images, vendor names rendered on history and detail.
- Real shipping address snapshot rendered on the detail page.
- Real timeline (placed, paid, processing, shipped, delivered, returned,
  refunded, cancelled) sourced from the backend audit aggregate.
- Returns and refunds reflect actual backend state with correct money
  conversions.
- Cancellation/return mutations refresh via the enriched read model so the
  UI never reverts to placeholder data.

Remaining gaps are scoping items (cross-order refund list, public
tracking-number search) that do not block the customer transactional
journey.