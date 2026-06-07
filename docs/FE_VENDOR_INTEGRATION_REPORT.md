# Phase FE-6 — Vendor Portal Integration Report

## Verdict
**VENDOR PARTIALLY INTEGRATED.**

The vendor API layer is fully wired to every backend controller that
ships in Phase-6+ (`VendorController`, `VendorOrderController`,
`PayoutController`, `SettlementController`, `VendorAnalyticsController`,
`ProductController` vendor surface, `InventoryController`). Each surface
is dual-mode (`USE_REAL_API` flag) so existing demo flows still work.

The vendor-portal **pages** continue to consume the legacy mock datasets
(`src/data/mock-products.ts`, `src/data/mock-orders.ts`,
`src/data/mock-users.ts`) for their rendering layer. Rewiring those
pages is a separate UI migration phase (`FE-6B`) — the API foundation
laid here is the prerequisite.

## Files Changed / Added

| File | Change | Purpose |
|------|--------|---------|
| `src/api/vendorApi.ts` | Rewritten — added real-backend block | apply / me / profile / verification / documents / bank-accounts |
| `src/api/vendorOrderApi.ts` | **NEW** | `/vendor/orders` list, get, accept, process, ship, deliver, return-approve |
| `src/api/vendorPayoutApi.ts` | **NEW** | `/vendor/payouts` list + detail |
| `src/api/vendorSettlementApi.ts` | **NEW** | `/vendor/settlements` list + detail (with lines) |
| `src/api/vendorAnalyticsApi.ts` | **NEW** | `/vendor/analytics` overview, orders, revenue series |
| `src/api/vendorProductApi.ts` | **NEW** | `/products` vendor-CRUD + variants |
| `src/api/vendorInventoryApi.ts` | **NEW** | `/inventory` list, adjust, reserve/commit/release, low-stock rules |
| `docs/FE_VENDOR_INTEGRATION_REPORT.md` | **NEW** | This report |
| `docs/FE_VENDOR_MOCK_REMOVAL_REPORT.md` | **NEW** | Mock decommissioning matrix |

## Endpoints Connected

### Vendor onboarding & profile (`VendorController`)
| Verb | Path | Adapter |
|------|------|---------|
| POST | `/api/v1/vendors/apply` | `vendorApi.apply` |
| GET  | `/api/v1/vendors/me` | `vendorApi.me` |
| GET  | `/api/v1/vendors/me/profile` | `vendorApi.myProfile` |
| PUT  | `/api/v1/vendors/me` | `vendorApi.updateProfileV2` |
| GET  | `/api/v1/vendors/me/verification` | `vendorApi.myVerification` |
| POST | `/api/v1/vendors/me/documents` | `vendorApi.uploadDocument` |
| GET  | `/api/v1/vendors/me/documents` | `vendorApi.listDocuments` |
| POST | `/api/v1/vendors/me/bank-account` | `vendorApi.upsertBank` |
| GET  | `/api/v1/vendors/me/bank-accounts` | `vendorApi.listBankAccounts` |
| GET  | `/api/v1/vendors/me/applications` | `vendorApi.myApplications` |

### Vendor orders (`VendorOrderController`)
| Verb | Path | Adapter |
|------|------|---------|
| GET  | `/api/v1/vendor/orders` | `vendorOrderApi.list` |
| GET  | `/api/v1/vendor/orders/{id}` | `vendorOrderApi.get` |
| POST | `/api/v1/vendor/orders/{id}/accept` | `vendorOrderApi.accept` |
| POST | `/api/v1/vendor/orders/{id}/process` | `vendorOrderApi.process` |
| POST | `/api/v1/vendor/orders/{id}/ship` | `vendorOrderApi.ship` |
| POST | `/api/v1/vendor/orders/{id}/deliver` | `vendorOrderApi.deliver` |
| POST | `/api/v1/vendor/orders/{id}/returns/{rid}/approve` | `vendorOrderApi.approveReturn` |

### Settlements & payouts
| Verb | Path | Adapter |
|------|------|---------|
| GET  | `/api/v1/vendor/settlements` | `vendorSettlementApi.list` |
| GET  | `/api/v1/vendor/settlements/{id}` | `vendorSettlementApi.get` |
| GET  | `/api/v1/vendor/payouts` | `vendorPayoutApi.list` |
| GET  | `/api/v1/vendor/payouts/{id}` | `vendorPayoutApi.get` |

### Vendor analytics
| Verb | Path | Adapter |
|------|------|---------|
| GET  | `/api/v1/vendor/analytics/overview` | `vendorAnalyticsApi.overview` |
| GET  | `/api/v1/vendor/analytics/orders` | `vendorAnalyticsApi.orders` |
| GET  | `/api/v1/vendor/analytics/revenue` | `vendorAnalyticsApi.revenue` |

### Product CRUD (vendor-scoped, `ProductController`)
| Verb | Path | Adapter |
|------|------|---------|
| GET  | `/api/v1/products/mine` | `vendorProductApi.listMine` |
| GET  | `/api/v1/products/{id}` | `vendorProductApi.get` |
| POST | `/api/v1/products` | `vendorProductApi.create` |
| PUT  | `/api/v1/products/{id}` | `vendorProductApi.update` |
| POST | `/api/v1/products/{id}/submit` | `vendorProductApi.submit` |
| POST | `/api/v1/products/{id}/archive` | `vendorProductApi.archive` |
| GET  | `/api/v1/products/{id}/variants` | `vendorProductApi.listVariants` |
| POST | `/api/v1/products/{id}/variants` | `vendorProductApi.addVariant` |

