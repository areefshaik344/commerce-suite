# Phase FE-5B — Order Mock Removal (Final)

## Summary

The customer order surface no longer relies on placeholder data when the
backend is reachable. Mock data remains only for:

1. Demo / test profiles whose order IDs are not UUIDs (e.g. `ORD-…`).
2. Endpoints the backend does not yet expose (track-by-number, customer-
   wide refund list, shipment labels/packages).
3. `VITE_USE_MOCK_API=1` mode (still required for unit tests and offline
   demos).

## Placeholders Removed

| Placeholder | Source (pre-FE-5B) | Replacement |
|-------------|--------------------|-------------|
| `"Vendor"` constant on order items | `orderAdapter.itemFromBackend` | `OrderLineItemDto.vendorName` |
| `it.sku ?? "Product"` constant | `orderAdapter.itemFromBackend` | `OrderLineItemDto.productTitle` |
| `/placeholder.svg` hard-coded image | `orderAdapter.itemFromBackend` | `OrderLineItemDto.imageUrl` |
| `EMPTY_ADDRESS` on every detail | `orderFromBackend` | `parseAddress(AddressSnapshotDto.json)` |
| Synthesised single `ORDER_PLACED` event | `orderFromBackend` | `OrderTimelineDto.entries` |
| Empty `shipments`/`returns`/`refunds` on detail | `orderFromBackend` | dedicated storefront sub-endpoints |
| `mockOrderRecords` look-ups on shipment GET | `shipmentApi.getById` | `/storefront/shipments/{id}` |
| Tracking timeline synthesised from local mock | `shippingApi.getShipmentDetail` | `ShipmentSummaryDto.events[]` |
| `/admin/refunds` filtered client-side | `refundApi.listForOrder` | `/storefront/orders/{id}/refunds` |

## Retained Mocks (intentional)

| Path | Reason |
|------|--------|
| `orderManagementApi.list/getById` mock fallback | Required for `VITE_USE_MOCK_API=1` and for legacy `ORD-…` ids used by demo customer profile and Vitest suites. |
| `shipmentApi.updateStatus` mock branch | Vendor-portal demo still uses mock shipments — vendor-portal migration is a separate phase. |
| `shippingApi.trackByNumber` | Backend endpoint not implemented (`GET /shipments/by-tracking/{no}`). |
| `shippingApi.listForOrder` / `listForVendor` | Vendor / fulfillment portal surfaces; outside FE-5B scope. |
| `shippingApi.generateLabel` / `createPackage` / `listTasksForVendor` | No backend equivalents. |
| `refundApi.listForCustomer` | No cross-order storefront refund endpoint. |

## Files Touched

- `src/api/orderAdapter.ts`
- `src/api/orderManagementApi.ts`
- `src/api/shipmentApi.ts`
- `src/api/shippingApi.ts`
- `src/api/refundApi.ts`

No UI components were modified — the existing `OrderDetailsPage`,
`OrdersPage`, shipment tracking and refund summary components already
consumed the enriched `OrderRecord` shape that the storefront adapter
now populates with real data.

## Verification

- `tsc --noEmit` (frontend) passes.
- Storefront flag: `USE_REAL_API` (set via `VITE_USE_MOCK_API !== "1"`)
  unchanged; UUID gating routes legacy demo orders to the mock path.
- Money conversions go through `paiseToRupees` exclusively at the DTO
  boundary.