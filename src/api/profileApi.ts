/**
 * Profile API — production-shaped mock.
 * Every endpoint returns `{ status, message, data }` and throws ApiError on failure.
 * Swap implementation when wiring real backend; signatures stay stable.
 */
import { simulateDelay, ApiError, type ApiResponse } from "./apiClient";
import { mockUsers, mockSessions, mockCredentials } from "@/mocks";
import type {
  User, Address, NotificationPreferences, VendorBusinessProfile, DeviceSession,
} from "@/data/mock-users";

function ok<T>(data: T, message = "OK"): ApiResponse<T> {
  return { data, status: 200, message };
}

function findUserOrThrow(userId: string): User {
  const idx = mockUsers.findIndex((u) => u.id === userId);
  if (idx === -1) throw new ApiError("User not found", 404, "USER_NOT_FOUND");
  return mockUsers[idx];
}
function patchUser(userId: string, patch: Partial<User>): User {
  const idx = mockUsers.findIndex((u) => u.id === userId);
  if (idx === -1) throw new ApiError("User not found", 404, "USER_NOT_FOUND");
  mockUsers[idx] = { ...mockUsers[idx], ...patch };
  return mockUsers[idx];
}

/* ---------- Personal details ---------- */

export interface UpdatePersonalRequest {
  name?: string; phone?: string; gender?: User["gender"]; dob?: string; bio?: string;
}
export interface UpdateAvatarRequest { dataUrl: string; }

/* ---------- Addresses ---------- */

function isDuplicate(a: Address, b: Address): boolean {
  return a.line1.trim().toLowerCase() === b.line1.trim().toLowerCase()
    && a.pincode === b.pincode
    && a.city.trim().toLowerCase() === b.city.trim().toLowerCase();
}

/* ---------- Password ---------- */

function findCred(userId: string) {
  const cred = mockCredentials.find((c) => c.userId === userId);
  if (!cred) throw new ApiError("Credentials not found", 404, "CRED_NOT_FOUND");
  return cred;
}

/* ---------- API ---------- */

