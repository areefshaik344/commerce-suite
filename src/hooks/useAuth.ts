import { useAuthStore } from "@/store/authStore";
import { useCallback } from "react";
import type { UserRole } from "@/data/mock-users";

/** Public roles exposed in upper-case for UI components. */
export const ROLE = { USER: "customer", VENDOR: "vendor", ADMIN: "admin" } as const;

export function useAuth() {
  const store = useAuthStore();

  const is = useCallback(
    (role: UserRole | UserRole[]) =>
      Array.isArray(role) ? role.includes(store.currentRole) : store.currentRole === role,
    [store.currentRole]
  );

  return {
    // State
    user: store.currentUser,
    role: store.currentRole,
    isAuthenticated: store.isAuthenticated,
    isBootstrapping: store.isBootstrapping,
    isRefreshing: store.isRefreshing,
    isAdmin: store.currentRole === "admin",
    isVendor: store.currentRole === "vendor",
    isCustomer: store.currentRole === "customer",

    // Predicates
    is,
    hasRole: (role: UserRole) => store.currentRole === role,

    // Actions
    login: store.login,
    loginAsync: store.loginAsync,
    loginWithCredentials: store.loginWithCredentials,
    signupWithCredentials: store.signupWithCredentials,
    registerVendor: store.registerVendor,
    applyAsVendor: store.applyAsVendor,
    logout: store.logout,
    bootstrap: store.bootstrap,
    refresh: store.refresh,

    // Vendor governance
    vendorApplications: store.vendorApplications,
    approveVendor: store.approveVendor,
    rejectVendor: store.rejectVendor,
  };
}
