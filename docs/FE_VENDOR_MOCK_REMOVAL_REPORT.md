# Phase FE-6 — Vendor Mock Removal Report

## Status

API-layer mocks for vendor surfaces are now superseded by typed,
backend-wired adapters (gated by `USE_REAL_API`). Page-level mock
imports remain pending FE-6B (UI migration).

## Decommissioned at the API layer

| Pre-FE-6 source | Replacement |
|-----------------|-------------|
| `mockVendors` look-up in `vendorApi.getVendorProfile` | `vendorApi.me` / `myProfile` (`/vendors/me*`) |
| `mockProducts` filter in `vendorApi.getVendorProducts` | `vendorProductApi.listMine` (`/products/mine`) |
| `mockOrders` filter in `vendorApi.getVendorOrders` | `vendorOrderApi.list` (`/vendor/orders`) |
| Hard-coded `monthlySales` in `vendorApi.getVendorAnalytics` | `vendorAnalyticsApi.overview/orders/revenue` (`/vendor/analytics/*`) |
| `mockProducts.push` in `vendorApi.createProduct` | `vendorProductApi.create` (`POST /products`) |
| `mockProducts.splice` in `vendorApi.deleteProduct` | `vendorProductApi.archive` (`POST /products/{id}/archive`) |
| (none — fake payout data) | `vendorPayoutApi.list/get` (`/vendor/payouts`) |
| (none — fake settlement data) | `vendorSettlementApi.list/get` (`/vendor/settlements`) |
| Manual KYC/bank state | `vendorApi.uploadDocument/listDocuments/upsertBank/listBankAccounts` |
| Manual application state | `vendorApi.apply/myApplications` |
| Inventory edits against `mockProducts` | `vendorInventoryApi.update/adjust/reserve/commit/release/upsertLowStockRule` |

## Retained mocks (intentional, page-level)

| File | Reason |
|------|--------|
| `VendorDashboard.tsx` | Still imports `analyticsData` / `orders` from mock-orders. Migration target: `vendorAnalyticsApi.overview`. |
| `VendorOrders.tsx`, `VendorOrderDetail.tsx` | Imports `mock-orders` / `mock-users`. Migration target: `vendorOrderApi`. |
| `VendorProducts.tsx`, `VendorProductForm.tsx`, `VendorProductEdit.tsx` | Imports `mock-products`. Migration target: `vendorProductApi`. |
| `VendorInventory.tsx`, `VendorLowStockAlerts.tsx` | Imports `mock-products`. Migration target: `vendorInventoryApi`. |
| `VendorAnalytics.tsx` | Imports `mock-products`. Migration target: `vendorAnalyticsApi`. |
| `VendorReviews.tsx` | `reviews` mock — no backend reviews API yet. |
| `VendorSettings.tsx` | `vendors` mock — migration target: `vendorApi.myProfile` / `updateProfileV2`. |
| `VendorBulkUpload.tsx`, `VendorAds.tsx`, `VendorCoupons.tsx`, `VendorDisputes.tsx`, `VendorTickets.tsx`, `VendorReturns.tsx` | No backend controllers yet — remain mock-only. |
| Legacy `vendorApi.getVendorProducts/Orders/Analytics/AllVendors/BySlug/createProduct/updateProduct/deleteProduct/updateVendorProfile` | Kept for backward compatibility with un-migrated pages; emit a `TODO` in docs only. |

## Test coverage

- `tsc --noEmit` clean.
- Real-backend paths fail fast on `401/403` (auth missing) so tests in
  mock mode (`VITE_USE_MOCK_API=1`) continue to operate against the
  in-memory fallbacks.

## Next steps (FE-6B)

Mechanical, page-by-page migration of the vendor portal screens from
`@/data/mock-*` imports to the new `vendor*Api` modules created here.
No new endpoints required; the API surface is complete for everything
backend currently exposes.