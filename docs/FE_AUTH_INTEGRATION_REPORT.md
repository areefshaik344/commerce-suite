# FE Auth Integration Report — Phase FE-2

**Date:** 2026-06-07
**Scope:** Wire frontend authentication flows to the certified Spring Boot backend (`AuthController`, `ProfileController`).
**Transport:** `src/api/httpClient.ts` (axios + JWT injection + single-flight refresh).
**Toggle:** `VITE_USE_MOCK_API` (`1` = mock, `0`/unset in `.env.production` = real backend).

---

## 1. Architecture

`src/api/authApi.ts` now exports a feature-flagged adapter:

```
authApi = USE_REAL_API ? realAuthApi : mockAuthApi
```

`realAuthApi` calls the real backend over `httpClient` and adapts the
backend `ProfileDto` to the frontend `User` shape via `profileToUser()`,
so the rest of the app (store, hooks, routes) is untouched.

## 2. Flow-by-flow integration matrix

| Flow | Backend endpoint | Frontend caller | Status |
|---|---|---|---|
| Login | `POST /auth/login` + `GET /me` | `authStore.loginAsync` | ✅ Integrated |
| Signup | `POST /auth/signup` + `GET /me` | `authStore.signupWithCredentials` | ✅ Integrated |
| Logout | `POST /auth/logout` | `authStore.logout` | ✅ Integrated |
| Refresh | `POST /auth/refresh` | `authStore.refresh` + httpClient interceptor | ✅ Integrated |
| Session restore | `GET /me` | `authStore.bootstrap` | ✅ Integrated |
| Email verification | `POST /auth/email/verify` | `EmailVerificationPage` via `authApi.verifyOtp("verify-email")` | 🟡 Token-based (no resend endpoint on backend) |
| Forgot password | `POST /auth/password/forgot` | `ForgotPasswordPage` | ✅ Integrated |
| Reset password | `POST /auth/password/reset` | `ResetPasswordPage` | ✅ Integrated (token-based; OTP UI in mock mode only) |
| Change password | `POST /auth/password/change` | `ChangePasswordForm` | 🟡 Not yet wired (still hits mock path) |
| MFA / TOTP | n/a (not in backend scope) | — | ⛔ Backend does not expose MFA endpoints |
| Phone OTP login | n/a | — | ⛔ Backend does not expose SMS OTP |

## 3. Token / session management

- Access token: in-memory + `sessionStorage` (`tokenStorage.setAccess`).
- Refresh token: `localStorage` (persistent) or `sessionStorage` (session-only).
- `Authorization: Bearer <jwt>` injected by `httpClient` request interceptor.
- `X-Request-Id` propagated for backend tracing/audit.
- Single-flight refresh: `httpClient.performRefresh()` dedupes parallel 401s.
- Multi-tab sync: storage events broadcast login/logout/refresh.
- Silent refresh timer scheduled by `authStore.scheduleSilentRefresh` based on JWT `exp`.
- RBAC: backend roles (`ADMIN` / `VENDOR` / `CUSTOMER`) mapped to the existing
  `UserRole` union in `profileToUser()`. `RoleRoute` / `PermissionRoute` continue
  to gate navigation unchanged.

## 4. DTO mapping

| Backend (ProfileDto)       | Frontend (User)       | Notes |
|---------------------------|------------------------|-------|
| `id` (UUID)               | `id` (string)          | preserved |
| `email`                   | `email`                | preserved |
| `phone`                   | `phone`                | empty string when null |
| `roles[]`                 | `role`                 | first matching of ADMIN > VENDOR > CUSTOMER |
| `fullName`/`displayName`  | `name`                 | falls back to email |
| `avatarUrl`               | `avatar`               | empty string when null |
| `accountStatus`           | `accountStatus`        | upper-case enum preserved |
| `emailVerified` / `phoneVerified` | same             | preserved |

## 5. Verification matrix

Local (mock backend, `VITE_USE_MOCK_API=1`):

- ✅ Customer / Vendor / Admin login (demo credentials)
- ✅ Refresh flow (15-minute access TTL)
- ✅ Logout broadcasts across tabs
- ✅ Session restoration after reload
- ✅ Protected route gating (`ProtectedRoute`, `RoleRoute`, `PermissionRoute`)
- ✅ RBAC navigation (admin/vendor menus)

Real backend (`VITE_USE_MOCK_API=0`):

- 🟡 Cannot be executed in this environment (no live Spring Boot instance reachable from sandbox).
- Code paths compiled and type-checked; runtime smoke must be re-run on staging.

## 6. Remaining gaps

1. **Phone OTP login** — no backend endpoint; not in Phase FE-2 scope.
2. **MFA / TOTP** — backend does not expose; deferred.
3. **Change password from settings panel** — UI exists, real endpoint exists
   (`POST /auth/password/change`), but the call site still uses the legacy mock path.
   Tracked as a follow-up sweep.
4. **`/auth/email/verify` does not support resend** — `EmailVerificationPage`
   "Resend OTP" is a no-op in real mode (returns success). Backend would need
   a `/auth/email/resend` endpoint to fully match the UX.
5. **CORS** — backend `app.cors.allowed-origins` must include the deployed
   frontend origin before real-mode auth can succeed in production.

## 7. Files changed

- `src/api/authApi.ts` — dual-mode adapter (mock + real backend).

## 8. Verdict

**AUTH PARTIALLY INTEGRATED.**

All core flows (login / signup / refresh / logout / session restore / forgot /
reset / email-verify) are wired to the real backend behind a single feature
flag. MFA and SMS OTP are out of scope (backend has no endpoints). Change-password
wiring and staging smoke tests are the only remaining items before this can be
upgraded to **AUTH FULLY INTEGRATED**.