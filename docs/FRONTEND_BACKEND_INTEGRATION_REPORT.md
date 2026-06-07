# Frontend ↔ Backend Integration Certification Report

**Date:** 2026-06-07  **Scope:** Whole platform (frontend + backend + DTO + deployment)
**Verdict:** **NOT PRODUCTION READY**

---

## Executive Summary

The backend (Spring Boot, 38 controllers across 23 bounded contexts, Flyway V001–V019, security hardened,
performance-tuned in Phase 12) is production-grade and certified READY in `docs/GO_LIVE_CERTIFICATION.md`.

The frontend, however, **is not wired to the backend at all**. `src/api/apiClient.ts` is explicitly a
**mock transport** ("Mock API Client / Simulates network requests with configurable delay and error rates").
There is **no `fetch`, no `axios`, no `XMLHttpRequest`** call anywhere in `src/api/`. Every `*Api.ts`
service ultimately resolves against in-memory mock fixtures under `src/mocks/` and `src/data/mock-*`.

`93 source files` directly import mock data or use mock-backed stores. This includes every customer,
vendor, and admin page, every dashboard, every analytics chart, and the auth flow.

There is also **no `.env`, no `.env.production`, and no `VITE_API_URL`** defined — so even if the
transport were swapped, there is no base URL configured for the backend.

## Scorecard (0–10)

| Dimension                  | Score | Notes |
|---------------------------|------:|-------|
| Frontend Integration       |   2   | UI is complete; transport is mock-only. |
| Backend Integration        |   9   | Backend is production-ready and standalone-tested. |
| API Coverage (consumed)    |   1   | 0 of ~38 controllers actually called over HTTP. |
| DTO Consistency            |   5   | Shapes are largely compatible by design, but never exchanged. |
| Authentication Integration |   2   | `authApi.ts` calls the mock client; tokens stored locally only. |
| RBAC Integration           |   4   | Role guards exist client-side; backend RBAC never enforced E2E. |
| Dashboard Integration      |   1   | All metrics from `analyticsData` mock object. |
| Analytics Integration      |   1   | Recharts fed from `mock-orders.ts`. |
| Deployment Readiness       |   3   | Backend deployable; frontend has no API base URL. |
| **Composite**              | **3.1** | |

## Blocker List

| ID | Severity | Area | Description |
|----|----------|------|-------------|
| B1 | CRITICAL | Transport | `src/api/apiClient.ts` is a mock. Needs a real `fetch`/`axios` HTTP client with `VITE_API_URL`, `Authorization: Bearer`, refresh-token interceptor, request-id propagation, standard `{success,data,message,timestamp}` envelope handling. |
| B2 | CRITICAL | Env | No `.env`, `.env.production`, or `VITE_API_URL` defined. Add and document. |
| B3 | CRITICAL | Auth | `authApi.ts` resolves against mock users; real `/api/v1/auth/*` endpoints are never hit. JWT/refresh flow is not exercised E2E. |
| B4 | CRITICAL | Data | All `*Api.ts` modules return data from `src/mocks/*`. Each must be re-pointed at real REST endpoints (see `docs/API_COVERAGE_MATRIX.md`). |
| B5 | HIGH     | Dashboards | `AdminAnalytics`, `VendorAnalytics`, `VendorDashboard` read from `analyticsData` mock. Wire to `/api/v1/admin/analytics/*` and `/api/v1/vendor/analytics/*`. |
| B6 | HIGH     | RBAC | Client routes use `RoleRoute`/`PermissionRoute` but backend enforcement is never reached. Must verify 401/403 handling end-to-end after B1. |
| B7 | HIGH     | CORS | Backend allowed origins not synced with deployed frontend domain. |
| B8 | MED      | Errors | No global error toast/redirect for backend `ApiResponse.error`. |
| B9 | MED      | Refresh | Token refresh hook (`skipAuthRefresh` field exists in mock client) has no real implementation. |

## Required Remediation (high level)

1. Replace `src/api/apiClient.ts` with a real HTTP client (axios or `fetch`) reading
   `import.meta.env.VITE_API_URL`, attaching `Authorization`, handling 401 → refresh → retry,
   propagating `X-Request-Id`, and unwrapping the standard backend envelope.
2. Add `.env.example`, `.env.development`, `.env.production` with `VITE_API_URL`.
3. Audit each `src/api/*Api.ts` and remove `import ... from "@/mocks/..."`.
4. Delete or quarantine `src/mocks/*` and `src/data/mock-*` once API modules are clean.
5. Re-wire `useAuth`, `useProfile`, `useCart`, `useCheckout`, `useOrders`, dashboards, analytics, and admin/vendor stores to call the real APIs.
6. Configure backend `CORS` allowed-origin to match the deployed frontend.
7. Run k6 scenario `11_full_journey.js` against staging from a deployed frontend build to certify.

## Final Verdict

**NOT PRODUCTION READY.** Backend ships; frontend is a high-fidelity prototype on mock data.
The platform cannot operate on real backend data end-to-end until B1–B4 are resolved.