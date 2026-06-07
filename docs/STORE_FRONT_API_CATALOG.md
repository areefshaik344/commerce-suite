# Storefront API Catalog

Base path: `/api/v1/storefront` — all endpoints `GET`, public, envelope `{success,data,message,timestamp}`. Money fields in **paise**.

## GET /products
Search + facets + sort metadata.

Query: `q`, `categoryId`, `categorySlug`, `brandIds` (repeatable), `vendorId`, `minPricePaise`, `maxPricePaise`, `minRating`, `page` (0-based), `size` (≤60), `sort` ∈ `newest|price-asc|price-desc|rating|popularity`.

Returns `ProductSearchResultDto`:
```
{ page: PageResponse<ProductCardDto>,
  facets: { brands[], categories[], priceRange{minPaise,maxPaise}, ratings[{minRating,productCount}] },
  sortOptions: [{code,label}], appliedSort: string }
```

## GET /products/{slug}
Returns `ProductDetailDto` — product head, brand, category, `media[]`, `variants[]`, `defaultVariant`, `attributes[]`, `inventorySummary`, `reviewSummary`, `relatedProducts[]` (up to 8 cards in the same category).

## GET /products/{id}/reviews
Query: `page`, `size`. Returns `PageResponse<ReviewItemDto>` including `customerDisplayName` joined from `profiles`.

## GET /products/{id}/reviews/summary
Returns `ReviewSummaryDto` — `averageRating`, `reviewCount`, `verifiedCount`, `ratingDistribution: {1..5 → count}`.

## GET /brands
Returns `BrandFilterDto[]` — id, name, slug, logoUrl, productCount (APPROVED only).

## GET /suggest?q=&limit=
Returns `string[]` — up to 10 product-title suggestions.

## DTO reference

`ProductCardDto`: `id, slug, title, brandId, brandName, categoryId, categoryName, defaultVariantId, pricePaise, compareAtPaise?, currency, primaryImageUrl?, primaryImageAlt?, averageRating, reviewCount, stockStatus (IN_STOCK|LOW_STOCK|OUT_OF_STOCK), availableQty, featured, vendorId`.

`ProductVariantSummaryDto`: `id, sku, pricePaise, compareAtPaise?, currency, isDefault, availableQty, stockStatus, optionsJson`.

`InventorySummaryDto`: `totalOnHand, totalReserved, totalAvailable, stockStatus`.

`ProductMediaItemDto`: `id, url, altText, mediaType, sortOrder`.

`ProductAttributeItemDto`: `code, label, value, unit`.

`ReviewItemDto`: `id, productId, customerId, customerDisplayName, rating, title, reviewText, verifiedPurchase, helpfulCount, createdAt`.

## Notes
- All endpoints exclude soft-deleted rows (`deleted_at IS NULL`) and restrict products to `status='APPROVED'`.
- Read paths bypass JPA in favor of native SQL projections to avoid N+1 fan-out — domain services and FSMs are unchanged.
- Security: `/api/v1/storefront/**` is `permitAll` (parallel to `/api/v1/catalog/**`).