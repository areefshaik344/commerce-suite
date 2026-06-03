/**
 * Cart store — single source of truth for the customer's cart.
 *
 * Implements:
 *   - Variant-aware lines (deduped by lineId).
 *   - Inventory-aware quantity clamping at write time.
 *   - Optimistic mutations; never blocks UI on transport.
 *   - Save for later, vendor grouping (selectors live in lib/pricing).
 *
 * Backward compat: legacy callers still use `cart` / `savedForLater` /
 * `addToCart(product, qty, variants)` etc. Those signatures are preserved
 * via thin adapters so existing pages keep working while we migrate.
 */
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Product } from "@/data/mock-products";
import { CART } from "@/config/constants";
import {
  type CartItem, type CartItemVariantSnapshot, type SavedCartItem,
  makeLineId,
} from "@/types/checkout";

/** Legacy alias — old code imported `CartItem` from this module. */
export type { CartItem, SavedCartItem as SavedItem } from "@/types/checkout";

interface CartState {
  items: CartItem[];
  saved: SavedCartItem[];

  /** Add or merge a line. Quantity is clamped to per-line max and stock. */
  addItem: (product: Product, quantity?: number, variant?: CartItemVariantSnapshot | null) => CartItem | null;
  removeItem: (lineId: string) => void;
  setQuantity: (lineId: string, quantity: number) => void;
  incrementQuantity: (lineId: string, delta?: number) => void;
  clear: () => void;

  saveItemForLater: (lineId: string) => void;
  moveSavedToCart: (lineId: string) => void;
  removeSaved: (lineId: string) => void;

  /** Marks lines unavailable / clamps quantities after server validation. */
  reconcileAvailability: (changes: { lineId: string; available?: boolean; clampTo?: number }[]) => void;

  /* ---------------- Legacy back-compat surface (action aliases) ---------------- */
  /** @deprecated use `addItem` */
  addToCart: (product: Product, quantity?: number, variants?: Record<string, string>) => void;
  /** @deprecated use `removeItem(lineId)` */
  removeFromCart: (productId: string) => void;
  /** @deprecated use `setQuantity(lineId, qty)` */
  updateCartQuantity: (productId: string, quantity: number) => void;
  /** @deprecated use `clear` */
  clearCart: () => void;
  cartTotal: () => number;
  cartCount: () => number;
  /** @deprecated use `saveItemForLater(lineId)` */
  saveForLater: (productId: string) => void;
  /** @deprecated use `moveSavedToCart(lineId)` */
  moveToCart: (productId: string) => void;
}

function clampQty(q: number, stock: number): number {
  return Math.max(1, Math.min(q, CART.MAX_QUANTITY, Math.max(stock, 1)));
}

function buildItem(product: Product, quantity: number, variant: CartItemVariantSnapshot | null): CartItem {
  return {
    lineId: makeLineId(product.id, variant?.variantId ?? null),
    productId: product.id,
    product,
    variant,
    quantity,
    unitPriceSnapshot: product.price,
    vendorId: product.vendorId,
    vendorName: product.vendorName,
    available: product.inStock,
    stockAtSync: product.stockCount,
    addedAt: Date.now(),
  };
}

