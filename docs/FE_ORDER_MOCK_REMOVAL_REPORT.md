# Phase FE-5 — Order Mock Removal Report

## Mocks removed from the production path

| Mock behaviour | Replaced by | File |
|----------------|-------------|------|
| Random ~5% payment-failure simulation on `placeOrder` | Real backend `POST /orders` errors propagate verbatim | `src/api/checkoutApi.ts` |
| Fake `ORD-<timestamp>` order id generation on place | Backend-issued UUID from `POST /orders` | `src/api/checkoutApi.ts` |
| In-memory order list / detail | `GET /orders`, `GET /orders/{id}` | `src/api/orderManagementApi.ts` |
| In-memory cancellation FSM | `POST /orders/{id}/cancel` (server FSM) | `src/api/orderManagementApi.ts` |
| In-memory return create / status mutation | `POST /returns`, `POST /returns/{id}/{receive\|complete\|reject}` | `src/api/orderManagementApi.ts` |
| In-memory shipment fetch + status mutation | `GET /shipments/{id}`, `POST /shipments/{id}/status` | `src/api/shipmentApi.ts` |
| In-memory tracking event recording | `POST /shipments/{id}/tracking-events` | `src/api/shippingApi.ts` |
| In-memory refund-by-order read | `GET /admin/refunds` (filtered) | `src/api/refundApi.ts` |

## Mocks retained (intentional)

| Mock | Reason |
|------|--------|
| `mockOrderRecords` dataset | Demo profile (`VITE_USE_MOCK_API=1`) and unit-test fixtures |
| Random `Math.random()` placement failure | Only reachable in mock mode |
| `shippingApi.trackByNumber`, `generateLabel`, `createPackage`, `listCouriers`, `listTasksForVendor` | No backend endpoint exists |
| `refundApi.listForCustomer` | No customer-scoped backend endpoint |
| `orderApi` (legacy `mock-orders` surface) | Used only by demo admin/vendor pages — not on the customer production path |

## Trigger

All real-API code paths are guarded by `USE_REAL_API` (from
`src/api/httpClient.ts`) **and** a UUID gate so legacy `ORD-…` /
`SHP-…` / `RET-…` identifiers continue to round-trip through the
mock dataset for tests and demos.