/**
 * Profile store — single source of truth for the authenticated user's
 * profile, addresses, sessions, preferences and (when applicable) vendor
 * business profile.
 *
 * - All writes are optimistic with rollback on failure.
 * - All reads cache by userId; switching users invalidates caches.
 * - Stays in sync with the auth store: when the auth user changes, we
 *   invalidate everything that doesn't belong to the new user.
 */
import { create } from "zustand";
import { profileApi } from "@/api/profileApi";
import { ApiError } from "@/api/apiClient";
import { useAuthStore } from "@/store/authStore";
import type {
  Address, NotificationPreferences, VendorBusinessProfile, DeviceSession, User,
} from "@/data/mock-users";

type Status = "idle" | "loading" | "saving" | "error";

interface ProfileState {
  userId: string | null;
  profile: User | null;
  addresses: Address[];
  sessions: DeviceSession[];
  preferences: NotificationPreferences | null;
  vendorProfile: VendorBusinessProfile | null;

  status: Status;
  error: string | null;
  savingScope: string | null;

  // Loaders
  loadAll: (userId: string) => Promise<void>;
  loadProfile: (userId: string) => Promise<void>;
  loadAddresses: (userId: string) => Promise<void>;
  loadSessions: (userId: string) => Promise<void>;
  loadPreferences: (userId: string) => Promise<void>;
  loadVendorProfile: (userId: string) => Promise<void>;

  // Personal + avatar
  updatePersonal: (data: { name?: string; phone?: string; gender?: User["gender"]; dob?: string; bio?: string }) => Promise<void>;
  uploadAvatar: (dataUrl: string) => Promise<void>;
  removeAvatar: () => Promise<void>;

  // Address
  addAddress: (addr: Omit<Address, "id">) => Promise<void>;
  updateAddress: (id: string, addr: Omit<Address, "id">) => Promise<void>;
  deleteAddress: (id: string) => Promise<void>;
  setDefaultAddress: (id: string) => Promise<void>;

  // Security
  changePassword: (current: string, next: string) => Promise<void>;
  revokeSession: (id: string) => Promise<void>;
  revokeAllOtherSessions: () => Promise<void>;

  // Preferences
  updatePreferences: (patch: Partial<NotificationPreferences>) => Promise<void>;

  // Account lifecycle
  deactivate: () => Promise<void>;
  reactivate: () => Promise<void>;
  deleteAccount: (password: string) => Promise<void>;

  // Vendor
  updateVendorProfile: (patch: Partial<VendorBusinessProfile>) => Promise<void>;

  // Utility
  reset: () => void;
}

const empty: Pick<ProfileState, "userId" | "profile" | "addresses" | "sessions" | "preferences" | "vendorProfile" | "status" | "error" | "savingScope"> = {
  userId: null,
  profile: null,
  addresses: [],
  sessions: [],
  preferences: null,
  vendorProfile: null,
  status: "idle",
  error: null,
  savingScope: null,
};

function getCurrentUserId(): string {
  const u = useAuthStore.getState().currentUser;
  if (!u) throw new ApiError("Not authenticated", 401, "NO_USER");
  return u.id;
}

/** Mirror personal-detail changes back to the auth user so the header avatar etc. update. */
function syncAuthUser(patch: Partial<User>) {
  const auth = useAuthStore.getState();
  if (!auth.currentUser) return;
  useAuthStore.setState({ currentUser: { ...auth.currentUser, ...patch } });
}

function errMessage(e: unknown, fallback = "Something went wrong"): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error) return e.message;
  return fallback;
}

