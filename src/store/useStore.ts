// Backward-compatible store that delegates to split stores
import { useAuthStore } from "./authStore";
import { useCartStore } from "./cartStore";
import { useWishlistStore } from "./wishlistStore";
import { useUIStore } from "./uiStore";
import type { UserRole, User } from "@/data/mock-users";
import type { Product } from "@/data/mock-products";

export type { VendorApplication } from "./authStore";

// Combined hook for backward compatibility — pages can gradually migrate to individual stores
export function useStore() {
  const auth = useAuthStore();
  const cart = useCartStore();
  const wishlist = useWishlistStore();
  const ui = useUIStore();

  return {
    // Auth
    currentUser: auth.currentUser,
    currentRole: auth.currentRole,
    isAuthenticated: auth.isAuthenticated,
    login: auth.login,
    loginWithCredentials: auth.loginWithCredentials,
    signupWithCredentials: auth.signupWithCredentials,
    registerVendor: auth.registerVendor,
    applyAsVendor: auth.applyAsVendor,
    logout: auth.logout,
    vendorApplications: auth.vendorApplications,
    approveVendor: auth.approveVendor,
    rejectVendor: auth.rejectVendor,

    // Cart
    cart: cart.cart,
    savedForLater: cart.savedForLater,
    addToCart: cart.addToCart,
    removeFromCart: cart.removeFromCart,
    updateCartQuantity: cart.updateCartQuantity,
    clearCart: cart.clearCart,
    cartTotal: cart.cartTotal,
    cartCount: cart.cartCount,
    saveForLater: cart.saveForLater,
    moveToCart: cart.moveToCart,
    removeSaved: cart.removeSaved,

    // Wishlist
    wishlist: wishlist.wishlist,
    toggleWishlist: wishlist.toggleWishlist,
    isInWishlist: wishlist.isInWishlist,

    // UI
    compareList: ui.compareList,
    addToCompare: ui.addToCompare,
    removeFromCompare: ui.removeFromCompare,
    clearCompare: ui.clearCompare,
    isInCompare: ui.isInCompare,
    recentlyViewed: ui.recentlyViewed,
    addToRecentlyViewed: ui.addToRecentlyViewed,
    searchQuery: ui.searchQuery,
    setSearchQuery: ui.setSearchQuery,
    searchHistory: ui.searchHistory,
    addToSearchHistory: ui.addToSearchHistory,
    clearSearchHistory: ui.clearSearchHistory,
    sidebarOpen: ui.sidebarOpen,
    setSidebarOpen: ui.setSidebarOpen,
    theme: ui.theme,
    setTheme: ui.setTheme,
  };
}
