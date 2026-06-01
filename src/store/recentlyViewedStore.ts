import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { RecentlyViewedItem } from "@/types/catalog";

const MAX_HISTORY = 20;

interface RecentlyViewedState {
  items: RecentlyViewedItem[];
  track: (productId: string) => void;
  remove: (productId: string) => void;
  clear: () => void;
}

export const useRecentlyViewedStore = create<RecentlyViewedState>()(
  persist(
    (set, get) => ({
      items: [],
      track: (productId) => {
        const filtered = get().items.filter((i) => i.productId !== productId);
        const next: RecentlyViewedItem[] = [
          { productId, viewedAt: new Date().toISOString() },
          ...filtered,
        ].slice(0, MAX_HISTORY);
        set({ items: next });
      },
      remove: (productId) =>
        set({ items: get().items.filter((i) => i.productId !== productId) }),
      clear: () => set({ items: [] }),
    }),
    { name: "markethub-recently-viewed", version: 1 }
  )
);