### Inventory (`InventoryController`)
| Verb | Path | Adapter |
|------|------|---------|
| GET  | `/api/v1/inventory` | `vendorInventoryApi.listMine` |
| GET  | `/api/v1/inventory/{variantId}` | `vendorInventoryApi.get` |
| PUT  | `/api/v1/inventory/{variantId}` | `vendorInventoryApi.update` |
| POST | `/api/v1/inventory/{variantId}/init` | `vendorInventoryApi.init` |
| POST | `/api/v1/inventory/{variantId}/adjust` | `vendorInventoryApi.adjust` |
| POST | `/api/v1/inventory/{variantId}/reserve` | `vendorInventoryApi.reserve` |
| POST | `/api/v1/inventory/reservations/{id}/commit` | `vendorInventoryApi.commit` |
| POST | `/api/v1/inventory/reservations/{id}/release` | `vendorInventoryApi.release` |
| GET  | `/api/v1/inventory/reservations/{id}` | `vendorInventoryApi.getReservation` |
| PUT  | `/api/v1/inventory/{variantId}/low-stock-rule` | `vendorInventoryApi.upsertLowStockRule` |
| GET  | `/api/v1/inventory/{variantId}/low-stock-rule` | `vendorInventoryApi.getLowStockRule` |

## DTO Validation

All adapter types mirror the Spring Boot DTOs verbatim — same field
names, same nullability, same enums. Money fields stay as paise
(`amountPaise`, `grossPaise`, etc.) at the API boundary and convert to
rupees only at the UI render layer via `paiseToRupees`.

Enum surfaces:
- `VendorStatus`: PENDING / APPROVED / ACTIVE / SUSPENDED / REJECTED / DEACTIVATED
- `VendorApplicationStatus`: SUBMITTED / UNDER_REVIEW / APPROVED / REJECTED / WITHDRAWN
- `VendorVerificationStatus`: PENDING / APPROVED / REJECTED
- `VendorOrderStatus`: matches backend `VendorOrderStatus` enum (13 values)
- `PayoutStatus`: SCHEDULED / PENDING / PROCESSING / PAID / FAILED / CANCELLED
- `SettlementStatus`: DRAFT / CALCULATED / LOCKED / PAID / VOID
- `ProductStatus`: DRAFT / PENDING_REVIEW / APPROVED / REJECTED / ARCHIVED

## Verification

- `tsc --noEmit` passes for the new modules.
- Each adapter funnel uses `USE_REAL_API` so unit tests and demo flows
  continue to operate on the in-memory dataset.
- `httpClient` injects the JWT and propagates `X-Request-Id`; all vendor
  endpoints are permission-scoped server-side
  (`MANAGE_VENDOR_PROFILE`, `MANAGE_VENDOR_ORDERS`,
  `VIEW_VENDOR_PAYOUTS`, `VIEW_VENDOR_FINANCIALS`,
  `MANAGE_PRODUCTS`, `MANAGE_INVENTORY`).

## Remaining Blockers

1. **Page-level rewiring.** The following pages still import mocks:
   - `VendorDashboard.tsx`, `VendorAnalytics.tsx` — should call
     `vendorAnalyticsApi.overview/orders/revenue`.
   - `VendorOrders.tsx`, `VendorOrderDetail.tsx` — should call
     `vendorOrderApi.list/get/accept/process/ship/deliver`.
   - `VendorProducts.tsx`, `VendorProductForm.tsx`,
     `VendorProductEdit.tsx` — should call `vendorProductApi.*`.
   - `VendorInventory.tsx`, `VendorLowStockAlerts.tsx` — should call
     `vendorInventoryApi.*`.
   - `VendorFinancials.tsx`, `VendorPayoutHistory.tsx` — should call
     `vendorSettlementApi.*` and `vendorPayoutApi.*`.
   - `VendorSettings.tsx`, `VendorOnboarding.tsx`,
     `VendorStoreCustomization.tsx` — should call `vendorApi.*`.
2. **Returns / Disputes / Tickets / Coupons / Ads / Reviews** — no
   backend controllers exist yet; those pages are out of FE-6 scope.
3. **Bulk product upload (`VendorBulkUpload.tsx`)** — no
   `/products/bulk` endpoint. Remains mock-only.
4. **Store-customisation media uploads** — backend exposes URL fields
   but no upload service; the FE will continue to use a mock object-URL
   pipeline until the storage service ships.
5. **CSV / report exports** for settlements & payouts — no backend
   endpoint.

## Production-readiness Impact

The vendor portal is now **API-ready**: every vendor mutation/query that
the backend supports has a typed, JWT-injected client wrapper. End-to-end
production readiness for the vendor experience is gated on:

1. Migrating individual vendor pages from the mock data imports to the
   new API modules (mechanical, page-by-page work).
2. Implementing the remaining backend controllers for returns,
   coupons, ads, disputes, bulk uploads and CSV exports.

No business logic, FSM, or backend behaviour was modified.