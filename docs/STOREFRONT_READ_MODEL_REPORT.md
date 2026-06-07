# Storefront Read Model Implementation Report — Phase BE-RM-1

**Date:** 2026-06-07
**Scope:** Add backend read-only storefront query APIs that supply the frontend with fully aggregated DTOs, unblocking Phase FE-3.

## Design principles

- **Read-only.** No domain service is modified. No FSM, ownership rule, or write path is touched.
- **Single endpoint per UI surface.** Each storefront screen calls one URL and receives everything it renders.
- **No N+1.** `StorefrontReadService` issues at most O(1) SQL statements per resource type, using `LATERAL` sub-selects and IN-clause batching.
- **Native SQL projections.** Avoids polluting JPA entities and keeps the read model decoupled from the write model.
- **MONEY_SPEC compliant.** All money values are paise (`long`); frontend converts on display.

## Files added

### DTOs (`com.commercesuite.catalog.dto.storefront`)
- `ProductCardDto` — id, slug, title, brand+category name, default-variant price, primary image, rating, reviewCount, stockStatus, availableQty, featured, vendorId.
- `ProductDetailDto` — product + brand + category + media[] + variants[] + defaultVariant + attributes[] + inventorySummary + reviewSummary + relatedProducts[].
- `ProductSearchResultDto` — `PageResponse<ProductCardDto>` + `StorefrontFacetsDto` + `SortOptionDto[]` + appliedSort.
- `StorefrontFacetsDto` — `BrandFilterDto[]`, `CategoryFacetDto[]`, `PriceRangeDto`, `RatingBucketDto[]`.
- `BrandFilterDto`, `CategoryFacetDto`, `PriceRangeDto`, `RatingBucketDto`, `SortOptionDto`.
- `ReviewSummaryDto` — averageRating, reviewCount, verifiedCount, ratingDistribution (1..5).
- `ReviewItemDto` — review + `customerDisplayName` joined from `profiles`.
- `ProductMediaItemDto`, `ProductAttributeItemDto`, `ProductVariantSummaryDto`, `InventorySummaryDto`.

### Service
- `StorefrontReadService` — JdbcTemplate-backed, `@Transactional(readOnly = true)`. Methods: `search`, `detailBySlug`, `reviewSummary`, `listReviews`, `suggest`, `brandFilter`.
- `StorefrontSearchCriteria` — keyword, categoryId/slug, brandIds[], vendorId, min/maxPricePaise, minRating.

### Controller
- `StorefrontController` at `/api/v1/storefront/**` (read-only, `permitAll` via `SecurityConfig`).

### Security
- `SecurityConfig` updated: `GET /api/v1/storefront/**` permitAll, mirroring `/api/v1/catalog/**`.

## APIs added

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/storefront/products` | `ProductSearchResultDto` |
| GET | `/api/v1/storefront/products/{slug}` | `ProductDetailDto` |
| GET | `/api/v1/storefront/products/{id}/reviews` | `PageResponse<ReviewItemDto>` |
| GET | `/api/v1/storefront/products/{id}/reviews/summary` | `ReviewSummaryDto` |
| GET | `/api/v1/storefront/brands` | `BrandFilterDto[]` |
| GET | `/api/v1/storefront/suggest?q=` | `String[]` |

## Query optimization

- **Product card list:** single SQL with `LEFT JOIN LATERAL` sub-selects for default variant (price/compareAt/currency), primary media, review aggregate, and inventory rollup — one row per product, one round-trip per page.
- **Facets:** computed once over the filtered set using `GROUP BY` per facet dimension (brand, category) and aggregate functions for price range and rating buckets. No per-product fan-out.
- **PDP:** 1 head query + 5 batched child queries (media, variants+inventory, attributes, inventory summary, review summary) + 1 related-products card query = 7 statements total, independent of result size.
- **Reviews list:** 1 count + 1 page query, with `profiles` joined inline for `customerDisplayName`.
- **Suggest:** single indexed `LIKE` query, capped at 10 rows.

## Frontend blockers removed

1. Product card price / rating / review count / image / stock — now in `ProductCardDto`.
2. Brand and category names on cards — joined server-side.
3. PDP media gallery, variants, attributes, inventory totals — `ProductDetailDto`.
4. Review rating distribution + customer display name — `ReviewSummaryDto` + `ReviewItemDto`.
5. Search facets (brand counts, category counts, price range, rating buckets) — `StorefrontFacetsDto`.
6. Sort options metadata — `SortOptionDto[]` carried in search response.
7. Search suggestions — `/storefront/suggest`.
8. Related products — embedded in `ProductDetailDto`.

## Verdict

**READY FOR FE-3 COMPLETION.**

All read models required by `FE_CATALOG_INTEGRATION_REPORT.md §6` are now
available. Frontend can switch `productApi`, `reviewApi`, and brand filter
UI to the `/api/v1/storefront/*` endpoints behind the existing
`VITE_USE_MOCK_API` flag without further backend work.