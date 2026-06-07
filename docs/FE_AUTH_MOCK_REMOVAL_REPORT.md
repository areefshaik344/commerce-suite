# FE Auth Mock Removal Report — Phase FE-2

**Date:** 2026-06-07

## Strategy

Mock auth fixtures (`src/mocks/mockUsers.ts`, `mockCredentials`) are still
imported by non-auth subsystems (orders, reviews, vendor applications, admin
seeds). Deleting them in Phase FE-2 would break the build before the rest of
the platform is migrated (see `docs/MOCK_REMOVAL_REPORT.md`).

The chosen approach is **feature-flagged switchover**:

- `VITE_USE_MOCK_API=0` (production default) → `authApi` calls the real backend.
- `VITE_USE_MOCK_API=1` (development default) → `authApi` retains the mock
  transport so reviewers can still log in with the demo credentials.

This eliminates mock auth from the **production code path** while keeping
local-dev usable and avoiding a cascade of unrelated breakage.

## Mock surfaces neutralised in production mode

| File / symbol | Production path | Notes |
|---|---|---|
| `mockCredentials` | not reached | Real `realAuthApi.login` ignores it entirely. |
| `mockUsers` (in `authApi`) | not reached | Real `/me` returns the canonical user. |
| `signMockToken` | not invoked | Tokens come from the backend JWT signer. |
| `OTP store / OTP attempts` (in-memory) | not invoked | Real flows use backend tokens. |
| Hardcoded demo passwords | inert | Backend BCrypt(12) hashes are the only source of truth in real mode. |

## Mock surfaces still present (intentional, dev-only)

- `src/data/mock-users.ts` — defines the **`User` TypeScript type** that the
  whole app is built around. Type-only; no runtime data leaks into production.
- `src/mocks/mockUsers.ts` / `mockCredentials` — consumed by `mockAuthApi` and
  by other yet-to-be-migrated modules (orders, reviews, vendor governance).
  Scheduled for deletion as those modules migrate (see `MOCK_REMOVAL_REPORT.md`).
- `authStore.login(role)` demo role-switcher — falls through to `loginAsync`
  with mock credentials when running against the mock backend; harmless and
  unreachable when `VITE_USE_MOCK_API=0` because the matching demo accounts
  do not exist in the real DB.
- `authStore.vendorApplications` / `approveVendor` / `rejectVendor` — still
  mock-backed; vendor governance migration is a separate phase.

## Verification

- `.env.production` ships with `VITE_USE_MOCK_API=0` → real backend is used in
  any production build.
- `.env.development` ships with `VITE_USE_MOCK_API=1` → mocks remain available
  for local reviewers.
- Type-checks pass; no production import graph reaches `mockUsers` /
  `mockCredentials` for authentication.

## Files changed

- `src/api/authApi.ts` — feature-flagged mock/real adapter.

## Files marked for follow-up deletion

- `src/mocks/mockUsers.ts` (after orders + admin + vendor governance migrations)
- `signMockToken` / OTP store inside `authApi.ts` (after mock fallback is dropped)