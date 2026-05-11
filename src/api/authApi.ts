/**
 * Mock auth API — production-shaped: every endpoint returns
 * `{ status, message, data }` and throws ApiError with HTTP-style codes on failure.
 * Swap this single file when wiring a real Spring Boot backend.
 */
import { simulateDelay, ApiError, type ApiResponse } from "./apiClient";
import { mockUsers, mockCredentials } from "@/mocks";
import type { User } from "@/data/mock-users";
import { signMockToken, decodeToken, isExpired, TOKEN_TTL } from "@/lib/tokenStorage";

function ok<T>(data: T, message = "OK"): ApiResponse<T> {
  return { data, status: 200, message };
}

function issueTokens(user: User) {
  const now = Math.floor(Date.now() / 1000);
  const accessToken = signMockToken({
    sub: user.id,
    role: user.role,
    type: "access",
    exp: now + TOKEN_TTL.ACCESS_SECONDS,
  });
  const refreshToken = signMockToken({
    sub: user.id,
    role: user.role,
    type: "refresh",
    exp: now + TOKEN_TTL.REFRESH_SECONDS,
  });
  return { accessToken, refreshToken };
}

// In-memory OTP "inbox" — tracks issued OTPs with a 60s TTL.
interface OtpRecord { code: string; issuedAt: number; purpose: "verify-email" | "reset-password" | "phone-login"; target: string; }
const otpStore = new Map<string, OtpRecord>();
const OTP_TTL_MS = 60_000;

// Brute-force protection: 5 attempts per (purpose, target), then a 5-minute lockout.
const OTP_MAX_ATTEMPTS = 5;
const OTP_LOCKOUT_MS = 5 * 60_000;
interface OtpAttempt { count: number; lockedUntil: number; }
const otpAttempts = new Map<string, OtpAttempt>();

function attemptKey(purpose: string, target: string) {
  return `${purpose}:${target.toLowerCase()}`;
}
function getAttempts(key: string): OtpAttempt {
  return otpAttempts.get(key) ?? { count: 0, lockedUntil: 0 };
}
function resetAttempts(key: string) { otpAttempts.delete(key); }

function generateOtp() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

export interface LoginRequest { email: string; password: string; }
export interface SignupRequest { name: string; email: string; phone: string; password: string; }
export interface AuthTokens { accessToken: string; refreshToken: string; }
export interface AuthResponse extends AuthTokens { user: User; }