export const useProfileStore = create<ProfileState>((set, get) => ({
  ...empty,

  loadAll: async (userId) => {
    set({ userId, status: "loading", error: null });
    try {
      const [p, addrs, sess, prefs, vendor] = await Promise.all([
        profileApi.getProfile(userId),
        profileApi.listAddresses(userId),
        profileApi.listSessions(userId),
        profileApi.getPreferences(userId).catch(() => null),
        profileApi.getVendorProfile(userId).catch(() => null),
      ]);
      set({
        userId,
        profile: p.data,
        addresses: addrs.data,
        sessions: sess.data,
        preferences: prefs?.data ?? null,
        vendorProfile: vendor?.data ?? null,
        status: "idle",
      });
    } catch (e) {
      set({ status: "error", error: errMessage(e, "Failed to load profile") });
    }
  },

  loadProfile: async (userId) => {
    const res = await profileApi.getProfile(userId);
    set({ profile: res.data });
  },
  loadAddresses: async (userId) => {
    const res = await profileApi.listAddresses(userId);
    set({ addresses: res.data });
  },
  loadSessions: async (userId) => {
    const res = await profileApi.listSessions(userId);
    set({ sessions: res.data });
  },
  loadPreferences: async (userId) => {
    const res = await profileApi.getPreferences(userId);
    set({ preferences: res.data });
  },
  loadVendorProfile: async (userId) => {
    const res = await profileApi.getVendorProfile(userId);
    set({ vendorProfile: res.data });
  },

  /* ---------------- personal & avatar ---------------- */
  updatePersonal: async (data) => {
    const userId = getCurrentUserId();
    const prev = get().profile;
    if (!prev) throw new ApiError("Profile not loaded", 400);
    // optimistic
    const optimistic: User = { ...prev, ...data };
    set({ profile: optimistic, savingScope: "personal", error: null, status: "saving" });
    syncAuthUser(data);
    try {
      const res = await profileApi.updatePersonal(userId, data);
      set({ profile: res.data, status: "idle", savingScope: null });
      syncAuthUser(res.data);
    } catch (e) {
      set({ profile: prev, status: "error", savingScope: null, error: errMessage(e) });
      syncAuthUser(prev);
      throw e;
    }
  },

  uploadAvatar: async (dataUrl) => {
    const userId = getCurrentUserId();
    const prev = get().profile;
    if (!prev) throw new ApiError("Profile not loaded", 400);
    set({ profile: { ...prev, avatar: dataUrl }, savingScope: "avatar", status: "saving" });
    syncAuthUser({ avatar: dataUrl });
    try {
      const res = await profileApi.uploadAvatar(userId, { dataUrl });
      set((s) => ({ profile: s.profile ? { ...s.profile, avatar: res.data.avatar } : s.profile, status: "idle", savingScope: null }));
    } catch (e) {
      set({ profile: prev, savingScope: null, status: "error", error: errMessage(e) });
      syncAuthUser({ avatar: prev.avatar });
      throw e;
    }
  },

  removeAvatar: async () => {
    const userId = getCurrentUserId();
    const prev = get().profile;
    if (!prev) return;
    set({ profile: { ...prev, avatar: "" }, savingScope: "avatar", status: "saving" });
    syncAuthUser({ avatar: "" });
    try {
      await profileApi.removeAvatar(userId);
      set({ status: "idle", savingScope: null });
    } catch (e) {
      set({ profile: prev, savingScope: null, status: "error", error: errMessage(e) });
      syncAuthUser({ avatar: prev.avatar });
      throw e;
    }
  },

  /* ---------------- addresses ---------------- */
  addAddress: async (addr) => {
    const userId = getCurrentUserId();
    set({ savingScope: "address", status: "saving", error: null });
    try {
      const res = await profileApi.addAddress(userId, addr);
      set({ addresses: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  updateAddress: async (id, addr) => {
    const userId = getCurrentUserId();
    const prev = get().addresses;
    // optimistic
    set({
      addresses: prev.map((a) => (a.id === id ? { ...a, ...addr } : addr.isDefault ? { ...a, isDefault: false } : a)),
      savingScope: "address",
      status: "saving",
    });
    try {
      const res = await profileApi.updateAddress(userId, id, addr);
      set({ addresses: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ addresses: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  deleteAddress: async (id) => {
    const userId = getCurrentUserId();
    const prev = get().addresses;
    set({ addresses: prev.filter((a) => a.id !== id), savingScope: "address", status: "saving" });
    try {
      const res = await profileApi.deleteAddress(userId, id);
      set({ addresses: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ addresses: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  setDefaultAddress: async (id) => {
    const userId = getCurrentUserId();
    const prev = get().addresses;
    set({ addresses: prev.map((a) => ({ ...a, isDefault: a.id === id })), savingScope: "address", status: "saving" });
    try {
      const res = await profileApi.setDefaultAddress(userId, id);
      set({ addresses: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ addresses: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  /* ---------------- security ---------------- */
  changePassword: async (current, next) => {
    const userId = getCurrentUserId();
    set({ savingScope: "password", status: "saving", error: null });
    try {
      const res = await profileApi.changePassword(userId, current, next);
      set((s) => ({
        profile: s.profile ? { ...s.profile, passwordChangedAt: res.data.changedAt } : s.profile,
        status: "idle", savingScope: null,
      }));
      syncAuthUser({ passwordChangedAt: res.data.changedAt });
    } catch (e) {
      set({ status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  revokeSession: async (id) => {
    const userId = getCurrentUserId();
    const prev = get().sessions;
    set({ sessions: prev.filter((s) => s.id !== id), savingScope: "sessions", status: "saving" });
    try {
      const res = await profileApi.revokeSession(userId, id);
      set({ sessions: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ sessions: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  revokeAllOtherSessions: async () => {
    const userId = getCurrentUserId();
    const prev = get().sessions;
    set({ sessions: prev.filter((s) => s.current), savingScope: "sessions", status: "saving" });
    try {
      const res = await profileApi.revokeAllOtherSessions(userId);
      set({ sessions: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ sessions: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  /* ---------------- preferences ---------------- */
  updatePreferences: async (patch) => {
    const userId = getCurrentUserId();
    const prev = get().preferences;
    if (!prev) throw new ApiError("Preferences not loaded", 400);
    const optimistic = { ...prev, ...patch };
    set({ preferences: optimistic, savingScope: "preferences", status: "saving" });
    try {
      const res = await profileApi.updatePreferences(userId, patch);
      set({ preferences: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ preferences: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  /* ---------------- lifecycle ---------------- */
  deactivate: async () => {
    const userId = getCurrentUserId();
    set({ savingScope: "lifecycle", status: "saving" });
    try {
      await profileApi.deactivate(userId);
      set((s) => ({ profile: s.profile ? { ...s.profile, status: "deactivated" } : s.profile, status: "idle", savingScope: null }));
      syncAuthUser({ status: "deactivated" });
    } catch (e) {
      set({ status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  reactivate: async () => {
    const userId = getCurrentUserId();
    set({ savingScope: "lifecycle", status: "saving" });
    try {
      await profileApi.reactivate(userId);
      set((s) => ({ profile: s.profile ? { ...s.profile, status: "active" } : s.profile, status: "idle", savingScope: null }));
      syncAuthUser({ status: "active" });
    } catch (e) {
      set({ status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  deleteAccount: async (password) => {
    const userId = getCurrentUserId();
    set({ savingScope: "lifecycle", status: "saving" });
    try {
      await profileApi.deleteAccount(userId, password);
      set({ savingScope: null, status: "idle" });
      // Force a logout so the deleted account can't continue using the app.
      await useAuthStore.getState().logout();
    } catch (e) {
      set({ status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  /* ---------------- vendor ---------------- */
  updateVendorProfile: async (patch) => {
    const userId = getCurrentUserId();
    const prev = get().vendorProfile;
    if (!prev) throw new ApiError("Vendor profile not loaded", 400);
    const optimistic = { ...prev, ...patch };
    set({ vendorProfile: optimistic, savingScope: "vendor", status: "saving" });
    try {
      const res = await profileApi.updateVendorProfile(userId, patch);
      set({ vendorProfile: res.data, status: "idle", savingScope: null });
    } catch (e) {
      set({ vendorProfile: prev, status: "error", savingScope: null, error: errMessage(e) });
      throw e;
    }
  },

  reset: () => set({ ...empty }),
}));

/** Subscribe to auth changes — invalidate caches when user identity changes. */
if (typeof window !== "undefined") {
  let lastId: string | null = null;
  useAuthStore.subscribe((state) => {
    const id = state.currentUser?.id ?? null;
    if (id !== lastId) {
      lastId = id;
      useProfileStore.getState().reset();
    }
  });
}