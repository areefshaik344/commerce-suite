# FE Catalog Mock Removal Report — Phase FE-3

**Date:** 2026-06-07

## Strategy

Same feature-flag pattern as Phase FE-2: `VITE_USE_MOCK_API` toggles
individual adapters between mock and real backend. Only adapters that
can be wired **without breaking the existing UI** are switched in this
phase.

## Mocks neutralised in production mode (`VITE_USE_MOCK_API=0`)

| Mock source                       | Replaced by                                              |
|-----------------------------------|----------------------------------------------------------|
| `mockCategories` (in category tree) | `GET /catalog/categories` (`BackendCategoryDto → CategoryNode`) |

## Mocks still in use (intentional, blocked by backend DTO gaps)

| Mock source                | Reason                                                    |
|----------------------------|-----------------------------------------------------------|
| `mockProducts`             | Backend `ProductDto` has no price / rating / media / brand-name. Aggregated `ProductCardDto` + `ProductDetailDto` required before cut-over. |
| `mockReviews`              | Backend `ProductReviewDto` lacks `customerName`; join required server-side before wiring. |
| `productApi.getBrands` (string[]) | UI consumes brand names; backend brand list is `BrandDto[]` (id-keyed). Filter UI refactor required. |
| `productApi.searchSuggestions` | No backend endpoint. |
| `productApi.getRelatedProducts` | No backend endpoint. |
| `productApi.getFeaturedProducts` / trending / deals | No public backend endpoints. |
| Vendor/admin product CRUD overlays (`productOverrides`, `vendorDrafts`) | Phase FE-4/FE-5 will migrate these to `/products/*` and `/admin/products/*`. |

## Files queued for deletion (later phase)

- `src/mocks/mockProducts.ts` — after Product DTO bridge ships.
- `src/mocks/mockReviews.ts` — after Review join lands.
- Mock branches inside `productApi.ts` and `reviewApi.ts` — after backend
  endpoints listed in `FE_CATALOG_INTEGRATION_REPORT.md §6` are available.

## Files changed

- `src/api/categoryApi.ts` — feature-flagged real-backend category tree.

## Verification

- Type-check passes.
- Mock mode (`.env.development`) keeps full catalog UX working.
- Real mode (`.env.production`) wires category tree to backend; other
  catalog surfaces gracefully fall back to mocks (logged) until the
  backend ships the aggregated read models.