# Phase FE-5 — Order Flow Validation

## Checkout → Order Creation

1. `checkoutStore.placeOrder(draft)` calls `checkoutApi.placeOrder({ draft })`.
2. When the active checkout session id is a UUID, the FE issues
   `POST /api/v1/orders { checkoutId }` with an `Idempotency-Key` header.
3. Backend `OrderCreationService.create(checkoutId, actor)` commits inventory
   reservations (Phase 6) and returns an `OrderDto`.
4. FE surfaces `{ orderId, placedAt }` to the success page.

Result: **wired end-to-end**.

## Order Creation → Reservation Commit

Reservation lifecycle is server-owned. The FE no longer mutates
`reservation` state on placement; `checkout/start` reserves and `POST /orders`
commits. Failure modes (`409 RESERVATION_EXPIRED`, `402 PAYMENT_FAILED`)
surface through the existing `ApiError` channel.

Result: **wired**; client-side reservation simulation is dormant in real mode.

## Order → Shipment

`useOrderShipments` → `shippingApi.listForOrder(orderId)` still reads from the
cached `OrderRecord.shipments` array because the backend does not yet expose
a `GET /orders/{id}/shipments` aggregate. Individual shipment fetches and
transitions (`GET /shipments/{id}`, `POST /shipments/{id}/status`,
`POST /shipments/{id}/tracking-events`) are fully wired.

Result: **partially wired** — single-shipment surfaces are live; order-scoped
shipment list relies on the order DTO payload.

## Shipment → Delivery

`ShipmentStatus` UI vocabulary maps to backend states as:

| UI | Backend |
|----|---------|
| `PACKING` | `CREATED` |
| `READY_TO_SHIP` | `READY_FOR_PICKUP` |
| `IN_TRANSIT` | `IN_TRANSIT` |
| `OUT_FOR_DELIVERY` | `OUT_FOR_DELIVERY` |
| `DELIVERED` | `DELIVERED` |
| `FAILED_DELIVERY` | `FAILED` *(also collapses `RETURN_TO_ORIGIN`)* |

Backend FSM (`ShipmentStatus.canTransitionTo`) enforces legality; FE no
longer pre-validates with `canTransitionShipment` on the real path.

## Return → Refund

- Customer-initiated: `POST /returns` (FE resolves `vendorOrderId` + per-item
  `qty` by re-reading the order detail).
- Vendor / admin transitions: `receive` → `complete` → backend RefundService
  publishes a refund request consumed by `GET /admin/refunds`.
- `ReturnStatus` mapping:

| UI | Backend |
|----|---------|
| `REQUESTED` | `REQUESTED` |
| `APPROVED` | `APPROVED` |
| `REJECTED` | `REJECTED` |
| `PICKED_UP` | `RECEIVED` |
| `REFUNDED` | `COMPLETED` |

## DTO contract alignment

| Concern | Source of truth | FE bridge |
|---------|-----------------|-----------|
| Money | backend paise (`long`) | `paiseToRupees` in `src/lib/money.ts` |
| Pagination | Spring `Page` zero-based | `+1` / `-1` in adapter |
| Enums | backend enum names | mapper tables in `src/api/orderAdapter.ts` |
| IDs | server-issued UUIDs | `isUuid` guard preserves legacy demo ids |

## Known UI gaps

- Product names, images and vendor display names degrade to placeholders
  for backend-sourced orders (see FE_ORDERS_INTEGRATION_REPORT §Gap 1).
- Order timeline shows a single synthesised `ORDER_PLACED` plus shipment
  events; richer event history awaits a backend read model.
- Address snapshot is empty until an address-projection endpoint is
  available.

These do not block the order lifecycle itself.