export const profileApi = {
  async getProfile(userId: string): Promise<ApiResponse<User>> {
    await simulateDelay(220);
    return ok(findUserOrThrow(userId));
  },

  async updatePersonal(userId: string, req: UpdatePersonalRequest): Promise<ApiResponse<User>> {
    await simulateDelay(420);
    const sanitized: Partial<User> = {
      ...(req.name !== undefined && { name: req.name.trim() }),
      ...(req.phone !== undefined && { phone: req.phone }),
      ...(req.gender !== undefined && { gender: req.gender }),
      ...(req.dob !== undefined && { dob: req.dob }),
      ...(req.bio !== undefined && { bio: req.bio }),
    };
    return ok(patchUser(userId, sanitized), "Profile updated");
  },

  async uploadAvatar(userId: string, req: UpdateAvatarRequest): Promise<ApiResponse<{ avatar: string }>> {
    await simulateDelay(700);
    if (!req.dataUrl?.startsWith("data:image/")) throw new ApiError("Invalid image data", 422, "BAD_IMAGE");
    patchUser(userId, { avatar: req.dataUrl });
    return ok({ avatar: req.dataUrl }, "Avatar updated");
  },

  async removeAvatar(userId: string): Promise<ApiResponse<{ avatar: string }>> {
    await simulateDelay(280);
    patchUser(userId, { avatar: "" });
    return ok({ avatar: "" }, "Avatar removed");
  },

  /* Addresses */
  async listAddresses(userId: string): Promise<ApiResponse<Address[]>> {
    await simulateDelay(180);
    return ok(findUserOrThrow(userId).addresses ?? []);
  },

  async addAddress(userId: string, addr: Omit<Address, "id">): Promise<ApiResponse<Address[]>> {
    await simulateDelay(420);
    const user = findUserOrThrow(userId);
    const list = user.addresses ?? [];
    const newAddr: Address = { ...addr, id: `a-${Date.now()}` };
    if (list.some((a) => isDuplicate(a, newAddr))) {
      throw new ApiError("This address already exists", 409, "DUPLICATE_ADDRESS");
    }
    let next = [...list, newAddr];
    if (newAddr.isDefault || next.length === 1) {
      next = next.map((a) => ({ ...a, isDefault: a.id === newAddr.id }));
    }
    patchUser(userId, { addresses: next });
    return ok(next, "Address added");
  },

  async updateAddress(userId: string, addressId: string, addr: Omit<Address, "id">): Promise<ApiResponse<Address[]>> {
    await simulateDelay(420);
    const user = findUserOrThrow(userId);
    const list = user.addresses ?? [];
    if (!list.some((a) => a.id === addressId)) throw new ApiError("Address not found", 404);
    const updated: Address = { ...addr, id: addressId };
    if (list.some((a) => a.id !== addressId && isDuplicate(a, updated))) {
      throw new ApiError("Another address with the same details already exists", 409, "DUPLICATE_ADDRESS");
    }
    let next = list.map((a) => (a.id === addressId ? updated : a));
    if (updated.isDefault) next = next.map((a) => ({ ...a, isDefault: a.id === addressId }));
    if (!next.some((a) => a.isDefault) && next.length > 0) next[0].isDefault = true;
    patchUser(userId, { addresses: next });
    return ok(next, "Address updated");
  },

  async deleteAddress(userId: string, addressId: string): Promise<ApiResponse<Address[]>> {
    await simulateDelay(360);
    const user = findUserOrThrow(userId);
    const list = user.addresses ?? [];
    const target = list.find((a) => a.id === addressId);
    if (!target) throw new ApiError("Address not found", 404);
    if (target.isDefault && list.length === 1) {
      throw new ApiError("Cannot delete the only address. Add another first.", 422, "LAST_DEFAULT");
    }
    let next = list.filter((a) => a.id !== addressId);
    if (target.isDefault && next.length > 0) next[0] = { ...next[0], isDefault: true };
    patchUser(userId, { addresses: next });
    return ok(next, "Address removed");
  },

  async setDefaultAddress(userId: string, addressId: string): Promise<ApiResponse<Address[]>> {
    await simulateDelay(220);
    const user = findUserOrThrow(userId);
    const list = user.addresses ?? [];
    if (!list.some((a) => a.id === addressId)) throw new ApiError("Address not found", 404);
    const next = list.map((a) => ({ ...a, isDefault: a.id === addressId }));
    patchUser(userId, { addresses: next });
    return ok(next, "Default address updated");
  },

  /* Password */
  async changePassword(userId: string, currentPassword: string, newPassword: string): Promise<ApiResponse<{ changedAt: string }>> {
    await simulateDelay(550);
    const cred = findCred(userId);
    if (cred.password !== currentPassword) throw new ApiError("Current password is incorrect", 401, "BAD_CURRENT");
    if (cred.password === newPassword) throw new ApiError("New password must be different", 422, "SAME_PASSWORD");
    cred.password = newPassword;
    const changedAt = new Date().toISOString();
    patchUser(userId, { passwordChangedAt: changedAt });
    return ok({ changedAt }, "Password changed successfully");
  },

  /* Sessions / devices */
  async listSessions(userId: string): Promise<ApiResponse<DeviceSession[]>> {
    await simulateDelay(220);
    return ok(mockSessions[userId] ?? []);
  },

  async revokeSession(userId: string, sessionId: string): Promise<ApiResponse<DeviceSession[]>> {
    await simulateDelay(320);
    const list = mockSessions[userId] ?? [];
    const target = list.find((s) => s.id === sessionId);
    if (!target) throw new ApiError("Session not found", 404);
    if (target.current) throw new ApiError("Cannot revoke the current session — use logout instead", 422, "CURRENT_SESSION");
    mockSessions[userId] = list.filter((s) => s.id !== sessionId);
    return ok(mockSessions[userId], "Device signed out");
  },

  async revokeAllOtherSessions(userId: string): Promise<ApiResponse<DeviceSession[]>> {
    await simulateDelay(450);
    const list = mockSessions[userId] ?? [];
    mockSessions[userId] = list.filter((s) => s.current);
    return ok(mockSessions[userId], "All other devices signed out");
  },

  /* Notification preferences */
  async getPreferences(userId: string): Promise<ApiResponse<NotificationPreferences>> {
    await simulateDelay(180);
    const u = findUserOrThrow(userId);
    if (!u.preferences) throw new ApiError("Preferences not initialised", 404);
    return ok(u.preferences);
  },

  async updatePreferences(userId: string, prefs: Partial<NotificationPreferences>): Promise<ApiResponse<NotificationPreferences>> {
    await simulateDelay(320);
    const u = findUserOrThrow(userId);
    const next: NotificationPreferences = { ...(u.preferences as NotificationPreferences), ...prefs };
    patchUser(userId, { preferences: next });
    return ok(next, "Preferences saved");
  },

  /* Account lifecycle */
  async deactivate(userId: string): Promise<ApiResponse<{ status: "deactivated" }>> {
    await simulateDelay(420);
    patchUser(userId, { status: "deactivated" });
    return ok({ status: "deactivated" as const }, "Account deactivated");
  },

  async reactivate(userId: string): Promise<ApiResponse<{ status: "active" }>> {
    await simulateDelay(360);
    patchUser(userId, { status: "active" });
    return ok({ status: "active" as const }, "Account reactivated");
  },

  async deleteAccount(userId: string, password: string): Promise<ApiResponse<{ deleted: true }>> {
    await simulateDelay(700);
    const cred = findCred(userId);
    if (cred.password !== password) throw new ApiError("Password is incorrect", 401, "BAD_PASSWORD");
    patchUser(userId, { status: "deleted" });
    return ok({ deleted: true as const }, "Account deletion scheduled");
  },

  /* Vendor business profile */
  async getVendorProfile(userId: string): Promise<ApiResponse<VendorBusinessProfile | null>> {
    await simulateDelay(220);
    return ok(findUserOrThrow(userId).vendorProfile ?? null);
  },

  async updateVendorProfile(userId: string, patch: Partial<VendorBusinessProfile>): Promise<ApiResponse<VendorBusinessProfile>> {
    await simulateDelay(520);
    const u = findUserOrThrow(userId);
    if (!u.vendorProfile) throw new ApiError("Vendor profile not initialised", 404, "NO_VENDOR_PROFILE");
    if (patch.storeSlug && patch.storeSlug !== u.vendorProfile.storeSlug) {
      const exists = mockUsers.some((other) => other.id !== userId && other.vendorProfile?.storeSlug === patch.storeSlug);
      if (exists) throw new ApiError("Store slug already taken", 409, "SLUG_TAKEN");
    }
    const next: VendorBusinessProfile = { ...u.vendorProfile, ...patch };
    patchUser(userId, { vendorProfile: next });
    return ok(next, "Vendor profile updated");
  },
};