function variantFromMap(productId: string, map: Record<string, string> | undefined): CartItemVariantSnapshot | null {
  if (!map || Object.keys(map).length === 0) return null;
  const variantId = `${productId}::${Object.values(map).join("-")}`;
  return { variantId, sku: variantId, options: map };
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      saved: [],

      addItem: (product, quantity = 1, variant = null) => {
        if (!product.inStock) return null;
        const lineId = makeLineId(product.id, variant?.variantId ?? null);
        const items = get().items;
        const existing = items.find(i => i.lineId === lineId);
        if (items.length >= CART.MAX_ITEMS && !existing) return null;

        if (existing) {
          const next = clampQty(existing.quantity + quantity, product.stockCount);
          set({ items: items.map(i => i.lineId === lineId ? { ...i, quantity: next, product, unitPriceSnapshot: product.price, stockAtSync: product.stockCount, available: product.inStock } : i) });
          return { ...existing, quantity: next };
        }
        const created = buildItem(product, clampQty(quantity, product.stockCount), variant);
        set({ items: [...items, created] });
        return created;
      },

      removeItem: (lineId) => set({ items: get().items.filter(i => i.lineId !== lineId) }),

      setQuantity: (lineId, quantity) => {
        if (quantity <= 0) { get().removeItem(lineId); return; }
        set({
          items: get().items.map(i =>
            i.lineId === lineId ? { ...i, quantity: clampQty(quantity, i.product.stockCount) } : i
          ),
        });
      },

      incrementQuantity: (lineId, delta = 1) => {
        const it = get().items.find(i => i.lineId === lineId);
        if (!it) return;
        get().setQuantity(lineId, it.quantity + delta);
      },

      clear: () => set({ items: [] }),

      saveItemForLater: (lineId) => {
        const item = get().items.find(i => i.lineId === lineId);
        if (!item) return;
        set({
          items: get().items.filter(i => i.lineId !== lineId),
          saved: [...get().saved, {
            lineId: item.lineId, productId: item.productId, product: item.product,
            variant: item.variant, savedAt: Date.now(),
          }],
        });
      },

      moveSavedToCart: (lineId) => {
        const s = get().saved.find(i => i.lineId === lineId);
        if (!s) return;
        set({ saved: get().saved.filter(i => i.lineId !== lineId) });
        get().addItem(s.product, 1, s.variant);
      },

      removeSaved: (lineId) => set({ saved: get().saved.filter(i => i.lineId !== lineId) }),

      reconcileAvailability: (changes) => {
        const map = new Map(changes.map(c => [c.lineId, c]));
        set({
          items: get().items.map(i => {
            const c = map.get(i.lineId);
            if (!c) return i;
            const qty = c.clampTo !== undefined ? Math.min(i.quantity, c.clampTo) : i.quantity;
            return { ...i, available: c.available ?? i.available, quantity: Math.max(1, qty) };
          }),
        });
      },

      /* ---------------- Legacy adapters ---------------- */
      addToCart: (product, quantity = 1, variants) => {
        get().addItem(product, quantity, variantFromMap(product.id, variants));
      },
      removeFromCart: (productId) => {
        const target = get().items.find(i => i.productId === productId);
        if (target) get().removeItem(target.lineId);
      },
      updateCartQuantity: (productId, quantity) => {
        const target = get().items.find(i => i.productId === productId);
        if (target) get().setQuantity(target.lineId, quantity);
      },
      clearCart: () => get().clear(),
      cartTotal: () => get().items.reduce((s, i) => s + i.unitPriceSnapshot * i.quantity, 0),
      cartCount: () => get().items.reduce((s, i) => s + i.quantity, 0),
      saveForLater: (productId) => {
        const t = get().items.find(i => i.productId === productId);
        if (t) get().saveItemForLater(t.lineId);
      },
      moveToCart: (productId) => {
        const t = get().saved.find(i => i.productId === productId);
        if (t) get().moveSavedToCart(t.lineId);
      },
    }),
    {
      name: "markethub-cart",
      version: 2,
      partialize: (s) => ({ items: s.items, saved: s.saved }),
      migrate: (persisted, version) => {
        // v1 shape: { cart: [{product, quantity, selectedVariants}], savedForLater: [{product, savedAt}] }
        const p = persisted as { cart?: Array<{ product: Product; quantity: number; selectedVariants?: Record<string, string> }>; savedForLater?: Array<{ product: Product; savedAt: number }> } | undefined;
        if (version >= 2 || !p?.cart) return persisted as never;
        const items: CartItem[] = (p.cart ?? []).map(c => {
          const variant = variantFromMap(c.product.id, c.selectedVariants);
          return buildItem(c.product, c.quantity, variant);
        });
        const saved: SavedCartItem[] = (p.savedForLater ?? []).map(s => ({
          lineId: makeLineId(s.product.id, null),
          productId: s.product.id,
          product: s.product,
          variant: null,
          savedAt: s.savedAt,
        }));
        return { items, saved } as never;
      },
    }
  )
);