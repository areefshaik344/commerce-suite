/**
 * Adapter: Spring Boot storefront DTOs → legacy frontend `Product` / `Review`
 * shapes. Centralizes the paise→rupees money bridge and brand/category name
 * denormalization so the rest of the UI does not need to change.
 *
 * Source of truth: docs/STORE_FRONT_API_CATALOG.md
 */
import type { Product } from "@/data/mock-products";
import type { Review } from "@/data/mock-orders";
import { paiseToRupees } from "@/lib/money";

/* ---------------------------- backend DTOs ---------------------------- */
export interface BackendProductCardDto {
  id: string;
  slug: string;
  title: string;
  brandId: string | null;
  brandName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  defaultVariantId: string | null;
  pricePaise: number;
  compareAtPaise: number | null;
  currency: string;
  primaryImageUrl: string | null;
  primaryImageAlt: string | null;
  averageRating: number;
  reviewCount: number;
  stockStatus: "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";
  availableQty: number;
  featured: boolean;
  vendorId: string | null;
}

export interface BackendMediaDto { id: string; url: string; altText: string | null; mediaType: string; sortOrder: number; }
export interface BackendVariantDto { id: string; sku: string; pricePaise: number; compareAtPaise: number | null; currency: string; isDefault: boolean; availableQty: number; stockStatus: BackendProductCardDto["stockStatus"]; optionsJson: string | null; }
export interface BackendAttributeDto { code: string; label: string; value: string; unit: string | null; }
export interface BackendInventorySummaryDto { totalOnHand: number; totalReserved: number; totalAvailable: number; stockStatus: BackendProductCardDto["stockStatus"]; }
export interface BackendReviewSummaryDto {
  averageRating: number; reviewCount: number; verifiedCount: number;
  ratingDistribution: Record<string, number>;
}
export interface BackendProductDetailDto {
  id: string; slug: string; title: string; description: string | null;
  brandId: string | null; brandName: string | null;
  categoryId: string | null; categoryName: string | null;
  vendorId: string | null; vendorName: string | null;
  featured: boolean;
  media: BackendMediaDto[];
  variants: BackendVariantDto[];
  defaultVariant: BackendVariantDto | null;
  attributes: BackendAttributeDto[];
  inventorySummary: BackendInventorySummaryDto;
  reviewSummary: BackendReviewSummaryDto;
  relatedProducts: BackendProductCardDto[];
  tags?: string[];
}

export interface BackendReviewItemDto {
  id: string; productId: string; customerId: string;
  customerDisplayName: string | null;
  rating: number; title: string | null; reviewText: string | null;
  verifiedPurchase: boolean; helpfulCount: number; createdAt: string;
}

export interface BackendBrandFilterDto { id: string; name: string; slug: string; logoUrl: string | null; productCount: number; }

/** Spring Data `Page` envelope. */
export interface BackendPageResponse<T> {
  content: T[];
  number: number;       // 0-based
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface BackendStorefrontFacets {
  brands: BackendBrandFilterDto[];
  categories: { id: string; name: string; slug: string; productCount: number }[];
  priceRange: { minPaise: number; maxPaise: number };
  ratings: { minRating: number; productCount: number }[];
}

export interface BackendProductSearchResult {
  page: BackendPageResponse<BackendProductCardDto>;
  facets: BackendStorefrontFacets;
  sortOptions: { code: string; label: string }[];
  appliedSort: string;
}

/* ---------------------------- mappers ---------------------------- */

const computeDiscount = (pricePaise: number, comparePaise: number | null): number => {
  if (!comparePaise || comparePaise <= pricePaise) return 0;
  return Math.round(((comparePaise - pricePaise) / comparePaise) * 100);
};

export function cardToLegacyProduct(c: BackendProductCardDto): Product {
  const price = paiseToRupees(c.pricePaise);
  const originalPrice = c.compareAtPaise ? paiseToRupees(c.compareAtPaise) : price;
  return {
    id: c.id,
    name: c.title,
    slug: c.slug,
    description: "",
    price,
    originalPrice,
    discount: computeDiscount(c.pricePaise, c.compareAtPaise),
    rating: c.averageRating ?? 0,
    reviewCount: c.reviewCount ?? 0,
    images: c.primaryImageUrl ? [c.primaryImageUrl] : [],
    category: c.categoryName ?? "",
    subcategory: "",
    brand: c.brandName ?? "",
    inStock: c.stockStatus !== "OUT_OF_STOCK",
    stockCount: c.availableQty ?? 0,
    vendorId: c.vendorId ?? "",
    vendorName: "",
    tags: [],
    specs: {},
    variants: [],
    featured: c.featured,
    trending: false,
  };
}

export function detailToLegacyProduct(d: BackendProductDetailDto): Product {
  const defaultVariant = d.defaultVariant ?? d.variants[0] ?? null;
  const pricePaise = defaultVariant?.pricePaise ?? 0;
  const comparePaise = defaultVariant?.compareAtPaise ?? null;
  const price = paiseToRupees(pricePaise);
  const originalPrice = comparePaise ? paiseToRupees(comparePaise) : price;

  // Group raw option strings by group name (best-effort from optionsJson).
  const optionMap = new Map<string, Set<string>>();
  for (const v of d.variants) {
    if (!v.optionsJson) continue;
    try {
      const obj = JSON.parse(v.optionsJson) as Record<string, string>;
      for (const [k, val] of Object.entries(obj)) {
        if (!optionMap.has(k)) optionMap.set(k, new Set());
        optionMap.get(k)!.add(String(val));
      }
    } catch { /* ignore */ }
  }

  return {
    id: d.id,
    name: d.title,
    slug: d.slug,
    description: d.description ?? "",
    price,
    originalPrice,
    discount: computeDiscount(pricePaise, comparePaise),
    rating: d.reviewSummary?.averageRating ?? 0,
    reviewCount: d.reviewSummary?.reviewCount ?? 0,
    images: d.media.filter(m => m.mediaType === "IMAGE" || m.mediaType === "image" || !m.mediaType)
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(m => m.url),
    category: d.categoryName ?? "",
    subcategory: "",
    brand: d.brandName ?? "",
    inStock: (d.inventorySummary?.totalAvailable ?? 0) > 0,
    stockCount: d.inventorySummary?.totalAvailable ?? 0,
    vendorId: d.vendorId ?? "",
    vendorName: d.vendorName ?? "",
    tags: d.tags ?? [],
    specs: Object.fromEntries(d.attributes.map(a => [a.label, a.unit ? `${a.value} ${a.unit}` : a.value])),
    variants: Array.from(optionMap.entries()).map(([name, opts]) => ({ name, options: Array.from(opts) })),
    featured: d.featured,
    trending: false,
  };
}

export function reviewItemToLegacy(r: BackendReviewItemDto): Review & { verifiedPurchase: boolean } {
  return {
    id: r.id,
    productId: r.productId,
    userId: r.customerId,
    userName: r.customerDisplayName ?? "Verified Customer",
    rating: r.rating,
    title: r.title ?? "",
    comment: r.reviewText ?? "",
    date: r.createdAt,
    helpful: r.helpfulCount,
    verifiedPurchase: r.verifiedPurchase,
  };
}