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
import { httpClient, USE_REAL_API } from "./httpClient";
import { ApiError } from "./apiClient";
import { isUuid, type BackendCartDto } from "./cartCheckoutAdapter";

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
    if (USE_REAL_API) {
      try {
        const res = await httpClient.get<BackendCartDto>("/cart");
        const serverQty = new Map<string, number>();
        for (const it of res.data.items) serverQty.set(it.variantId, it.qty);
        const adjusted: { lineId: string; available: number }[] = [];
        const unavailable: string[] = [];
        for (const it of items) {
          const vid = it.variant?.variantId ?? it.productId;
          if (!isUuid(vid)) continue; // mock product → skip server validation for this line
          const have = serverQty.get(vid);
          if (have === undefined) unavailable.push(it.lineId);
          else if (have < it.quantity) adjusted.push({ lineId: it.lineId, available: have });
        }
        return mockSuccess({
          ok: unavailable.length === 0 && adjusted.length === 0,
          unavailable, adjusted,
          message: unavailable.length || adjusted.length ? "Some items need attention" : "Cart is valid",
        });
      } catch (err) {
        if (!(err instanceof ApiError)) throw err;
      }
    }
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
    if (USE_REAL_API) {
      const variantId = req.variant?.variantId ?? req.product.id;
      if (isUuid(variantId)) {
        try {
          await httpClient.post<BackendCartDto>("/cart/items", { variantId, qty: req.quantity });
          return mockSuccess(buildItem(req), "Item added");
        } catch (err) {
          if (err instanceof ApiError) throw err;
        }
      }
    }
    await simulateDelay(150);
    return mockSuccess(buildItem(req), "Item added");
  },

  async syncCart(cart: Cart): Promise<ApiResponse<Cart>> {
    if (USE_REAL_API) {
      try {
        await httpClient.get<BackendCartDto>("/cart");
        // Server is source-of-truth for membership; UI keeps its local snapshot
        // for rich product info that the server cart does not carry.
        return mockSuccess({ ...cart, updatedAt: Date.now() });
      } catch (err) {
        if (!(err instanceof ApiError)) throw err;
      }
    }
    await simulateDelay(150);
    return mockSuccess({ ...cart, updatedAt: Date.now() });
  },

  /** Update quantity for a server cart line. Mock path is a no-op. */
  async updateItem(serverLineId: string, qty: number): Promise<ApiResponse<{ ok: true }>> {
    if (USE_REAL_API && isUuid(serverLineId)) {
      try {
        await httpClient.put<BackendCartDto>(`/cart/items/${serverLineId}`, { qty });
        return mockSuccess({ ok: true as const }, "Item updated");
      } catch (err) {
        if (err instanceof ApiError) throw err;
      }
    }
    return mockSuccess({ ok: true as const });
  },

  async removeItem(serverLineId: string): Promise<ApiResponse<{ ok: true }>> {
    if (USE_REAL_API && isUuid(serverLineId)) {
      try {
        await httpClient.delete<BackendCartDto>(`/cart/items/${serverLineId}`);
        return mockSuccess({ ok: true as const }, "Item removed");
      } catch (err) {
        if (err instanceof ApiError) throw err;
      }
    }
    return mockSuccess({ ok: true as const });
  },

  async saveForLater(serverLineId: string): Promise<ApiResponse<{ ok: true }>> {
    if (USE_REAL_API && isUuid(serverLineId)) {
      try {
        await httpClient.post("/cart/save-for-later", { cartItemId: serverLineId });
        return mockSuccess({ ok: true as const }, "Saved");
      } catch (err) {
        if (err instanceof ApiError) throw err;
      }
    }
    return mockSuccess({ ok: true as const });
  },
};