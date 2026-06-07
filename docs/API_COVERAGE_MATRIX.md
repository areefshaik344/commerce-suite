# API Coverage Matrix

**Legend:** ✅ wired to real backend · 🟡 frontend service exists but transport is mock · ❌ no consumer · ➖ no controller

Across the entire frontend, **zero endpoints** currently reach the backend over HTTP — every row below is 🟡 until `src/api/apiClient.ts` is replaced. The matrix is therefore presented as the *intended* coverage that must be validated post-remediation.

## Auth & Identity

| Screen | Hook | Service | Endpoint | Controller | Status |
|---|---|---|---|---|---|
| `LoginPage` | `useAuth` | `authApi.login` | `POST /api/v1/auth/login` | `AuthController.login` | 🟡 |
| `SignupPage` | `useAuth` | `authApi.signup` | `POST /api/v1/auth/signup` | `AuthController.signup` | 🟡 |
| `EmailVerificationPage` | `useAuth` | `authApi.verifyEmail` | `POST /api/v1/auth/email/verify` | `AuthController.verifyEmail` | 🟡 |
| `ForgotPasswordPage` | `useAuth` | `authApi.forgot` | `POST /api/v1/auth/password/forgot` | `AuthController.forgot` | 🟡 |
| `ResetPasswordPage` | `useAuth` | `authApi.reset` | `POST /api/v1/auth/password/reset` | `AuthController.reset` | 🟡 |
| Session bootstrap | `useAuth` | `authApi.refresh` | `POST /api/v1/auth/refresh` | `AuthController.refresh` | 🟡 |
| Logout button | `useAuth` | `authApi.logout` | `POST /api/v1/auth/logout` | `AuthController.logout` | 🟡 |
| Profile · Sessions | `SessionsManager` | `authApi.logoutAll` | `POST /api/v1/auth/logout/all` | `AuthController.logoutAll` | 🟡 |
| MFA enable/verify | — | ❌ no frontend hook | `/api/v1/mfa/*` | `MfaController` | ❌ orphan |

## Profile & Address

| Screen | Service | Endpoint | Controller | Status |
|---|---|---|---|---|
| `ProfilePage` | `profileApi.get/update` | `/api/v1/profile` | `ProfileController` | 🟡 |
| `AddressManager` | `profileApi.addresses.*` | `/api/v1/addresses` | `AddressController` | 🟡 |
| `NotificationPreferencesForm` | `notificationApi.prefs` | `/api/v1/notifications/preferences` | `NotificationPreferenceController` | 🟡 |

## Catalog

| Screen | Service | Endpoint | Controller | Status |
|---|---|---|---|---|
| `HomePage` | `productApi.featured/trending`, `categoryApi.list` | `/api/v1/products?featured=…`, `/api/v1/categories` | `ProductController`, `CategoryController` | 🟡 |
| `ProductsPage` | `productApi.search` | `/api/v1/products?…&page=` | `ProductController` | 🟡 |
| `ProductDetailPage` | `productApi.get`, `reviewApi.list` | `/api/v1/products/{slug}`, `/reviews` | `ProductController`, `ReviewController` | 🟡 |
| `ComparePage` | `productApi.batch` | `/api/v1/products?ids=` | `ProductController` | 🟡 |
| `VendorStorePage` | `vendorApi.publicStore` | `/api/v1/vendors/{slug}` | `VendorController` | 🟡 |

## Cart, Checkout, Orders, Payments

| Screen | Service | Endpoint | Controller | Status |
|---|---|---|---|---|
| `CartPage` | `cartApi.*` | `/api/v1/cart` | `CartController` | 🟡 |
| `CheckoutPage` | `checkoutApi.*`, `couponApi.apply`, `shippingApi.estimate` | `/api/v1/checkout/*`, `/coupons/apply`, `/shipping/estimate` | `CheckoutController`, `CouponController`, — | 🟡 |
| `OrderSuccessPage` / `PaymentStatusPage` | `paymentApi.confirm` | `/api/v1/payments/*` | `PaymentController` | 🟡 |
| `OrdersPage` / `OrderDetailPage` | `orderApi.*` | `/api/v1/orders` | `OrderController` | 🟡 |
| `ShipmentTrackingPage` | `shipmentApi.get` | `/api/v1/shipments/{id}` | `ShipmentController` | 🟡 |

## Vendor Portal

| Screen | Service | Endpoint | Controller | Status |
|---|---|---|---|---|
| `VendorDashboard`, `VendorAnalytics` | `adminApi/vendorApi` | `/api/v1/vendor/analytics/*` | `VendorAnalyticsController` | 🟡 |
| `VendorProducts`, `VendorProductForm`, `VendorProductEdit` | `productApi` (vendor scope) | `/api/v1/vendor/products` | `AdminProductController` (vendor-scoped variant) | 🟡 |
| `VendorOrders`, `VendorOrderDetail` | `orderManagementApi` | `/api/v1/vendor/orders` | `VendorOrderController` | 🟡 |
| `VendorInventory`, `VendorLowStockAlerts` | — | `/api/v1/inventory` | `InventoryController` | 🟡 |
| `VendorReviews` | `reviewApi.vendor` | `/api/v1/vendor/reviews` | `ReviewController` (vendor scope) | 🟡 |

## Admin Portal

| Screen | Service | Endpoint | Controller | Status |
|---|---|---|---|---|
| `AdminDashboard` / `AdminAnalytics` / `AdminReporting` | `adminApi.analytics` | `/api/v1/admin/analytics/*` | `AdminAnalyticsController` | 🟡 |
| `AdminOrders` / `AdminOrderDetail` | `adminApi.orders` | `/api/v1/admin/orders` | `AdminOrderController` | 🟡 |
| `AdminProducts` / `AdminProductDetail` | `adminApi.products` | `/api/v1/admin/products` | `AdminProductController` | 🟡 |
| `AdminVendors` / `AdminVendorDetail` / `AdminVendorApplications` | `adminApi.vendors` | `/api/v1/admin/vendors` | `AdminVendorController` | 🟡 |
| `AdminUsers` / `AdminUserDetail` | `adminApi.users` | `/api/v1/admin/users` | (admin user controller) | 🟡 |
| `AdminCoupons` / `AdminCreateCoupon` | `couponApi.admin` | `/api/v1/admin/coupons` | `CouponController` (admin) | 🟡 |
| `AdminCommission` | `adminApi.commission` | `/api/v1/admin/commission` | (commission controller) | 🟡 |
| `AdminCategories`, `AdminCMS`, `AdminBannerForm`, `AdminEmailTemplates` | `adminApi.cms` | `/api/v1/admin/cms/*` | — partial | 🟡 |
| `AdminFraud` | — | n/a (mock only) | ➖ | ❌ no controller |
| `AdminAuditLog` | `auditApi.list` | `/api/v1/admin/audit` | `AdminAuditController` | 🟡 |
| `AdminSettings` | `adminApi.settings` | `/api/v1/admin/settings` | — partial | 🟡 |

## Orphans (backend without consumer)

- `MfaController` — no frontend MFA enrollment screen.
- `DlqAdminController` — no UI; CLI-only.
- `AdminWebhookController` — no UI; CLI-only (intentional).
- `AdminPayoutController`, `PayoutController`, `AdminSettlementController`, `SettlementController` — no UI surface.
- `MetricsController` — Prometheus scrape only (intentional).
- `BrandController` — no brand-management UI.

## Gaps (frontend without backend)

- `AdminFraud` page — no controller.
- `Compare` & `RecentlyViewed` are pure client-state; OK to remain client-side.
- `Notifications` real-time push — backend currently polled; WS endpoint not exposed.