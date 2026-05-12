import { useEffect, useMemo } from "react";
import { useProfileStore } from "@/store/profileStore";
import { useAuthStore } from "@/store/authStore";

/**
 * Single hook surface for the profile module.
 * Auto-loads everything for the authenticated user on mount.
 * Re-loads when the auth user changes.
 */
export function useProfile(autoLoad: boolean = true) {
  const authUser = useAuthStore((s) => s.currentUser);
  const store = useProfileStore();

  useEffect(() => {
    if (!autoLoad) return;
    if (authUser?.id && (!store.userId || store.userId !== authUser.id)) {
      void store.loadAll(authUser.id);
    }
  }, [autoLoad, authUser?.id, store]);

  const isLoading = store.status === "loading" && !store.profile;
  const isSaving = store.status === "saving";

  const defaultAddress = useMemo(
    () => store.addresses.find((a) => a.isDefault) ?? store.addresses[0] ?? null,
    [store.addresses]
  );

  return {
    // state
    profile: store.profile,
    addresses: store.addresses,
    sessions: store.sessions,
    preferences: store.preferences,
    vendorProfile: store.vendorProfile,
    defaultAddress,
    role: authUser?.role ?? "customer",
    isLoading,
    isSaving,
    savingScope: store.savingScope,
    error: store.error,

    // actions
    refresh: () => authUser?.id && store.loadAll(authUser.id),
    updatePersonal: store.updatePersonal,
    uploadAvatar: store.uploadAvatar,
    removeAvatar: store.removeAvatar,

    addAddress: store.addAddress,
    updateAddress: store.updateAddress,
    deleteAddress: store.deleteAddress,
    setDefaultAddress: store.setDefaultAddress,

    changePassword: store.changePassword,
    revokeSession: store.revokeSession,
    revokeAllOtherSessions: store.revokeAllOtherSessions,

    updatePreferences: store.updatePreferences,

    deactivate: store.deactivate,
    reactivate: store.reactivate,
    deleteAccount: store.deleteAccount,

    updateVendorProfile: store.updateVendorProfile,
  };
}