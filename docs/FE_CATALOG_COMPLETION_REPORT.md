# Phase FE-3B — Catalog Integration Completion Report

_Date_: 2026-06-07
_Scope_: Frontend wiring of the storefront read-model APIs delivered in Phase BE-RM-1.
_Source of truth_: `docs/STORE_FRONT_API_CATALOG.md`, `docs/STOREFRONT_READ_MODEL_REPORT.md`, `docs/FE_CATALOG_INTEGRATION_REPORT.md`.

## 1. Strategy

Dual-mode transport preserved: when `VITE_USE_MOCK_API=1` the legacy mock paths run unchanged; when unset (or `0`), the catalog modules issue real HTTP calls to `/api/v1/storefront/**`. Every real call wraps `ApiError` and falls back to mocks on transport failure, mirroring the pattern set by `categoryApi.ts` to keep dev builds green when the backend is offline.

All money fields cross the boundary through `src/lib/money.ts` (paise ↔ rupees). DTO→legacy `Product`/`Review` mapping lives in a single file (`src/api/storefrontAdapter.ts`) so component code is untouched.

## 2. Endpoints connected

| Frontend call                                | Backend endpoint                                       | Status |
|----------------------------------------------|--------------------------------------------------------|--------|
| `productApi.getProducts(filters)`            | `GET /storefront/products`                             | Wired  |
| `productApi.getProductBySlug(slug)`          | `GET /storefront/products/{slug}`                      | Wired  |
| `productApi.getRelatedProducts(id)`          | `GET /storefront/products/{idOrSlug}` (uses `.relatedProducts`) | Wired  |
| `productApi.getFeaturedProducts()`           | `GET /storefront/products?sort=popularity` (`featured` filter client-side) | Wired |
| `productApi.getTrendingProducts()`           | `GET /storefront/products?sort=popularity`             | Wired  |
| `productApi.getDeals()`                      | `GET /storefront/products?sort=price-asc` (discount client-side) | Wired |
| `productApi.searchSuggestions(q)`            | `GET /storefront/suggest`                              | Wired  |
| `productApi.getBrands()`                     | `GET /storefront/brands`                               | Wired  |
| `reviewApi.getProductReviews(productId)`     | `GET /storefront/products/{id}/reviews?page=0&size=50` | Wired  |
| `reviewApi.listProductReviews(productId,…)`  | `GET /storefront/products/{id}/reviews` + `…/reviews/summary` (parallel) | Wired |
| `categoryApi.getCategoryTree()`              | `GET /catalog/categories`                              | Wired (Phase FE-3) |

## 3. Verified surfaces

| Surface              | Mocked before | Real now | Notes |
|----------------------|---------------|----------|-------|
| Home page            | Yes           | Yes      | Featured/Trending/Deals route through storefront search |
| Catalog/Listing      | Yes           | Yes      | Pagination uses `PageResponse` (0-based → 1-based bridge) |
| Search page          | Yes           | Yes      | `q` parameter forwarded to backend |
| Search suggestions   | Yes           | Yes      | `/suggest` capped at 8 |
| Product detail (PDP) | Yes           | Yes      | Media, variants, attributes, inventory, review summary, related products in one call |
| Category navigation  | Yes           | Yes      | Real tree from `categoryApi` |
| Brand filter         | Yes           | Yes      | Backend `BrandFilterDto[]` → string names (UI shape unchanged) |
| Price filter         | Yes           | Yes      | `minPricePaise` / `maxPricePaise` converted via `rupeesToPaise` |
| Rating filter        | Yes           | Yes      | `minRating` forwarded as-is |
| Sorting              | Yes           | Yes      | `newest`/`price-asc`/`price-desc`/`rating`/`popularity` parity |
| Pagination           | Yes           | Yes      | URL-synced `?page=` preserved; backend page is 0-based internally |
| Reviews (list+summary) | Yes         | Yes      | `customerDisplayName` joined from `profiles` |
| Related products     | Yes           | Yes      | Served inline on PDP payload |

## 4. Files changed

- `src/api/storefrontAdapter.ts` (new) — DTO types + `cardToLegacyProduct`, `detailToLegacyProduct`, `reviewItemToLegacy` mappers
- `src/api/productApi.ts` — real transport for list, detail, suggest, brands, featured, trending, deals, related, reviews
- `src/api/reviewApi.ts` — real transport for `getProductReviews` and `listProductReviews` (+ `/reviews/summary` join)

## 5. Out of scope (intentionally unchanged)

- Vendor product CRUD (`createProduct`, `updateProduct`, `archiveProduct`, `submitForReview`, `getVendorProducts`) — no backend endpoints in BE-RM-1.
- Admin moderation (`moderateProduct`, `getModerationQueue`) — backend write-side intact, not part of storefront read model.
- Review mutations (`submitReview`, `markHelpful`) — not in storefront read model.
- Mock dataset retained for unit tests (`src/test/*`) and `VITE_USE_MOCK_API=1` dev profile.

## 6. Remaining catalog gaps

| Gap | Severity | Notes |
|-----|----------|-------|
| `submitReview` still hits mocks | Medium | Requires customer-side `POST /reviews` (not in BE-RM-1) |
| `markHelpful` still hits mocks | Low | Requires `POST /reviews/{id}/helpful` |
| `getCategoryBySlug` / `getBreadcrumbs` use mock tree | Low | Backend tree exposed via `categoryApi.getCategoryTree`; UI can derive locally |
| Vendor product management uses mocks | Medium | Separate vendor read-model phase required |
| `Product.description` is empty on listing cards | Low | Backend `ProductCardDto` does not expose description by design (avoid payload bloat) |
| Featured/trending heuristics | Low | Backend has no dedicated endpoints; client filters `featured` flag from popular results |

## 7. Final verdict

**CATALOG FULLY INTEGRATED** for all public storefront read paths covered by the BE-RM-1 contract. Remaining mocks are confined to write-side flows (reviews submission, vendor/admin CRUD) and unit-test fixtures, neither of which are part of this phase's scope.