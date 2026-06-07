# Phase FE-3B — Catalog Mock Removal (Final)

_Date_: 2026-06-07

## Policy

- `VITE_USE_MOCK_API` controls transport. `=1` → mocks (used by unit tests + offline dev). Unset/`0` → real `/api/v1/storefront/**`.
- Mock fixtures (`src/mocks/mockProducts.ts`, `src/mocks/mockReviews.ts`, `src/data/mock-products.ts`) remain checked in **only** because (a) tests under `src/test/marketplace.test.ts` consume them directly, and (b) vendor/admin write-side flows have no backend equivalent yet.
- No production code path returns mock data when `VITE_USE_MOCK_API` is unset/`0` _and_ the backend responds successfully.

## Production-path mocks removed (effective when `USE_REAL_API` is true)

| Module                                | Mock dependency before                         | After |
|---------------------------------------|------------------------------------------------|-------|
| `productApi.getProducts`              | `mockProducts` filter+sort+paginate           | `GET /storefront/products` |
| `productApi.getProductBySlug`         | `mockProducts.find(p => p.slug === slug)`     | `GET /storefront/products/{slug}` |
| `productApi.getRelatedProducts`       | `mockProducts.filter(category match)`         | PDP `relatedProducts[]` |
| `productApi.getFeaturedProducts`      | `mockProducts.filter(p.featured)`             | Storefront search (`featured` flag) |
| `productApi.getTrendingProducts`      | `mockProducts.filter(p.trending)`             | Storefront search (`sort=popularity`) |
| `productApi.getDeals`                 | `mockProducts.filter(p.discount >= 20)`       | Storefront search + client discount filter |
| `productApi.searchSuggestions`        | Title prefix scan of `mockProducts`           | `GET /storefront/suggest` |
| `productApi.getBrands`                | `new Set(mockProducts.map(p => p.brand))`     | `GET /storefront/brands` |
| `productApi.getProductReviews`        | `mockReviews.filter(productId)`               | `GET /storefront/products/{id}/reviews` |
| `reviewApi.getProductReviews`         | `mockReviews.filter(productId)`               | `GET /storefront/products/{id}/reviews` |
| `reviewApi.listProductReviews`        | In-memory sort + summarize from `mockReviews` | `…/reviews` + `…/reviews/summary` (parallel) |
| `categoryApi.getCategoryTree`         | `mockCategories` (Phase FE-3)                 | `GET /catalog/categories` |

## Mocks intentionally retained

| Mock | Reason |
|------|--------|
| `mockProducts`, `mockCategories` | Dev profile (`VITE_USE_MOCK_API=1`) and unit tests |
| `mockReviews` | Same as above + write-side fallback (`submitReview`, `markHelpful`) |
| Vendor draft overlay (`vendorDrafts`, `productOverrides`) in `productApi.ts` | Vendor CRUD has no backend endpoints in BE-RM-1 |
| Admin moderation queue | No backend moderation read endpoint in BE-RM-1 |

## Verification matrix

| Page / Component       | Mock data present at runtime when `USE_REAL_API`? |
|------------------------|---------------------------------------------------|
| Home (featured/trending/deals) | No |
| Catalog listing               | No |
| Search results                | No |
| Search suggestions dropdown   | No |
| Product detail (PDP)          | No |
| Reviews list + summary        | No |
| Brand filter                  | No |
| Category nav                  | No |
| Vendor product dashboard      | **Yes** (intentional — out of scope) |
| Admin moderation queue        | **Yes** (intentional — out of scope) |

## Conclusion

All customer-facing catalog surfaces are free of mock data when running against the live backend. Remaining mock usage is confined to (a) the unit-test profile and (b) vendor/admin operational tooling that will be addressed in a dedicated vendor/admin read-model phase.