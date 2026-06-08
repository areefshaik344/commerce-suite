# Phase FE-6B — Vendor UI Migration Report

**Date:** 2026-06-08
**Scope:** Customer-of-record migration of the Vendor Portal pages from
mock fixtures (`@/data/mock-*`, `@/mocks/*`) to the real backend API
layer established in Phase FE-6.

## Pages migrated

| Page | Source data (before) | Source data (after) | States covered |
|------|----------------------|---------------------|----------------|
| `VendorDashboard` | `analyticsData`, `orders` (mock) | `vendorAnalyticsApi.overview`, `vendorAnalyticsApi.revenue("MONTH")`, `vendorOrderApi.list` | loading (skeletons), empty, error, success |
| `VendorOrders` | `mockOrders`, `mockUsers` | `vendorOrderApi.list` (paginated) | loading, empty/search-empty, error, success |
| `VendorOrderDetail` | `mockOrders.find` | `vendorOrderApi.get` + `accept/process/ship/deliver` mutations | loading, not-found, error, success |
| `VendorProducts` | `mockProducts.filter` (vendor=v-1) | `vendorProductApi.listMine` + `archive` mutation | loading, empty, search-empty, error, success |
| `VendorInventory` | local `useState` from `mockProducts` | `vendorInventoryApi.listMine` + `update` mutation | loading, empty, error, success |
| `VendorLowStockAlerts` | `mockProducts` threshold filter | `vendorInventoryApi.listMine` + `adjust` mutation | loading, empty (all-stocked), error, success |
| `VendorAnalytics` | hardcoded `salesData`, `conversionData`, `trafficSources`, `mockProducts` | `vendorAnalyticsApi.overview / orders / revenue` (HOUR/DAY/WEEK/MONTH) | loading, empty, error, success |
| `VendorFinancials` | hardcoded `revenueData`, `payouts` arrays | `vendorPayoutApi.list` + `vendorSettlementApi.list` | loading, empty, error, success |
| `VendorSettings` | `vendors[0]` from `mock-users` | `vendorApi.myProfile` (read) + `vendorApi.updateProfileV2` (write) | loading, error, success |
| `VendorReviews` | `reviews` from `mock-orders` | Placeholder (no backend endpoint yet) | empty (informational) |
| `VendorProductForm` | `categories` from `mock-products` | `categoryApi.getCategoryTree` + `vendorProductApi.create` | loading-aware select, validation, error |
| `VendorProductEdit` | `categories`, `products` from `mock-products` | `vendorProductApi.get` + `vendorProductApi.update` + `categoryApi.getCategoryTree` | loading, not-found, error, success |

## Endpoints connected

- `GET /vendor/analytics/overview` — dashboard KPIs (GMV, orders, AOV, checkout conversion)
- `GET /vendor/analytics/orders?period=…` — order count series
- `GET /vendor/analytics/revenue?period=…` — GMV series
- `GET /vendor/orders` (paginated) and `GET /vendor/orders/{id}`
- `POST /vendor/orders/{id}/{accept|process|ship|deliver}` — FSM transitions
- `GET /products/mine`, `GET /products/{id}`, `POST /products`, `PUT /products/{id}`, `POST /products/{id}/archive`
- `GET /inventory`, `PUT /inventory/{variantId}`, `POST /inventory/{variantId}/adjust`
- `GET /vendor/payouts`, `GET /vendor/settlements`
- `GET /vendors/me/profile`, `PUT /vendors/me`
- `GET /catalog/categories`

## Data shape & money handling

- All money fields rendered through `paiseToRupees` from `src/lib/money.ts`.
- Order/product/payout/settlement status badges mapped to backend enum
  values (uppercased), not the legacy lower-case mock vocabulary.
- All list views use TanStack Query for caching, refetch on
  mutation success via `queryClient.invalidateQueries`.

## State coverage matrix

Every migrated list/detail view exposes:
- **Loading** — `<Skeleton/>` rows or block-level skeletons.
- **Empty** — descriptive text ("No orders yet", "All SKUs are well-stocked").
- **Error** — destructive-tinted message when `isError` is true.
- **Success** — table/grid with real data.

Mutation actions (status transitions, archive, restock, profile save,
product create/update) all show toast feedback and disable buttons
while `isPending`.

## Backend-blocked surfaces (intentional placeholders / mock-only)

| Page | Reason |
|------|--------|
| `VendorReviews` | No `/api/v1/vendor/reviews` endpoint yet. Replaced mock list with a clean "coming soon" empty state. |
| `VendorBulkUpload`, `VendorAds`, `VendorCoupons`, `VendorDisputes`, `VendorTickets`, `VendorReturns`, `VendorStoreCustomization`, `VendorShipping`, `VendorPerformance`, `VendorPayoutHistory` | Out of FE-6B scope; remain on legacy stubs. None of them imported `@/data/mock-*` or `@/mocks/*`. |

## Production-readiness impact

- The Vendor Portal's primary daily-driver surfaces (Dashboard, Orders,
  Order Detail, Products, Product CRUD, Inventory, Low-Stock,
  Analytics, Financials, Settings) now run end-to-end against the
  Spring Boot backend when `VITE_USE_REAL_API` is on.
- No vendor page imports demo users, demo orders, demo products, demo
  payouts, or hardcoded KPI numbers anymore.
- Mock fallbacks live only inside the API layer (`USE_REAL_API=false`),
  preserving the offline/dev story without leaking into the UI.

## Final verdict

**VENDOR FULLY INTEGRATED** for all in-scope pages.

Remaining (non-blocking) work:
- Wire `VendorReviews` once `/vendor/reviews` ships.
- Wire ancillary modules (bulk upload, ads, coupons, disputes, tickets,
  returns, shipping, performance, store customization) once their
  backend controllers are exposed — none are blocking core seller flow.