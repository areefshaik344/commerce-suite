import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserRole, User, VendorStatus } from "@/data/mock-users";
import { mockUsers, mockCredentials } from "@/mocks";

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
  vendorApplications: VendorApplication[];

  login: (role: UserRole) => void;
  loginWithCredentials: (email: string, password: string) => boolean;
  signupWithCredentials: (name: string, email: string, phone: string, password: string) => void;
  registerVendor: (name: string, email: string, phone: string, password: string, storeName: string, category: string, description: string) => void;
  applyAsVendor: (storeName: string, category: string, description: string) => { success: boolean; message: string };
  logout: () => void;
  approveVendor: (appId: string) => void;
  rejectVendor: (appId: string, note?: string) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      currentUser: null,
      currentRole: "customer",
      isAuthenticated: false,
      vendorApplications: [
        { id: "va-1", userId: "u-4", name: "Anita Singh", email: "anita@example.com", phone: "+91 76543 21098", storeName: "GadgetPro", category: "electronics", description: "Latest gadgets and accessories", status: "pending", appliedDate: "2025-02-20" },
      ],

      login: (role) => {
        const user = mockUsers.find(u => u.role === role) || mockUsers[0];
        set({ currentUser: user, currentRole: role, isAuthenticated: true });
      },

      loginWithCredentials: (email, password) => {
        if (password === "phone-otp") {
          const phoneUser = mockUsers.find(u => u.role === "customer") || mockUsers[0];
          set({ currentUser: phoneUser, currentRole: "customer", isAuthenticated: true });
          return true;
        }
        const cred = mockCredentials.find(c => c.email === email && c.password === password);
        if (!cred) return false;
        const user = mockUsers.find(u => u.id === cred.userId);
        if (!user) return false;
        set({ currentUser: user, currentRole: user.role, isAuthenticated: true });
        return true;
      },

      signupWithCredentials: (name, email, phone, _password) => {
        if (mockCredentials.find(c => c.email === email)) {
          throw new Error("Email already registered");
        }
        const newUser: User = {
          id: `u-${Date.now()}`,
          name, email, avatar: "", role: "customer",
          phone: `+91 ${phone}`,
          joinedDate: new Date().toISOString().split("T")[0],
          isVendor: false,
          vendorStatus: "none",
        };
        mockCredentials.push({ email, password: _password, userId: newUser.id });
        mockUsers.push(newUser);
        set({ currentUser: newUser, currentRole: "customer", isAuthenticated: true });
      },

      // Legacy: kept for backward compatibility (no longer creates users)
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

      logout: () => set({ currentUser: null, isAuthenticated: false, currentRole: "customer" }),

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
        currentUser: state.currentUser,
        currentRole: state.currentRole,
        isAuthenticated: state.isAuthenticated,
        vendorApplications: state.vendorApplications,
      }),
    }
  )
);
