# Mock Data Inventory

**Scan date:** 2026-06-07
**Method:** ripgrep over `src/` for imports of `@/mocks/*`, `@/data/mock-*`, and uses of in-memory fixture objects.
**Result:** 93 source files depend on mock data, including the API transport itself.

## Root-cause file (CRITICAL)

| File | Purpose | Used by | Replacement | Severity |
|------|---------|---------|-------------|----------|
| `src/api/apiClient.ts` | "Mock API Client. Simulates network requests with configurable delay and error rates." | Every `src/api/*Api.ts` | Real `fetch`/`axios` client with `VITE_API_URL`, JWT, refresh, request-id, envelope unwrap | **CRITICAL** |

## Mock fixture files (CRITICAL)

| File | Purpose | Replacement API | Severity |
|------|---------|-----------------|----------|
| `src/mocks/mockProducts.ts` | Catalog, categories, banners, deals, featured/trending | `/api/v1/products`, `/api/v1/categories`, `/api/v1/admin/banners` | CRITICAL |
| `src/mocks/mockOrders.ts` | Orders + analytics aggregates | `/api/v1/orders`, `/api/v1/admin/analytics/*` | CRITICAL |
| `src/mocks/mockOrderRecords.ts` | Order detail | `/api/v1/orders/{id}` | CRITICAL |
| `src/mocks/mockUsers.ts` | Auth/profile fixtures (demo creds) | `/api/v1/auth/*`, `/api/v1/profile` | CRITICAL |
| `src/mocks/mockReviews.ts` | Product reviews | `/api/v1/products/{id}/reviews` | HIGH |
| `src/data/mock-products.ts` | Re-exports mockProducts (legacy shim) | n/a — delete after migration | HIGH |
| `src/data/mock-orders.ts` | Re-exports mockOrders + `analyticsData` | n/a — delete after migration | HIGH |
| `src/data/mock-users.ts` | Re-exports mockUsers | n/a — delete after migration | HIGH |

## Pages still consuming mocks directly (HIGH)

Customer: `WishlistPage`, `CheckoutPage` (`OrderReview`, `AddressSelector`), `ProductDetailPage` (recently-viewed), `HomePage` (banners/deals).
Vendor: `VendorDashboard`, `VendorProducts`, `VendorProductForm`, `VendorProductEdit`, `VendorOrders`, `VendorOrderDetail`, `VendorInventory`, `VendorLowStockAlerts`, `VendorAnalytics`, `VendorReviews`, `VendorSettings`.
Admin: `AdminAnalytics`, `AdminOrders`, `AdminProducts` (+ all admin pages via `adminApi.ts` → mock client).

## Stores backed by mock data (HIGH)

`authStore`, `profileStore`, `cartStore`, `productStore`, `useStore` — initialized from `src/mocks/*`. Must be re-seeded from real APIs via React-Query hooks.

## API service modules pointing at mock client (CRITICAL — 22 files)

All of `src/api/*Api.ts` (admin, audit, auth, cart, category, checkout, coupon, invoice, notification, order, orderManagement, payment, product, profile, refund, return, review, shipment, shipping, vendor, wishlist) call the mock `apiClient`. Each must be re-validated after the transport swap.

## Hardcoded credentials (HIGH — security)

Demo credentials live in `src/mocks/mockUsers.ts` and `mem://auth/identity-management`:
`rahul@example.com`, `priya@vendor.com`, `admin@marketplace.com` — all `password`.
**Must be removed** from any production build path. Acceptable only in `src/test/*` fixtures.

## Decommission plan

1. Land real `apiClient.ts`.
2. Migrate one bounded context at a time (start with `auth` → `profile` → `catalog` → `cart` → `checkout` → `orders` → `payments` → `admin/vendor analytics`).
3. After each context, delete its mock file and re-export shim.
4. Final step: delete `src/mocks/` and `src/data/mock-*`. Verify build still passes (only `src/test/**` may reference fixtures).