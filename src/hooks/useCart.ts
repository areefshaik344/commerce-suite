import { useMemo } from "react";
import { useCartStore } from "@/store/cartStore";
import { useCouponStore } from "@/store/couponStore";
import { computePricing, groupItemsByVendor } from "@/lib/pricing";
import type { Product } from "@/data/mock-products";
import type { CartItemVariantSnapshot } from "@/types/checkout";

export function useCart() {
  const items = useCartStore(s => s.items);
  const saved = useCartStore(s => s.saved);
  const coupons = useCouponStore(s => s.applied);

  const vendorGroups = useMemo(() => groupItemsByVendor(items), [items]);
  const pricing = useMemo(
    () => computePricing({ items, shipping: {}, coupons }),
    [items, coupons]
  );

  return {
    items, saved, vendorGroups, pricing, coupons,
    isEmpty: items.length === 0,
    itemCount: items.reduce((s, i) => s + i.quantity, 0),
    add: (product: Product, qty?: number, variant?: CartItemVariantSnapshot | null) =>
      useCartStore.getState().addItem(product, qty, variant ?? null),
    remove: (lineId: string) => useCartStore.getState().removeItem(lineId),
    setQuantity: (lineId: string, q: number) => useCartStore.getState().setQuantity(lineId, q),
    increment: (lineId: string, delta = 1) => useCartStore.getState().incrementQuantity(lineId, delta),
    clear: () => useCartStore.getState().clear(),
    saveForLater: (lineId: string) => useCartStore.getState().saveItemForLater(lineId),
    moveToCart: (lineId: string) => useCartStore.getState().moveSavedToCart(lineId),
    removeSaved: (lineId: string) => useCartStore.getState().removeSaved(lineId),
  };
}