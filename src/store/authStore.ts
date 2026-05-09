import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserRole, User, VendorStatus } from "@/data/mock-users";
import { mockUsers, mockCredentials } from "@/mocks";
import { authApi } from "@/api/authApi";
import { ApiError } from "@/api/apiClient";
import {
  decodeToken,
  isExpired,
  tokenStorage,
  TOKEN_TTL,
} from "@/lib/tokenStorage";
import { authEvents } from "@/lib/authEvents";

export interface VendorApplication {
  id: string;
  userId: string;
  name: string;
  email: string;
  phone: string;
  storeName: string;
  category: string;
  description: string;
  status: "pending" | "approved" | "rejected";
  appliedDate: string;
  reviewNote?: string;
}

interface AuthState {
  currentUser: User | null;
  currentRole: UserRole;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
  vendorApplications: VendorApplication[];

  login: (role: UserRole) => void;
  loginWithCredentials: (email: string, password: string) => Promise<boolean>;
  loginAsync: (email: string, password: string) => Promise<{ user: User }>;
  signupWithCredentials: (name: string, email: string, phone: string, password: string) => Promise<User>;
  registerVendor: (name: string, email: string, phone: string, password: string, storeName: string, category: string, description: string) => void;
  applyAsVendor: (storeName: string, category: string, description: string) => { success: boolean; message: string };
  logout: () => Promise<void>;
  bootstrap: () => Promise<void>;
  refresh: () => Promise<boolean>;
  approveVendor: (appId: string) => void;
  rejectVendor: (appId: string, note?: string) => void;
}

let refreshTimer: ReturnType<typeof setTimeout> | null = null;
let refreshInFlight: Promise<boolean> | null = null;

function clearRefreshTimer() {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
}

