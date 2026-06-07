# FE Catalog Integration Report — Phase FE-3

**Date:** 2026-06-07
**Scope:** Wire frontend catalog surfaces (categories, brands, products, reviews, search) to the certified Spring Boot backend.
**Transport:** `src/api/httpClient.ts` (axios + JWT injection + envelope unwrap).
**Toggle:** `VITE_USE_MOCK_API` (`1` = mock, `0` = real backend).

---

## 1. Backend surface available

| Resource   | Backend endpoint(s)                                                                 |
|------------|--------------------------------------------------------------------------------------|
| Categories | `GET /catalog/categories` (public tree), admin CRUD under `/admin/categories`        |
| Brands     | `GET /catalog/brands` (public list), admin CRUD under `/admin/brands`                |
| Products   | `GET /catalog/products` (search), `GET /catalog/products/{slug}`, vendor + admin CRUD |
| Variants   | `POST/GET /products/{id}/variants`                                                   |
| Reviews    | `GET /catalog/products/{id}/reviews`, `POST /products/{id}/reviews`, `DELETE /reviews/{id}` |

## 2. Frontend integration matrix

| Surface              | Frontend caller             | Status                | Notes |
|----------------------|------------------------------|-----------------------|-------|
| Category tree        | `categoryApi.getCategoryTree`| ✅ Integrated (read)  | Adapter `BackendCategoryDto → CategoryNode` |
| Category by slug     | `categoryApi.getCategoryBySlug` | 🟡 Mock-only        | Backend exposes tree only; resolved client-side from real tree once real mode is on. |
| Breadcrumbs          | `categoryApi.getBreadcrumbs` | 🟡 Mock-only          | Same as above — client-side derivation. |
| Brand list           | `productApi.getBrands`       | 🔴 Not integrated     | Returns derived `string[]`; backend exposes full `BrandDto[]`. UI refactor required to consume IDs. |
| Product list/search  | `productApi.getProducts`     | 🔴 Not integrated     | DTO mismatch (see §3). Mock retained as the only safe path until backend ships an aggregated `ProductCardDto`. |
| Product by slug      | `productApi.getProductBySlug`| 🔴 Not integrated     | Backend `ProductDto` lacks price/media/rating; UI needs aggregated payload. |
| Product by id        | `productApi.getProductById`  | 🔴 Not integrated     | Same as above. |
| Featured / trending / deals | productApi.getFeaturedProducts etc. | 🔴 Not integrated | Not in backend public API surface yet. |
| Search suggestions   | `productApi.searchSuggestions` | 🔴 Not integrated   | Backend has no `/catalog/suggest` endpoint. |
| Related products     | `productApi.getRelatedProducts`| 🔴 Not integrated   | No backend endpoint. |
| Reviews (read)       | `reviewApi.listProductReviews` | 🔴 Not integrated   | Backend returns `PageResponse<ProductReviewDto>`; mapping requires `customerName` join which backend does not yet expose. |
| Reviews (write)      | `reviewApi.submitReview`     | 🔴 Not integrated     | Endpoint exists (`POST /products/{id}/reviews`) but is not wired. |

## 3. DTO mismatch summary (blockers)

The frontend `Product` shape (`src/data/mock-products.ts`) is denormalized
with 30+ fields (price, MRP, discount %, rating, reviewCount, brand name,
stock count, image[], specs, tags, etc.). The backend `ProductDto` is
intentionally normalized:

| Frontend `Product` field | Backend source                                |
|--------------------------|-----------------------------------------------|
| `price`, `mrp`           | `ProductVariantDto.pricePaise`, `compareAtPaise` (paise → rupees) |
| `discount`               | computed from variant `compareAt` − `price`   |
| `rating`, `reviewCount`  | aggregated from `ProductReviewDto`            |
| `image`, `images[]`      | `ProductMediaDto[]`                           |
| `brand` (string)         | `BrandDto.name` looked up via `brandId`       |
| `category` (string)      | `CategoryDto.name` looked up via `categoryId` |
| `stockCount`, `inStock`  | inventory module (`/inventory/...`)           |
| `specs`, `tags`          | `ProductAttributeDefinition` joins            |

Wiring `getProducts` / `getProductBySlug` without server-side aggregation
would require **N+M+K** round-trips per page (product + variants + media +
reviews + brand + category + inventory) and break the existing list UI.
The accepted plan in `docs/INTEGRATION_GAP_ANALYSIS.md` is to ship a backend
`ProductCardDto` / `ProductDetailDto` before the frontend cuts over.

## 4. Money / enum / pagination alignment

- **Money:** backend uses `long pricePaise` (`MONEY_SPEC`); frontend uses
  `number` rupees. `src/lib/money.ts` (`paiseToRupees` / `rupeesToPaise`)
  is the bridge for all future product wiring.
- **Enums:** `ProductStatus` matches 1:1 (`DRAFT`, `PENDING_REVIEW`,
  `APPROVED`, `REJECTED`, `ARCHIVED`). No casing transform required.
- **Pagination:** backend `PageResponse<T>` is `{items, page, size, total, totalPages}`;
  frontend `PaginatedResponse<T>` is `{data, page, pageSize, total, totalPages}`.
  A 5-line adapter is sufficient at the call site once products are wired.

## 5. Verification

- Local with `VITE_USE_MOCK_API=1`: HomePage, ProductsPage, ProductDetailPage,
  Search, Filters, Sorting, Pagination all functional (mock data).
- Local with `VITE_USE_MOCK_API=0`:
  - ✅ Category tree (real)
  - 🔴 Products / reviews / brands surfaces fall back to mock or fail (see §2).
- Type-checks pass.

## 6. Remaining gaps

1. Backend must expose aggregated `ProductCardDto` (for list/search) and
   `ProductDetailDto` (for PDP) before the frontend can switch off mocks.
2. Backend search endpoint needs to support: text query, category slug filter,
   brand IDs, price range, rating, sort, page/pageSize. Current `/catalog/products`
   supports a subset only.
3. Backend should expose `GET /catalog/suggest?q=` for autocomplete.
4. Backend should expose `GET /catalog/products/{slug}/related` (or a
   recommendations endpoint).
5. Frontend `productApi.getBrands` must be re-typed from `string[]` to
   `BrandDto[]` once the brand catalog drives filtering by ID rather than name.
6. Review write path (`submitReview`) requires the auth context to be
   present — already supplied by `httpClient`, but the call site is not
   yet routed to it.

## 7. Files changed

- `src/api/categoryApi.ts` — real-backend adapter behind `USE_REAL_API`.

## 8. Verdict

**CATALOG PARTIALLY INTEGRATED.**

Only the category tree is wired to the real backend. Products, brands,
reviews, search, suggestions, and related products remain on the mock
transport because the backend DTOs are normalized and aggregated read
models (`ProductCardDto`, `ProductDetailDto`, `/catalog/suggest`,
`/catalog/products/{slug}/related`) do not yet exist. Estimated
remediation: 5–7 backend-days for the aggregated DTOs, plus 3–4
frontend-days to wire and remove mocks.