import { useEffect, useMemo } from "react";
import { useRecentlyViewedStore } from "@/store/recentlyViewedStore";
import { products as allProducts } from "@/data/mock-products";

export function useRecentlyViewed(trackId?: string) {
  const items = useRecentlyViewedStore((s) => s.items);
  const track = useRecentlyViewedStore((s) => s.track);
  const clear = useRecentlyViewedStore((s) => s.clear);
  const remove = useRecentlyViewedStore((s) => s.remove);

  useEffect(() => {
    if (trackId) track(trackId);
  }, [trackId, track]);

  const products = useMemo(() => {
    const byId = new Map(allProducts.map((p) => [p.id, p]));
    return items.map((i) => byId.get(i.productId)).filter(Boolean);
  }, [items]);

  return { items, products, track, clear, remove };
}