function scheduleSilentRefresh(accessToken: string, runRefresh: () => Promise<boolean>) {
  clearRefreshTimer();
  const decoded = decodeToken(accessToken);
  if (!decoded) return;
  const msUntil = (decoded.exp - TOKEN_TTL.REFRESH_SKEW_SECONDS) * 1000 - Date.now();
  if (msUntil <= 0) {
    void runRefresh();
    return;
  }
  refreshTimer = setTimeout(() => { void runRefresh(); }, msUntil);
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      currentUser: null,
      currentRole: "customer",
      isAuthenticated: false,
      isBootstrapping: true,
      vendorApplications: [
        { id: "va-1", userId: "u-4", name: "Anita Singh", email: "anita@example.com", phone: "+91 76543 21098", storeName: "GadgetPro", category: "electronics", description: "Latest gadgets and accessories", status: "pending", appliedDate: "2025-02-20" },
      ],

      login: (role) => {
        // Demo role-switcher path — bypasses API but still issues mock tokens.
        const user = mockUsers.find((u) => u.role === role) || mockUsers[0];
        const cred = mockCredentials.find((c) => c.userId === user.id);
        if (cred) {
          void useAuthStore.getState().loginAsync(cred.email, cred.password).catch(() => {});
        } else {
          set({ currentUser: user, currentRole: role, isAuthenticated: true });
        }
      },

      loginAsync: async (email, password) => {
        const res = await authApi.login({ email, password });
        tokenStorage.setAccess(res.data.accessToken);
        tokenStorage.setRefresh(res.data.refreshToken);
        set({
          currentUser: res.data.user,
          currentRole: res.data.user.role,
          isAuthenticated: true,
          isBootstrapping: false,
        });
        scheduleSilentRefresh(res.data.accessToken, () => useAuthStore.getState().refresh());
        return { user: res.data.user };
      },

      loginWithCredentials: async (email, password) => {
        try {
          await useAuthStore.getState().loginAsync(email, password);
          return true;
        } catch {
          return false;
        }
      },

      signupWithCredentials: async (name, email, phone, password) => {
        const res = await authApi.signup({ name, email, phone, password });
        tokenStorage.setAccess(res.data.accessToken);
        tokenStorage.setRefresh(res.data.refreshToken);
        set({
          currentUser: res.data.user,
          currentRole: "customer",
          isAuthenticated: true,
          isBootstrapping: false,
        });
        scheduleSilentRefresh(res.data.accessToken, () => useAuthStore.getState().refresh());
        return res.data.user;
      },

      // Legacy shim — kept so existing callers compile. Vendor signup is now an authenticated upgrade.
      registerVendor: (_name, _email, _phone, _password, storeName, category, description) => {
        const state = useAuthStore.getState();
        if (state.currentUser) state.applyAsVendor(storeName, category, description);
      },

      applyAsVendor: (storeName, category, description) => {
        const state = useAuthStore.getState();
        const user = state.currentUser;
        if (!user) return { success: false, message: "You must be logged in" };
        if (state.vendorApplications.some(a => a.userId === user.id && a.status === "pending")) {
          return { success: false, message: "You already have a pending application" };
        }
        if (user.isVendor || user.vendorStatus === "active" || user.vendorStatus === "approved") {
          return { success: false, message: "You are already a seller" };
        }
        const app: VendorApplication = {
          id: `va-${Date.now()}`,
          userId: user.id,
          name: user.name, email: user.email, phone: user.phone,
          storeName, category, description,
          status: "pending",
          appliedDate: new Date().toISOString().split("T")[0],
        };
        // Update mock user record
        const idx = mockUsers.findIndex(u => u.id === user.id);
        if (idx !== -1) mockUsers[idx] = { ...mockUsers[idx], vendorStatus: "pending" };
        const updatedUser: User = { ...user, vendorStatus: "pending" };
        set(s => ({
          currentUser: updatedUser,
          vendorApplications: [...s.vendorApplications, app],
        }));
        return { success: true, message: "Application submitted" };
      },

      logout: async () => {
        clearRefreshTimer();
        const rt = tokenStorage.getRefresh();
        try { await authApi.logout(rt); } catch { /* swallow — clearing locally is sufficient */ }
        tokenStorage.clear();
        set({ currentUser: null, isAuthenticated: false, currentRole: "customer", isBootstrapping: false });
      },

      refresh: async () => {
        if (refreshInFlight) return refreshInFlight;
        refreshInFlight = (async () => {
          try {
            const rt = tokenStorage.getRefresh();
            const res = await authApi.refresh(rt);
            tokenStorage.setAccess(res.data.accessToken);
            tokenStorage.setRefresh(res.data.refreshToken);
            scheduleSilentRefresh(res.data.accessToken, () => useAuthStore.getState().refresh());
            return true;
          } catch (err) {
            // Refresh failed — clear session and notify UI.
            clearRefreshTimer();
            tokenStorage.clear();
            const wasAuthed = useAuthStore.getState().isAuthenticated;
            set({ currentUser: null, isAuthenticated: false, currentRole: "customer" });
            if (wasAuthed) authEvents.emitSessionExpired();
            return false;
          } finally {
            refreshInFlight = null;
          }
        })();
        return refreshInFlight;
      },

      bootstrap: async () => {
        const access = tokenStorage.getAccess();
        const refreshToken = tokenStorage.getRefresh();
        try {
          if (access && !isExpired(access)) {
            const me = await authApi.me(access);
            set({ currentUser: me.data, currentRole: me.data.role, isAuthenticated: true });
            scheduleSilentRefresh(access, () => useAuthStore.getState().refresh());
          } else if (refreshToken && !isExpired(refreshToken)) {
            const ok = await useAuthStore.getState().refresh();
            if (ok) {
              const me = await authApi.me(tokenStorage.getAccess());
              set({ currentUser: me.data, currentRole: me.data.role, isAuthenticated: true });
            }
          } else {
            tokenStorage.clear();
            set({ currentUser: null, isAuthenticated: false, currentRole: "customer" });
          }
        } catch (err) {
          if (err instanceof ApiError && err.status === 401) {
            tokenStorage.clear();
            set({ currentUser: null, isAuthenticated: false, currentRole: "customer" });
          }
        } finally {
          set({ isBootstrapping: false });
        }
      },

      approveVendor: (appId) => set(state => {
        const app = state.vendorApplications.find(a => a.id === appId);
        if (app) {
          const idx = mockUsers.findIndex(u => u.id === app.userId);
          if (idx !== -1) mockUsers[idx] = { ...mockUsers[idx], role: "vendor", isVendor: true, vendorStatus: "active" };
        }
        const updatedCurrent = state.currentUser && app && state.currentUser.id === app.userId
          ? { ...state.currentUser, role: "vendor" as UserRole, isVendor: true, vendorStatus: "active" as VendorStatus }
          : state.currentUser;
        return {
          currentUser: updatedCurrent,
          vendorApplications: state.vendorApplications.map(a => a.id === appId ? { ...a, status: "approved" as const } : a),
        };
      }),

      rejectVendor: (appId, note) => set(state => {
        const app = state.vendorApplications.find(a => a.id === appId);
        if (app) {
          const idx = mockUsers.findIndex(u => u.id === app.userId);
          if (idx !== -1) mockUsers[idx] = { ...mockUsers[idx], vendorStatus: "rejected" };
        }
        const updatedCurrent = state.currentUser && app && state.currentUser.id === app.userId
          ? { ...state.currentUser, vendorStatus: "rejected" as VendorStatus }
          : state.currentUser;
        return {
          currentUser: updatedCurrent,
          vendorApplications: state.vendorApplications.map(a => a.id === appId ? { ...a, status: "rejected" as const, reviewNote: note } : a),
        };
      }),
    }),
    {
      name: "markethub-auth",
      partialize: (state) => ({
        // Persist only non-sensitive metadata; tokens live in tokenStorage.
        currentUser: state.currentUser,
        currentRole: state.currentRole,
        isAuthenticated: state.isAuthenticated,
        vendorApplications: state.vendorApplications,
      }),
      onRehydrateStorage: () => (state) => {
        if (state) state.isBootstrapping = true;
      },
    }
  )
);
