import { useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useWishlistStore } from "@/store/wishlistStore";
import { useAuthStore } from "@/store/authStore";
import { usePermissions } from "@/hooks/usePermissions";
import { PERMISSIONS } from "@/lib/permissions";
import { toast } from "@/hooks/use-toast";

/**
 * Auth-aware wishlist hook with optimistic add/remove and login redirect
 * preservation. Components should call `toggle(productId)` and not worry
 * about state.
 */
export function useWishlist() {
  const user = useAuthStore((s) => s.currentUser);
  const { can } = usePermissions();
  const navigate = useNavigate();
  const location = useLocation();

  const wishlist = useWishlistStore((s) => s.wishlist);
  const add = useWishlistStore((s) => s.add);
  const remove = useWishlistStore((s) => s.remove);
  const clear = useWishlistStore((s) => s.clear);

  const requireAuth = useCallback((): boolean => {
    if (!user) {
      toast({ title: "Sign in required", description: "Log in to manage your wishlist." });
      navigate(`/login?from=${encodeURIComponent(location.pathname + location.search)}`);
      return false;
    }
    if (!can(PERMISSIONS.USE_WISHLIST)) {
      toast({ title: "Not available", description: "Your account can't use the wishlist right now.", variant: "destructive" });
      return false;
    }
    return true;
  }, [user, can, navigate, location]);

  const isWishlisted = useCallback((id: string) => wishlist.includes(id), [wishlist]);

  const toggle = useCallback(
    async (productId: string) => {
      if (!requireAuth()) return;
      try {
        if (wishlist.includes(productId)) {
          await remove(productId, user!.id);
          toast({ title: "Removed from wishlist" });
        } else {
          await add(productId, user!.id);
          toast({ title: "Added to wishlist" });
        }
      } catch (e) {
        toast({
          title: "Something went wrong",
          description: e instanceof Error ? e.message : "Please try again.",
          variant: "destructive",
        });
      }
    },
    [wishlist, add, remove, user, requireAuth]
  );

  return { wishlist, isWishlisted, toggle, clear, count: wishlist.length };
}