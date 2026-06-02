/**
 * Pure, memoizable selectors for the catalog domain.
 *
 * All product/variant/inventory derivations live HERE so that components
 * never reimplement business logic and Zustand stores never store derived
 * state. Each selector is a deterministic function of its inputs — safe
 * for `useMemo` and React.memo equality.
 */
import type {
  CatalogProduct,
  Inventory,
  InventoryStatus,
  ProductVariant,
} from "@/types/catalog";
import { getInventoryStatus, selectVariant } from "@/types/catalog";

/** Stock physically available to a customer right now. Never negative. */
export function getAvailableStock(inv: Inventory | undefined | null): number {
  if (!inv) return 0;
  return Math.max(0, inv.stock - inv.reserved);
}

/** Aggregate available stock across all variants of a product. */
export function getProductAvailableStock(p: Pick<CatalogProduct, "variants">): number {
  return p.variants.reduce((sum, v) => sum + getAvailableStock(v.inventory), 0);
}

/** Worst-case status across variants — used for product-card badges. */
export function getProductStockStatus(p: Pick<CatalogProduct, "variants">): InventoryStatus {
  const total = getProductAvailableStock(p);
  if (total <= 0) {
    const anyPreorder = p.variants.some((v) => v.inventory?.preorder);
    return anyPreorder ? "preorder" : "out_of_stock";
  }
  const anyLow = p.variants.some((v) => getInventoryStatus(v.inventory) === "low_stock");
  return anyLow ? "low_stock" : "in_stock";
}

/** Price range for the product across variants — used in card display. */
export function getProductPriceRange(p: Pick<CatalogProduct, "variants">): { min: number; max: number } {
  if (!p.variants.length) return { min: 0, max: 0 };
  const prices = p.variants.map((v) => v.price);
  return { min: Math.min(...prices), max: Math.max(...prices) };
}

/** Largest discount percent across variants (0-100). */
export function getMaxDiscountPercent(p: Pick<CatalogProduct, "variants">): number {
  let max = 0;
  for (const v of p.variants) {
    if (v.compareAtPrice && v.compareAtPrice > v.price) {
      const d = Math.round(((v.compareAtPrice - v.price) / v.compareAtPrice) * 100);
      if (d > max) max = d;
    }
  }
  return max;
}

/** Resolve the active variant for a partial selection, falling back to default. */
export function resolveActiveVariant(
  product: Pick<CatalogProduct, "variants" | "defaultVariantId">,
  selection: Record<string, string>
): ProductVariant {
  return (
    selectVariant(product, selection) ??
    product.variants.find((v) => v.id === product.defaultVariantId) ??
    product.variants[0]
  );
}

/** True when the requested option combination is sold (variant exists AND has stock). */
export function isSelectionPurchasable(
  product: Pick<CatalogProduct, "variants" | "defaultVariantId">,
  selection: Record<string, string>
): boolean {
  const v = selectVariant(product, selection);
  if (!v) return false;
  return getAvailableStock(v.inventory) > 0 || !!v.inventory?.preorder;
}

/** Build attribute-key → value map for the active variant; used for PDP specs. */
export function getActiveSpecs(
  product: Pick<CatalogProduct, "attributes">,
  variantOptions: Record<string, string>
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const a of product.attributes) out[a.key] = a.value;
  for (const [k, v] of Object.entries(variantOptions)) out[k] = v;
  return out;
}