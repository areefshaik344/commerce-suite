/**
 * Catalog domain — normalized, backend-ready contracts.
 *
 * The existing `Product` interface in `src/data/mock-products.ts` is the
 * denormalized view used by current pages. These types are the normalized
 * model the rest of the catalog module is built on. A `fromLegacyProduct`
 * adapter bridges the two so we can roll out without breaking callers.
 */
import type { Product as LegacyProduct } from "@/data/mock-products";

export const PRODUCT_STATUS = {
  DRAFT: "DRAFT",
  PENDING_REVIEW: "PENDING_REVIEW",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  ARCHIVED: "ARCHIVED",
} as const;
export type ProductStatus = typeof PRODUCT_STATUS[keyof typeof PRODUCT_STATUS];

export interface Brand {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
}

export interface CategoryNode {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  icon?: string;
  productCount?: number;
  children?: CategoryNode[];
}

export interface ProductMedia {
  id: string;
  productId: string;
  url: string;
  alt: string;
  type: "image" | "video";
  position: number;
}

export interface ProductAttribute {
  key: string;
  label: string;
  value: string;
}

export interface Inventory {
  id: string;
  variantId: string;
  stock: number;
  reserved: number;
  lowStockThreshold: number;
  preorder?: boolean;
  preorderEta?: string;
}

export interface ProductVariant {
  id: string;
  productId: string;
  sku: string;
  /** option key -> option value, e.g. { Color: "Black", Size: "M" } */
  options: Record<string, string>;
  price: number;
  compareAtPrice?: number;
  mediaIds: string[];
  inventory: Inventory;
}

export interface ProductReviewMeta {
  averageRating: number;
  reviewCount: number;
  ratingHistogram: Record<1 | 2 | 3 | 4 | 5, number>;
}

export interface CatalogProduct {
  id: string;
  slug: string;
  name: string;
  description: string;
  brandId: string;
  brandName: string;
  categoryId: string;
  categoryName: string;
  ownerId: string;             // vendor user id
  ownerName: string;
  status: ProductStatus;
  tags: string[];
  attributes: ProductAttribute[];
  optionGroups: { name: string; values: string[] }[];
  defaultVariantId: string;
  variants: ProductVariant[];
  media: ProductMedia[];
  reviewMeta: ProductReviewMeta;
  featured: boolean;
  trending: boolean;
  createdAt: string;
  updatedAt: string;
  /** Free-text reason captured by moderators on REJECTED status. */
  moderationReason?: string;
}

export interface WishlistItem {
  productId: string;
  variantId?: string;
  addedAt: string;
}

export interface RecentlyViewedItem {
  productId: string;
  viewedAt: string;
}

/* ----------------------- Helpers ----------------------- */

export type InventoryStatus = "in_stock" | "low_stock" | "out_of_stock" | "preorder";

export function getInventoryStatus(inv: Inventory | undefined | null): InventoryStatus {
  if (!inv) return "out_of_stock";
  const available = inv.stock - inv.reserved;
  if (available <= 0) return inv.preorder ? "preorder" : "out_of_stock";
  if (available <= inv.lowStockThreshold) return "low_stock";
  return "in_stock";
}

/** Resolve a variant from partial option selection. Returns null if no exact match. */
export function selectVariant(
  product: Pick<CatalogProduct, "variants" | "defaultVariantId">,
  options: Record<string, string>
): ProductVariant | null {
  if (!Object.keys(options).length) {
    return product.variants.find(v => v.id === product.defaultVariantId) ?? null;
  }
  return (
    product.variants.find(v =>
      Object.entries(options).every(([k, val]) => v.options[k] === val)
    ) ?? null
  );
}

/** Which option values are still selectable given the current partial selection? */
export function getAvailableOptions(
  product: Pick<CatalogProduct, "variants" | "optionGroups">,
  selection: Record<string, string>
): Record<string, Set<string>> {
  const out: Record<string, Set<string>> = {};
  for (const g of product.optionGroups) out[g.name] = new Set();
  for (const v of product.variants) {
    for (const [key, val] of Object.entries(v.options)) {
      const matchesOthers = Object.entries(selection).every(
        ([sk, sv]) => sk === key || v.options[sk] === sv
      );
      if (matchesOthers) out[key]?.add(val);
    }
  }
  return out;
}

