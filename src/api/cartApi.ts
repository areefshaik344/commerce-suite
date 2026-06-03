/**
 * Cart API — mock transport for the cart resource.
 *
 * Frontend cart state is the source of truth today (Zustand + persist).
 * Once a backend exists, swap each method body to a real network call
 * keeping the same signatures and DTOs (see src/types/checkout.ts).
 */
import { simulateDelay, mockSuccess, type ApiResponse } from "./apiClient";
import type { Product } from "@/data/mock-products";
import type { Cart, CartItem, CartItemVariantSnapshot } from "@/types/checkout";
import { makeLineId } from "@/types/checkout";

/** Legacy back-compat re-exports — older callers expect these here. */
export { couponApi as cartCouponApi } from "./couponApi";
export type { CouponDto as Coupon } from "@/types/checkout";

export interface AddToCartRequest {
  product: Product;
  quantity: number;
  variant?: CartItemVariantSnapshot | null;
}

export interface CartValidationResult {
  ok: boolean;
  unavailable: string[];          // lineIds
  adjusted: { lineId: string; available: number }[]; // qty reduced to available
  message: string;
}

function buildItem(req: AddToCartRequest): CartItem {
  const variant = req.variant ?? null;
  return {
    lineId: makeLineId(req.product.id, variant?.variantId ?? null),
    productId: req.product.id,
    product: req.product,
    variant,
    quantity: req.quantity,
    unitPriceSnapshot: req.product.price,
    vendorId: req.product.vendorId,
    vendorName: req.product.vendorName,
    available: req.product.inStock,
    stockAtSync: req.product.stockCount,
    addedAt: Date.now(),
  };
}

export const cartApi = {
  /** Server-side validation hook — mock version checks inventory/availability snapshots. */
  async validateCart(items: CartItem[]): Promise<ApiResponse<CartValidationResult>> {
    await simulateDelay(200);
    const unavailable: string[] = [];
    const adjusted: { lineId: string; available: number }[] = [];
    for (const it of items) {
      if (!it.product.inStock) unavailable.push(it.lineId);
      else if (it.quantity > it.product.stockCount) {
        adjusted.push({ lineId: it.lineId, available: it.product.stockCount });
      }
    }
    return mockSuccess({
      ok: unavailable.length === 0 && adjusted.length === 0,
      unavailable, adjusted,
      message: unavailable.length || adjusted.length
        ? "Some items need attention"
        : "Cart is valid",
    });
  },

  /** Round-trip an item add — backend will return the canonical line on success. */
  async addItem(req: AddToCartRequest): Promise<ApiResponse<CartItem>> {
    await simulateDelay(150);
    return mockSuccess(buildItem(req), "Item added");
  },

  async syncCart(cart: Cart): Promise<ApiResponse<Cart>> {
    await simulateDelay(150);
    return mockSuccess({ ...cart, updatedAt: Date.now() });
  },
};