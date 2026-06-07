# Phase FE-5 — Orders Integration Report

## Verdict: ORDERS PARTIALLY INTEGRATED

All Phase-6 backend endpoints currently exposed (Orders, Vendor Orders, Admin
Orders, Shipments, Tracking Events, Returns, Refunds) are now wired from the
frontend through `src/api/orderAdapter.ts`. Mock execution is retained only
when `VITE_USE_MOCK_API=1`, when the targeted entity id is not a server UUID
(e.g. legacy demo `ORD-…` records), or as a fallback for endpoints the
backend does not yet expose.

## Endpoints connected

| Surface | Verb / Path | Frontend call site |
|--------|-------------|--------------------|
| Place order | `POST /api/v1/orders` (Idempotency-Key) | `checkoutApi.placeOrder` |
| Customer order list | `GET /api/v1/orders` | `orderManagementApi.list` |
| Order detail | `GET /api/v1/orders/{id}` | `orderManagementApi.getById` |
| Cancel order | `POST /api/v1/orders/{id}/cancel` | `orderManagementApi.requestCancellation` |
| Request return | `POST /api/v1/returns` | `orderManagementApi.requestReturn` |
| Receive return | `POST /api/v1/returns/{id}/receive` | `orderManagementApi.updateReturnStatus(PICKED_UP)` |
| Complete return | `POST /api/v1/returns/{id}/complete` | `orderManagementApi.updateReturnStatus(REFUNDED)` |
| Reject return | `POST /api/v1/returns/{id}/reject` | `orderManagementApi.updateReturnStatus(REJECTED)` |
| Shipment detail | `GET /api/v1/shipments/{id}` | `shipmentApi.getById`, `shippingApi.getShipmentDetail` |
| Update shipment | `POST /api/v1/shipments/{id}/status` | `shipmentApi.updateStatus` |
| Tracking events | `GET/POST /api/v1/shipments/{id}/tracking-events` | `shippingApi.getShipmentDetail`, `shippingApi.recordTrackingEvent` |
| Refunds (admin) | `GET /api/v1/admin/refunds` | `refundApi.listForOrder` (filtered by orderId) |

## Architecture

- `src/api/orderAdapter.ts` centralises DTO mapping (paise → rupees via
  `src/lib/money.ts`) and bridges enum vocabularies between backend FSMs
  (docs/ORDER_FSM.md) and the existing UI shapes in `src/types/order.ts`.
- Status mappers collapse backend split states (`PARTIALLY_*`, `COMPLETED`,
  `CLOSED`, `PENDING_PAYMENT`) onto the closest UI status so existing badge,
  filter and timeline logic continues to work without UI changes.
- All write paths pass a UUID gate (`isUuid`) so legacy mock orders keep
  flowing through the in-memory dataset for tests / demo profile.
- `placeOrder` posts `{ checkoutId }` with an `Idempotency-Key` header
  mirroring the `/checkout/start` and `/checkout/{id}/cancel` pattern
  established in FE-4.

## Order state coverage (UI ↔ backend)

| Backend (`OrderStatus`) | UI (`OrderStatus`) |
|-------------------------|--------------------|
| `PENDING_PAYMENT`, `CREATED` | `CREATED` |
| `CONFIRMED` | `CONFIRMED` |
| `PROCESSING` | `PROCESSING` |
| `PARTIALLY_SHIPPED` | `PARTIALLY_SHIPPED` |
| `SHIPPED` | `SHIPPED` |
| `PARTIALLY_DELIVERED`, `DELIVERED`, `COMPLETED`, `CLOSED` | `DELIVERED` |
| `PARTIALLY_CANCELLED`, `CANCELLED` | `CANCELLED` |
| `PARTIALLY_RETURNED`, `RETURNED` | `RETURNED` |

`REFUNDED` is preserved at the payment-record layer
(`PaymentRecord.status`) because the backend models refund state on the
`refund_requests` table, not on the parent order.

## Remaining gaps

1. **Read-model enrichment** — Backend `OrderDto` / `OrderItemDto` carry only
   identifiers + paise. Product names, images, vendor display names and the
   shipping-address snapshot are filled with safe placeholders (`"Vendor"`,
   `"Product"`, empty address). A dedicated BE-RM-2 phase (mirroring
   BE-RM-1 storefront read model) is required for production-grade detail
   pages.
2. **Order timeline** — Backend exposes domain events via the outbox but no
   read endpoint; the FE renders a single synthesised `ORDER_PLACED` entry
   plus per-shipment tracking events.
3. **Customer refund list** — only the admin-scoped `GET /admin/refunds`
   exists. `refundApi.listForCustomer` remains mock-driven.
4. **Track-by-number** — `GET /shipments/by-tracking/{no}` is not yet
   implemented. `shippingApi.trackByNumber` falls back to the mock store.
5. **Vendor-order accept / process / ship / deliver** — wired controllers
   exist (`VendorOrderController`) but the vendor portal still uses mock
   transitions; not in FE-5 scope (handled by the upcoming vendor phase).
6. **Shipment label / package endpoints** — no backend equivalents; remain
   mock-only.

## Files changed

- `src/api/orderAdapter.ts` *(new)* — Backend DTO definitions + mappers.
- `src/api/orderManagementApi.ts` — Dual-mode list / detail / cancel /
  return / return-status.
- `src/api/shipmentApi.ts` — Dual-mode shipment fetch + status transition.
- `src/api/shippingApi.ts` — Dual-mode shipment detail + tracking events.
- `src/api/refundApi.ts` — Dual-mode admin refund list.
- `src/api/checkoutApi.ts` — `placeOrder` now posts `/orders` with the
  checkout session UUID and an idempotency key.

## Production-readiness impact

Checkout-to-order, order list, order detail, cancellation, return lifecycle
and shipment status flows are now backend-driven. UI polish for enriched
order detail (snapshots, addresses, timeline) is the remaining hard
blocker; functionally the customer can transact end-to-end against the real
backend.