export function isOwner(userId: string | undefined, product: Pick<CatalogProduct, "ownerId">): boolean {
  return !!userId && userId === product.ownerId;
}

/* ----------------------- Inventory helpers ----------------------- */

/** Pre-checkout reservation snapshot — used by cart/checkout planning. */
export interface InventoryReservation {
  variantId: string;
  quantity: number;
  expiresAt: string;
}

/** Stock physically available right now (stock - reserved, clamped to 0). */
export function availableStock(inv: Inventory | undefined | null): number {
  if (!inv) return 0;
  return Math.max(0, inv.stock - inv.reserved);
}

/** Apply a pending reservation in-memory (returns a NEW Inventory record). */
export function applyReservation(inv: Inventory, quantity: number): Inventory {
  return { ...inv, reserved: Math.min(inv.stock, inv.reserved + Math.max(0, quantity)) };
}

/** Release a reservation in-memory (returns a NEW Inventory record). */
export function releaseReservation(inv: Inventory, quantity: number): Inventory {
  return { ...inv, reserved: Math.max(0, inv.reserved - Math.max(0, quantity)) };
}

/* ----------------------- Legacy adapter ----------------------- */

/**
 * Convert the legacy denormalized `Product` (used by existing mock data and
 * existing pages) into the normalized `CatalogProduct` model so the new
 * catalog UI can be wired without rewriting the mock dataset.
 */
export function fromLegacyProduct(p: LegacyProduct): CatalogProduct {
  const media: ProductMedia[] = p.images.map((url, i) => ({
    id: `${p.id}-m-${i}`,
    productId: p.id,
    url,
    alt: `${p.name} image ${i + 1}`,
    type: "image",
    position: i,
  }));

  const optionGroups = (p.variants ?? []).map(v => ({ name: v.name, values: v.options }));

  // Cartesian product of option groups → variants. Capped to avoid blowup.
  const combos: Record<string, string>[] = optionGroups.reduce<Record<string, string>[]>(
    (acc, g) => {
      if (!acc.length) return g.values.map(v => ({ [g.name]: v }));
      const next: Record<string, string>[] = [];
      for (const a of acc) for (const v of g.values) next.push({ ...a, [g.name]: v });
      return next;
    },
    []
  ).slice(0, 24);

  const variantList: ProductVariant[] = (combos.length ? combos : [{}]).map((opts, i) => {
    const variantStock = Math.max(0, Math.floor(p.stockCount / Math.max(1, combos.length || 1)));
    return {
      id: `${p.id}-v-${i}`,
      productId: p.id,
      sku: `${p.id.toUpperCase()}-${i + 1}`,
      options: opts,
      price: p.price,
      compareAtPrice: p.originalPrice > p.price ? p.originalPrice : undefined,
      mediaIds: media.length ? [media[0].id] : [],
      inventory: {
        id: `${p.id}-inv-${i}`,
        variantId: `${p.id}-v-${i}`,
        stock: i === 0 ? p.stockCount : variantStock,
        reserved: 0,
        lowStockThreshold: 10,
      },
    };
  });

  return {
    id: p.id,
    slug: p.slug,
    name: p.name,
    description: p.description,
    brandId: p.brand.toLowerCase().replace(/\s+/g, "-"),
    brandName: p.brand,
    categoryId: p.category.toLowerCase().replace(/\s+/g, "-"),
    categoryName: p.category,
    ownerId: p.vendorId,
    ownerName: p.vendorName,
    status: PRODUCT_STATUS.APPROVED,
    tags: p.tags,
    attributes: Object.entries(p.specs).map(([k, v]) => ({ key: k, label: k, value: v })),
    optionGroups,
    defaultVariantId: variantList[0].id,
    variants: variantList,
    media,
    reviewMeta: {
      averageRating: p.rating,
      reviewCount: p.reviewCount,
      ratingHistogram: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
    },
    featured: p.featured,
    trending: p.trending,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
}