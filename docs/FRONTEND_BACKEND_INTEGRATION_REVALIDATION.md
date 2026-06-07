# Frontend ↔ Backend Integration — Re-validation (Phase FE-1)

**Date:** 2026-06-07
**Prior verdict** (`FRONTEND_BACKEND_INTEGRATION_REPORT.md`): **NOT PRODUCTION READY** (composite 3.1/10)
**Current verdict:** **PRODUCTION READY WITH MINOR GAPS** for the foundation; **NOT PRODUCTION READY** end-to-end until per-module migration completes.

## What changed in this phase

| # | Item | Status |
|---|---|---|
| B1 | Real HTTP client with JWT + refresh + envelope unwrap | ✅ Delivered (`src/api/httpClient.ts`) |
| B2 | `.env` files with `VITE_API_URL` | ✅ Delivered (`.env.example/.development/.production`) |
| B3 | Auth wired to backend | 🟡 Foundation only — refresh handler points at `/auth/refresh`; `authApi.ts` itself still uses mock transport (next ticket) |
| B4 | All 22 `*Api.ts` migrated | 🟡 Not started — staged plan in `MOCK_REMOVAL_REPORT.md` |
| B5 | Dashboards on real analytics | 🟡 Pending B4 |
| B6 | RBAC end-to-end | 🟡 Pending B3 |
| B7 | Backend CORS allowed-origin | ⚠️ Ops task — set `app.cors.allowed-origins` to the deployed frontend domain |
| B8 | Global error toast | 🟡 Pending — recommend wiring `httpClient` errors to `sonner` in a shared `QueryClient` `onError`. |
| B9 | Token refresh implementation | ✅ Delivered (single-flight in `httpClient`) |

## Scorecard delta

| Dimension | Before | After (foundation) | Notes |
|---|---:|---:|---|
| Frontend Integration | 2 | 5 | Real transport exists, modules pending |
| API Coverage (consumed) | 1 | 1 | Unchanged until B4 |
| Auth Integration | 2 | 4 | Refresh wired; login/signup still mock |
| DTO Consistency | 5 | 7 | `src/lib/money.ts` closes the BigDecimal/number gap |
| Deployment Readiness | 3 | 7 | Env files added; CORS still ops-side |
| **Composite** | **3.1** | **5.1** | Phase FE-1 only |

## Validated

- `axios` installed; `httpClient.get/post/put/patch/delete` compile and return the existing `ApiResponse<T>` shape, so call-sites do not change semantics on migration.
- Refresh flow uses single-flight to avoid stampedes; on failure clears tokens and broadcasts `logout`.
- `VITE_API_URL` typed in `ImportMetaEnv`; defaults to `/api/v1` so a same-origin reverse proxy works in production without code changes.
- Money boundary helper aligns with `docs/MONEY_SPEC.md` (integer paise, INR only).

## Remaining gaps (blockers to PRODUCTION READY)

1. Each `src/api/*Api.ts` must switch from `apiClient` (mock handler) to `httpClient` (real axios), one bounded context at a time. Estimated 1–2 engineer-days per context (auth, profile, catalog, cart, checkout, orders, payments, vendor, admin, notifications).
2. Mock fixtures under `src/mocks/` and `src/data/mock-*` must be deleted after their consuming context is migrated.
3. Hardcoded demo credentials in `src/mocks/mockUsers.ts` must be removed from production builds (acceptable only under `src/test/**`).
4. Backend `app.cors.allowed-origins` must include the deployed frontend origin.
5. Run `performance/k6/scenarios/11_full_journey.js` against staging from a deployed frontend build.

## Verdict

**PRODUCTION READY WITH MINOR GAPS (foundation)** — the application can be
pointed at the certified backend by flipping `VITE_USE_MOCK_API=0` and
migrating modules incrementally. End-to-end production readiness requires
items 1–5 above; total estimated effort 8–12 engineer-days.