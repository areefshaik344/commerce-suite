/**
 * Frozen DTO contracts for the catalog/product domain.
 *
 * These types describe the EXACT shape the future backend MUST return.
 * They are intentionally decoupled from internal mock types so the
 * transport layer can swap to HTTP without changes leaking into UI code.
 *
 * Wire envelope is the standard `ApiResponse<T>` / `PaginatedResponse<T>`
 * defined in `apiClient.ts`. These DTOs describe the `data` payload only.
 */
import type {
  CatalogProduct,
  CategoryNode,
  Inventory,
  ProductMedia,
  ProductReviewMeta,
  ProductStatus,
  ProductVariant,
} from "./catalog";

/* ----------------------------- Listings ----------------------------- */

/** Compact card-friendly projection — returned by list/search endpoints. */
export interface ProductCardDto {
  id: string;
  slug: string;
  name: string;
  brandName: string;
  categoryName: string;
  thumbnailUrl: string;
  price: number;
  compareAtPrice?: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  inStock: boolean;
  badges: ProductBadge[];
  status: ProductStatus;
}

export type ProductBadge = "FEATURED" | "TRENDING" | "BESTSELLER" | "NEW" | "LOW_STOCK" | "OUT_OF_STOCK";

/** Facet bucket returned by listing endpoint to drive dynamic filters. */
export interface FacetBucket {
  value: string;
  label: string;
  count: number;
}

export interface ProductFacetsDto {
  brand: FacetBucket[];
  category: FacetBucket[];
  priceRange: { min: number; max: number };
  rating: FacetBucket[];
  /** Dynamic per-category attribute facets, e.g. { RAM: [...], Color: [...] } */
  attributes: Record<string, FacetBucket[]>;
}

export interface ProductListDto {
  items: ProductCardDto[];
  facets: ProductFacetsDto;
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

/* ----------------------------- Details ----------------------------- */

export interface ProductDetailDto {
  product: CatalogProduct;
  defaultVariant: ProductVariant;
  inventoryByVariantId: Record<string, Inventory>;
  mediaByVariantId: Record<string, ProductMedia[]>;
  reviewSummary: ProductReviewMeta;
}

/* ----------------------------- Reviews ----------------------------- */

export interface ReviewDto {
  id: string;
  productId: string;
  userId: string;
  userName: string;
  rating: 1 | 2 | 3 | 4 | 5;
  title: string;
  comment: string;
  date: string;
  helpful: number;
  /** Backend-ready fields for future verified-purchase / moderation flows. */
  verifiedPurchase: boolean;
  status: "PUBLISHED" | "PENDING" | "REJECTED";
  vendorResponse?: { message: string; respondedAt: string };
}

export interface ReviewListDto {
  items: ReviewDto[];
  summary: ProductReviewMeta;
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

/* ---------------------------- Categories ---------------------------- */

export interface CategoryTreeDto {
  tree: CategoryNode[];
  /** Flat slug index for O(1) lookups on the client. */
  index: Record<string, CategoryNode>;
}

/* ----------------------------- Inventory ---------------------------- */

export interface InventoryDto extends Inventory {
  /** Pre-computed for callers — never trust client math on reservation. */
  available: number;
}