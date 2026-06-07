# Integration Gap Analysis

## Blockers (CRITICAL)

| ID | Gap | Evidence | Fix |
|----|-----|----------|-----|
| G1 | Mock HTTP transport | `src/api/apiClient.ts` header: "Mock API Client / Simulates network requests". No `fetch`/`axios` in `src/api/`. | Replace with real `fetch` client; honor `VITE_API_URL`; envelope unwrap; 401 refresh-retry; `X-Request-Id`. |
| G2 | No environment config | No `.env*` file exists. | Add `.env.example`, `.env.development`, `.env.production` with `VITE_API_URL`. |
| G3 | Auth flow on mocks | `authApi.ts` resolves against `mockUsers`. Tokens are fabricated client-side. | Wire to `/api/v1/auth/*`; store access+refresh; httpOnly cookie or secure storage per security memory. |
| G4 | Money type mismatch | Frontend uses `number`; backend uses `BigDecimal` (`MONEY_SPEC.md`). | DTO adapter using integer minor units + currency. |
| G5 | Hardcoded demo credentials in mocks | `src/mocks/mockUsers.ts` ships demo passwords. | Quarantine to `src/test/**` only; never bundle in production build. |

## High

| ID | Gap | Fix |
|----|-----|-----|
| G6 | DTO envelope mismatch (`success`, `timestamp` missing on frontend type) | Update `ApiResponse<T>` shape and unwrap in client. |
| G7 | Pagination shape mismatch (Spring `Page` vs frontend) | Adapter in transport. |
| G8 | Enum casing mismatch (order status, payment methods) | Adapter + alignment in `src/types/order.ts`. |
| G9 | Product attributes/variants shape mismatch | Adapter; update PDP to consume `attributes[]`. |
| G10 | Dashboards/analytics still rendering `analyticsData` mock | Replace `src/data/mock-orders.ts#analyticsData` with `useQuery` over `/api/v1/admin/analytics/*` and `/vendor/analytics/*`. |
| G11 | RBAC never enforced E2E | After G1, validate 401/403 paths via k6 `01_auth.js` + browser session. |
| G12 | CORS allowed origins not aligned with FE domain | Update Spring `CorsConfig` and k8s ingress. |
| G13 | No refresh-token implementation in client | Implement single-flight refresh + retry queue. |
| G14 | Backend `MfaController` orphan | Either build UI for `Profile › Security` or feature-flag off. |

## Medium

| ID | Gap | Fix |
|----|-----|-----|
| G15 | Notification `read` boolean vs `readAt` timestamp | Adapter. |
| G16 | Profile `status` derived locally | Map from backend `active`/`deactivatedAt`. |
| G17 | Order item snapshots vs cached fields | Adapter. |
| G18 | Address `country` default "India" vs ISO code | Normalize to `"IN"`. |
| G19 | Global error toast missing for backend `success:false` | Add in transport layer. |
| G20 | `AdminFraud` page has no controller | Either build controller or hide route. |

## Low

| ID | Gap | Fix |
|----|-----|-----|
| G21 | `Compare`, `RecentlyViewed` are client-only | Acceptable; document. |
| G22 | Real-time notifications via WS not yet exposed | Polling acceptable for v1. |
| G23 | `BrandController` has no UI | Acceptable; admin can manage via API or defer. |

## Deployment / Environment Risks

- **No `VITE_API_URL`** — production build would point at relative `/api` paths with no origin, breaking every call.
- **Token storage** in `src/lib/tokenStorage.ts` uses `localStorage`; review against security posture (XSS surface) — consider memory + refresh cookie.
- **CORS** must allow the frontend origin AND `Authorization`, `X-Request-Id`, `Idempotency-Key` headers.
- **CSP** in deployment manifests must allow API origin.
- **Service worker / caching** — none configured, but if added must bypass `/api/*`.

## Path to "PRODUCTION READY"

1. Land G1–G5 (transport, env, auth, money, demo creds). ~3–5 engineer-days.
2. Land G6–G14 (DTO/RBAC/CORS/refresh). ~5–7 engineer-days.
3. E2E with k6 `11_full_journey.js` against staging from a deployed FE build. ~2 days.
4. Re-run this audit; expected verdict: **PRODUCTION READY WITH INTEGRATION GAPS** (medium items can ship as known-debt).

## Final Verdict

**NOT PRODUCTION READY** — see `docs/FRONTEND_BACKEND_INTEGRATION_REPORT.md` for the full scorecard and blocker list.