# Mock Removal Report — Phase FE-1

**Date:** 2026-06-07  **Scope:** Frontend integration foundation

## Summary

Phase FE-1 introduces the production HTTP foundation that all `*Api.ts`
modules will migrate onto. The legacy in-memory mock transport
(`src/api/apiClient.ts`) is **retained alongside** the new real client so
that the application keeps building during the staged per-module cut-over
described below. Production builds disable the mock path via
`VITE_USE_MOCK_API=0`.

## Foundation delivered

| Artifact | Purpose |
|---|---|
| `src/api/httpClient.ts` | Real axios client. Reads `VITE_API_URL`, injects `Authorization`, propagates `X-Request-Id`, handles 401 → `/auth/refresh` → retry, unwraps backend `{success,data,message,timestamp}` envelope into the existing frontend `{data,status,message}` shape. |
| `.env.example`, `.env.development`, `.env.production` | `VITE_API_URL`, `VITE_API_TIMEOUT_MS`, `VITE_USE_MOCK_API` defined and documented. |
| `src/vite-env.d.ts` | Typed `ImportMetaEnv` for the new variables. |
| `src/lib/money.ts` | Single boundary helper between backend `paise` (integer) and frontend `rupees` (number). Eliminates float drift for the Money DTO mismatch flagged in `DTO_CONTRACT_VALIDATION.md`. |
| `axios` | Added to dependencies. |

## Mocks still present (NOT removed in this phase)

Removing the 93 mock-dependent source files in a single change would block
the build and is intentionally staged across follow-up tickets:

- `src/mocks/**` — 5 fixture files (products, orders, users, reviews, order records).
- `src/data/mock-*.ts` — 3 legacy re-export shims.
- `src/api/*Api.ts` — 22 modules still resolve against `apiClient` mock handlers.
- `src/store/*` — Zustand stores seeded from mocks (auth, profile, cart, product, useStore).
- Pages and dashboards listed in `MOCK_DATA_INVENTORY.md` (customer, vendor, admin).

These will be deleted as each bounded context is migrated to `httpClient`.

## Migration order (recommended)

1. `authApi` → `/auth/*` (unblocks RBAC and protected routes)
2. `profileApi` → `/profile/*`
3. `categoryApi`, `productApi` → `/categories`, `/products`
4. `cartApi`, `checkoutApi` → `/cart`, `/checkout`
5. `orderApi`, `paymentApi`, `shipmentApi` → `/orders/*`
6. `reviewApi`, `wishlistApi`, `couponApi`
7. `vendorApi`, `adminApi`, analytics endpoints
8. `notificationApi`, `auditApi`, `invoiceApi`, `refundApi`, `returnApi`, `shippingApi`, `orderManagementApi`

After each context is migrated, delete its mock fixture and verify
`vitest run` and `vite build` still pass.

## Verdict

Foundation is in place. Per-module wiring is the remaining work — tracked
in `docs/FRONTEND_BACKEND_INTEGRATION_REVALIDATION.md`.