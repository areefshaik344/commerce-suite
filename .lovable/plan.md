## Production-grade Auth Module (Mock Backend)

Build a complete, production-shaped auth system on top of the existing marketplace. All flows simulate real network behavior with `{ status, message, data }` envelopes, token lifecycle, role guards, and full UX states. Backend stays mocked — when Spring Boot is ready, only `authApi.ts` swaps.

### Architecture

```text
src/
├── api/
│   ├── apiClient.ts          (existing — extend with auth header + 401 handler)
│   └── authApi.ts            (REWRITE — login/register/otp/refresh/reset)
├── store/
│   └── authStore.ts          (REWRITE — tokens, session, hydrate, auto-refresh)
├── hooks/
│   ├── useAuth.ts            (REWRITE — typed actions + selectors)
│   └── useOtpTimer.ts        (NEW — countdown + resend gate)
├── lib/
│   ├── tokenStorage.ts       (NEW — namespaced storage, expiry-aware)
│   ├── passwordStrength.ts   (NEW — zxcvbn-lite scoring)
│   └── validation.ts         (NEW — Yup schemas)
├── routes/
│   ├── ProtectedRoute.tsx    (REWRITE)
│   ├── PublicRoute.tsx       (NEW)
│   └── RoleRoute.tsx         (NEW)
├── components/auth/
│   ├── OtpInput.tsx          (REWRITE — paste/backspace/auto-focus)
│   ├── PasswordInput.tsx     (NEW — show/hide)
│   ├── PasswordStrengthMeter.tsx (NEW)
│   ├── AuthLoader.tsx        (NEW — full-screen restore spinner)
│   └── SessionExpiredDialog.tsx (NEW)
└── pages/auth/
    ├── LoginPage.tsx         (REWRITE — Formik + Yup)
    ├── SignupPage.tsx        (REWRITE — Formik + Yup + terms)
    ├── EmailVerificationPage.tsx (REWRITE — uses useOtpTimer)
    ├── ForgotPasswordPage.tsx (REWRITE — OTP flow)
    └── ResetPasswordPage.tsx (REWRITE — strength meter)
```

### Features delivered

- **Auth core**: login, register, logout, session restore on refresh via `AuthLoader` boot gate.
- **OTP**: 6-box input, auto-focus, backspace nav, paste support, 60s expiry timer, resend disabled until expiry, expired/invalid OTP errors.
- **Password**: forgot → OTP → reset flow, show/hide toggle, real-time strength meter (Weak/Fair/Good/Strong), Yup rules (min 8, upper, lower, digit, symbol).
- **Tokens (simulated, prod-shaped)**:
  - Access token (15 min) + refresh token (7 days), JWT-like base64 payload with `exp`.
  - `tokenStorage.ts` keeps refresh in storage, access in-memory + sessionStorage fallback (mitigates naive localStorage XSS surface for the access token while keeping refresh portable).
  - `apiClient` interceptor: on 401 → call `refresh()`; on refresh failure → logout + show "Session expired".
  - Background timer schedules silent refresh ~60s before access expiry.
- **Roles**: `USER | ADMIN | VENDOR` (mapped from existing `customer/vendor/admin`). `RoleRoute` enforces; `useAuth` exposes `is(role)`.
- **Guards**: `ProtectedRoute` (redirects to `/login` with `from`), `PublicRoute` (logged-in users bounced to their dashboard), `RoleRoute` (403-style redirect).
- **API simulation**: every call returns `{ status: 'success'|'error', message, data }`, 300–700ms delay, scripted errors for invalid creds, wrong OTP, expired OTP, email-in-use, weak password.
- **UX**: toast notifications (sonner), inline field errors, loading buttons, error banners, session-expired modal.

### Validation (Yup)

- Email: required, valid format.
- Password: 8+ chars, 1 upper, 1 lower, 1 digit, 1 symbol.
- Confirm password: match.
- Terms: must be true on signup.
- OTP: exactly 6 digits.

### Backwards compatibility

- Keeps existing `customer/vendor/admin` role values (UI uppercase labels only) so vendor onboarding, admin pages, cart, etc. keep working without churn.
- Demo credentials still functional (rahul@example.com / priya@vendor.com / admin@marketplace.com / `password`).
- `useAuth` retains existing surface (`user`, `role`, `isAuthenticated`, `login`, `logout`, `applyAsVendor`, vendor application actions) plus new methods.

### Out of scope

- Real backend, real JWT signing, real email/SMS — all simulated.
- 2FA beyond email OTP.
- OAuth (Google/Apple) — not requested.

### Deliverables

All files above written end-to-end, wired into `App.tsx` routes, no pseudocode. After implementation I'll smoke-test the build and confirm flows compile.
