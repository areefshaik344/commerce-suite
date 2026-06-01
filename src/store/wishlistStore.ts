import { create } from "zustand";
import { persist } from "zustand/middleware";
import { wishlistApi } from "@/api/wishlistApi";

interface WishlistState {
  wishlist: string[];
  pending: Set<string>;
  toggleWishlist: (productId: string) => void;
  isInWishlist: (productId: string) => boolean;
  /** Optimistic add — rolls back on API failure. */
  add: (productId: string, userId?: string) => Promise<void>;
  /** Optimistic remove — rolls back on API failure. */
  remove: (productId: string, userId?: string) => Promise<void>;
  clear: () => void;
}

export const useWishlistStore = create<WishlistState>()(
  persist(
    (set, get) => ({
      wishlist: [],
      pending: new Set<string>(),
      toggleWishlist: (productId) => {
        const { wishlist } = get();
        set({
          wishlist: wishlist.includes(productId)
            ? wishlist.filter(id => id !== productId)
            : [...wishlist, productId],
        });
      },
      isInWishlist: (productId) => get().wishlist.includes(productId),
      add: async (productId, userId) => {
        const { wishlist } = get();
        if (wishlist.includes(productId)) return; // dedupe
        set({ wishlist: [...wishlist, productId] }); // optimistic
        try {
          await wishlistApi.addToWishlist(userId ?? "guest", productId);
        } catch {
          // rollback
          set({ wishlist: get().wishlist.filter((id) => id !== productId) });
          throw new Error("Could not add to wishlist");
        }
      },
      remove: async (productId, userId) => {
        const prev = get().wishlist;
        if (!prev.includes(productId)) return;
        set({ wishlist: prev.filter((id) => id !== productId) }); // optimistic
        try {
          await wishlistApi.removeFromWishlist(userId ?? "guest", productId);
        } catch {
          set({ wishlist: prev });
          throw new Error("Could not remove from wishlist");
        }
      },
      clear: () => set({ wishlist: [] }),
    }),
    {
      name: "markethub-wishlist",
      // Don't serialize the transient `pending` Set.
      partialize: (s) => ({ wishlist: s.wishlist }) as unknown as WishlistState,
    }
  )
);
