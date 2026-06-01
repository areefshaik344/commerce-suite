# Product Catalog Module — Implementation Plan

Build a production-grade catalog module on top of the existing auth/RBAC/profile architecture. Frontend-first, backend-ready contracts. No redesign of existing modules.

## Scope

Customer listing/search/PDP, wishlist, recently viewed, vendor product CRUD with ownership, admin moderation pipeline, scalable filter + variant + inventory system.

## Domain Model (`src/types/catalog.ts`)

Normalized entities (additive — keep existing `Product` interface in `mock-products.ts` working):

- `ProductStatus` = `DRAFT | PENDING_REVIEW | APPROVED | REJECTED | ARCHIVED`
- `Product` — core fields + `status`, `ownerId`, `brandId`, `categoryId`, `attributes`, `defaultVariantId`
- `ProductVariant` — `{ id, productId, sku, options: Record<string,string>, price, compareAtPrice, mediaIds[], inventoryId }`
- `Inventory` — `{ id, variantId, stock, lowStockThreshold, reserved, preorder }`
- `ProductMedia` — `{ id, productId, url, alt, type, position }`
- `ProductAttribute` — `{ key, label, value }`
- `Brand`, `Category` (nested via `parentId`)
- `WishlistItem`, `RecentlyViewedItem`, `ProductReview`

Helpers: `selectVariant(product, options)`, `getInventoryStatus(inv)`, `isOwner(user, product)`.

## API Layer

`api/productApi.ts` (extend):
- `listProducts(filters, page, pageSize)` with full filter set (category, brand[], priceRange, rating, inStock, discount, attributes, vendorId, status)
- `getProductBySlug`, `getProductById`
- `createProduct`, `updateProduct`, `archiveProduct` (ownership-validated server-side stub)
- `submitForReview`, `moderateProduct(id, action, reason)`
- `getVendorProducts(vendorId, filters)`
- `getModerationQueue(filters)`
- Extend `searchSuggestions` with brand/category hits

`api/categoryApi.ts` (new): `getCategoryTree`, `getCategoryBySlug`, `getBreadcrumbs(slug)`

`api/wishlistApi.ts` (extend): persisted stub, optimistic returns

All responses use the existing `ApiResponse<T>` / `PaginatedResponse<T>` envelope. Simulated 2% random failure on writes to exercise retry paths.

## State Layer (Zustand)

- `store/productStore.ts` — listing cache, current product, vendor/admin queues, loading + error states, memoized selectors
- `store/categoryStore.ts` — tree + flat map, bootstrap once
- `store/wishlistStore.ts` (extend) — auth-aware, optimistic add/remove with rollback, dedupe
- `store/recentlyViewedStore.ts` (new) — capped at 20, dedupe, persisted, user-scoped key

## Hooks

- `useProducts` — listing + pagination with URL sync (`?page=X`)
- `useProductFilters` — URL-synced filters, debounced 300ms, serialization
- `useWishlist` — guards unauth → login redirect with intent
- `useRecentlyViewed` — tracks PDP visits
- `useProductForm` — Formik+Yup for vendor create/edit with variant management

## Components (`src/components/product/`)

Atomic, composable, memoized where it matters:
- `ProductCard`, `ProductGrid`, `ProductFilters` (sidebar + mobile Sheet), `ProductSortBar`
- `ProductGallery` (thumbnails + zoom hover), `ProductVariantSelector` (disables unavailable combos)
- `ProductPriceBlock`, `InventoryBadge`, `WishlistButton`
- `ReviewList`, `RatingSummary` (reuse existing `WriteReviewForm`)
- `ProductSearchBar` (debounced, suggestions, recent + trending)
- `Breadcrumbs`, `EmptyProductState`, `ProductSkeleton`
- `ProductModerationActions` (admin approve/reject with reason)
- `VendorProductTable` (status chips, ownership filter)

## Pages

- `pages/customer/ProductsPage.tsx` — refactor for new filter system, keep URL pagination
- `pages/customer/ProductDetailPage.tsx` — refactor to use variants, gallery, related, recently-viewed
- `pages/vendor/VendorProducts.tsx` — table with moderation status
- `pages/vendor/VendorProductForm.tsx` — full variant editor, media manager, submit for review
- `pages/vendor/VendorProductEdit.tsx` — wraps form with ownership guard
- `pages/admin/AdminProducts.tsx` — moderation queue with bulk actions
- `pages/admin/AdminProductDetail.tsx` — approve/reject panel

## RBAC Integration

Use existing `<Can>`, `usePermissions`, `PermissionRoute`. Permissions: `MANAGE_PRODUCTS` (vendor, scoped via `ownsResource`), `MODERATE_PRODUCTS` (admin). No inline role strings.

## URL Sync Contract

```
/products?page=2&category=electronics&brand=Apple,Samsung
        &minPrice=1000&maxPrice=50000&rating=4&inStock=1
        &sort=price-asc&q=phone&attr.color=Black
```

Filter hook reads/writes via `useSearchParams`, debounced 300ms. Active chips individually removable.

## Performance

- `React.memo` on cards and variant option buttons
- `useMemo`/`useCallback` on expensive selectors
- `loading="lazy"` + `decoding="async"` on images
- Skeleton placeholders for grid + PDP
- List structure ready for `@tanstack/react-virtual` (already noted in production-gaps memory)

## Not Touching

- Auth, RBAC primitives, profile, account lifecycle
- Cart/checkout/order
- Existing shared `ProductCard` callers — new `components/product/ProductCard` becomes canonical; old shared file re-exports it for compatibility

## Deliverable

All listed files created or refactored, wired into the App router where pages already exist, TypeScript strict, no console errors, preview verified.