export const authApi = {
  async login(req: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    await simulateDelay(450);
    const cred = mockCredentials.find((c) => c.email.toLowerCase() === req.email.toLowerCase());
    if (!cred || cred.password !== req.password) {
      throw new ApiError("Invalid email or password", 401);
    }
    const user = mockUsers.find((u) => u.id === cred.userId);
    if (!user) throw new ApiError("Account not found", 404);
    return ok({ user, ...issueTokens(user) }, "Login successful");
  },

  async signup(req: SignupRequest): Promise<ApiResponse<AuthResponse>> {
    await simulateDelay(600);
    if (mockCredentials.find((c) => c.email.toLowerCase() === req.email.toLowerCase())) {
      throw new ApiError("Email is already registered", 409);
    }
    const newUser: User = {
      id: `u-${Date.now()}`,
      name: req.name.trim(),
      email: req.email.toLowerCase(),
      avatar: "",
      role: "customer",
      phone: `+91 ${req.phone}`,
      joinedDate: new Date().toISOString().split("T")[0],
      isVendor: false,
      vendorStatus: "none",
    };
    mockCredentials.push({ email: newUser.email, password: req.password, userId: newUser.id });
    mockUsers.push(newUser);
    return ok({ user: newUser, ...issueTokens(newUser) }, "Account created");
  },

  async logout(refreshToken: string | null): Promise<ApiResponse<{ revoked: boolean }>> {
    await simulateDelay(120);
    // In a real backend we'd revoke the refresh token server-side.
    return ok({ revoked: !!refreshToken }, "Logged out");
  },

  async refresh(refreshToken: string | null): Promise<ApiResponse<AuthTokens>> {
    await simulateDelay(250);
    if (!refreshToken) throw new ApiError("Missing refresh token", 401);
    const decoded = decodeToken(refreshToken);
    if (!decoded || decoded.type !== "refresh") throw new ApiError("Invalid refresh token", 401);
    if (isExpired(refreshToken)) throw new ApiError("Refresh token expired", 401);
    const user = mockUsers.find((u) => u.id === decoded.sub);
    if (!user) throw new ApiError("Account not found", 404);
    return ok(issueTokens(user), "Token refreshed");
  },

  async me(accessToken: string | null): Promise<ApiResponse<User>> {
    await simulateDelay(120);
    if (!accessToken) throw new ApiError("Not authenticated", 401);
    const decoded = decodeToken(accessToken);
    if (!decoded || decoded.type !== "access") throw new ApiError("Invalid access token", 401);
    if (isExpired(accessToken)) throw new ApiError("Access token expired", 401);
    const user = mockUsers.find((u) => u.id === decoded.sub);
    if (!user) throw new ApiError("Account not found", 404);
    return ok(user, "OK");
  },

  async sendOtp(target: string, purpose: OtpRecord["purpose"]): Promise<ApiResponse<{ sent: boolean; devCode?: string }>> {
    await simulateDelay(400);
    const key = attemptKey(purpose, target);
    const a = getAttempts(key);
    if (a.lockedUntil > Date.now()) {
      const seconds = Math.ceil((a.lockedUntil - Date.now()) / 1000);
      throw new ApiError(`Too many attempts. Try again in ${seconds}s.`, 429, "OTP_LOCKED");
    }
    // New OTP wipes any previous failed-attempt counter for this target.
    resetAttempts(key);
    const code = generateOtp();
    otpStore.set(`${purpose}:${target.toLowerCase()}`, { code, issuedAt: Date.now(), purpose, target });
    // Surface the code in dev so reviewers can complete the flow without an email/SMS provider.
    console.info(`[auth-mock] OTP for ${purpose} → ${target}: ${code}`);
    return ok({ sent: true, devCode: code }, "OTP sent");
  },

  async verifyOtp(target: string, purpose: OtpRecord["purpose"], code: string): Promise<ApiResponse<{ verified: true }>> {
    await simulateDelay(300);
    const key = `${purpose}:${target.toLowerCase()}`;
    const aKey = attemptKey(purpose, target);
    const a = getAttempts(aKey);
    if (a.lockedUntil > Date.now()) {
      const seconds = Math.ceil((a.lockedUntil - Date.now()) / 1000);
      throw new ApiError(`Too many failed attempts. Try again in ${seconds}s.`, 429, "OTP_LOCKED");
    }
    const record = otpStore.get(key);
    if (!record) throw new ApiError("No OTP requested for this address", 400);
    if (Date.now() - record.issuedAt > OTP_TTL_MS) {
      otpStore.delete(key);
      resetAttempts(aKey);
      throw new ApiError("OTP has expired. Please request a new one.", 410);
    }
    if (record.code !== code) {
      const next = a.count + 1;
      if (next >= OTP_MAX_ATTEMPTS) {
        otpAttempts.set(aKey, { count: next, lockedUntil: Date.now() + OTP_LOCKOUT_MS });
        otpStore.delete(key);
        throw new ApiError(`Too many failed attempts. Locked for ${OTP_LOCKOUT_MS / 60000} minutes.`, 429, "OTP_LOCKED");
      }
      otpAttempts.set(aKey, { count: next, lockedUntil: 0 });
      const remaining = OTP_MAX_ATTEMPTS - next;
      throw new ApiError(`Incorrect OTP. ${remaining} attempt${remaining === 1 ? "" : "s"} remaining.`, 422, "OTP_INVALID");
    }
    otpStore.delete(key);
    resetAttempts(aKey);
    return ok({ verified: true as const }, "Verified");
  },

  async forgotPassword(email: string): Promise<ApiResponse<{ message: string; devCode?: string }>> {
    await simulateDelay(450);
    // Always return success to prevent enumeration, but only generate OTP if account exists.
    const exists = mockCredentials.find((c) => c.email.toLowerCase() === email.toLowerCase());
    let devCode: string | undefined;
    if (exists) {
      const r = await authApi.sendOtp(email, "reset-password");
      devCode = r.data.devCode;
    }
    return ok({ message: "If this email exists, a reset code has been sent.", devCode });
  },

  async resetPassword(email: string, otp: string, newPassword: string): Promise<ApiResponse<{ reset: true }>> {
    await authApi.verifyOtp(email, "reset-password", otp);
    await simulateDelay(300);
    const cred = mockCredentials.find((c) => c.email.toLowerCase() === email.toLowerCase());
    if (!cred) throw new ApiError("Account not found", 404);
    cred.password = newPassword;
    return ok({ reset: true as const }, "Password reset successful");
  },

  async updateProfile(userId: string, data: Partial<User>): Promise<ApiResponse<User>> {
    await simulateDelay(400);
    const idx = mockUsers.findIndex((u) => u.id === userId);
    if (idx === -1) throw new ApiError("User not found", 404);
    mockUsers[idx] = { ...mockUsers[idx], ...data };
    return ok(mockUsers[idx], "Profile updated");
  },
};

export type { OtpRecord };
