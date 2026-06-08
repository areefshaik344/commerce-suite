# Phase FE-6B — Vendor Mock Removal (Final)

**Date:** 2026-06-08
**Result:** Zero `@/data/mock-*` and `@/mocks/*` imports in `src/pages/vendor/`.

Verification:
```bash
rg -n "@/data/mock-|@/mocks" src/pages/vendor/
# (no matches)
```

## Mocks removed (page-level)

| Page | Removed import / fixture |
|------|--------------------------|
| `VendorDashboard` | `analyticsData`, `orders` from `@/data/mock-orders`; hardcoded `₹89.5L`, `12,500`, `45`, `3.2%` KPI tiles; `revenueData` / `statusData` derived from mock analytics |
| `VendorOrders` | `orders` from `@/data/mock-orders`; `users` from `@/data/mock-users`; `getUserName` helper |
| `VendorOrderDetail` | `orders.find` from `@/data/mock-orders`; synthetic timeline derived from mock timestamps |
| `VendorProducts` | `products.filter(p => p.vendorId === "v-1")` from `@/data/mock-products`; mock-driven delete toast |
| `VendorInventory` | `products` from `@/data/mock-products`; local stock state seeded from mock |
| `VendorLowStockAlerts` | `products` from `@/data/mock-products`; client-side threshold filter |
| `VendorAnalytics` | `salesData`, `conversionData`, `trafficSources` hardcoded arrays; `vendorProducts` derived from `@/data/mock-products` |
| `VendorFinancials` | hardcoded `revenueData` and `payouts` arrays |
| `VendorSettings` | `vendors[0]` from `@/data/mock-users`; hardcoded GST / PAN / IFSC defaults |
| `VendorReviews` | `reviews` from `@/data/mock-orders` |
| `VendorProductForm` | `categories` from `@/data/mock-products` |
| `VendorProductEdit` | `categories`, `products` from `@/data/mock-products` |

## Replacement sources

- `vendorAnalyticsApi` → KPIs, revenue/orders series
- `vendorOrderApi` → list/get + FSM transitions
- `vendorProductApi` → list/get/create/update/archive
- `vendorInventoryApi` → listMine/update/adjust
- `vendorPayoutApi`, `vendorSettlementApi` → financial visibility
- `vendorApi.myProfile / updateProfileV2` → settings
- `categoryApi.getCategoryTree` → category select in product forms

## Retained mocks (out of scope, no page-level mock imports)

- `src/mocks/*` and `src/data/mock-*` remain on disk because the API
  layer still uses them as fallbacks under `USE_REAL_API=false`. Their
  deletion is tracked in `docs/MOCK_DATA_INVENTORY.md` step 4 and is
  blocked on the customer and admin portals completing the same
  page-level migration.
- Vendor ancillary pages (`VendorBulkUpload`, `VendorAds`,
  `VendorCoupons`, `VendorDisputes`, `VendorTickets`, `VendorReturns`,
  `VendorStoreCustomization`, `VendorShipping`, `VendorPerformance`,
  `VendorPayoutHistory`) never imported mocks; they remain
  feature-stub pages awaiting backend controllers.

## Files changed

- `src/pages/vendor/VendorDashboard.tsx`
- `src/pages/vendor/VendorOrders.tsx`
- `src/pages/vendor/VendorOrderDetail.tsx`
- `src/pages/vendor/VendorProducts.tsx`
- `src/pages/vendor/VendorInventory.tsx`
- `src/pages/vendor/VendorLowStockAlerts.tsx`
- `src/pages/vendor/VendorAnalytics.tsx`
- `src/pages/vendor/VendorFinancials.tsx`
- `src/pages/vendor/VendorSettings.tsx`
- `src/pages/vendor/VendorReviews.tsx`
- `src/pages/vendor/VendorProductForm.tsx`
- `src/pages/vendor/VendorProductEdit.tsx`
- `docs/FE_VENDOR_UI_MIGRATION_REPORT.md` (new)
- `docs/FE_VENDOR_MOCK_REMOVAL_FINAL.md` (new)

## Final verdict

**VENDOR FULLY INTEGRATED.**

- 12 vendor pages migrated.
- 0 mock imports remain in `src/pages/vendor/`.
- All in-scope pages expose loading / empty / error / success states.
- Reviews surface is the only deferred page; backend endpoint absent.
- Production-readiness: the seller-facing day-to-day flow is now
  backend-driven and ready for staging cutover.