# Marketplace Commerce Suite — Master Architecture & System Audit

> Source of truth: current React + Vite + TypeScript codebase under `src/`.
> Audience: frontend engineers, future Spring Boot backend team, QA, product, and SRE.
> Status: production-hardened frontend with mock API layer; backend integration pending.

---

## 1. Executive Summary

**Platform.** A multi-vendor B2C e-commerce marketplace (Flipkart-inspired) where independent sellers list products and end-customers transact under a centrally governed platform operated by Admin staff.

**Business model.** Commission-based marketplace:
- Vendors onboard, are KYC-verified, then list products.
- Customers browse, purchase, and receive items shipped per-vendor.
- The platform collects payment, splits orders per vendor, takes a commission, and orchestrates returns/refunds.

**Roles.**
- **Customer** — browse, wishlist, cart, checkout, orders, returns, reviews.
- **Vendor** — onboard, manage catalog/inventory, fulfill orders, handle returns, view analytics.
- **Admin** — govern vendors, moderate catalog, oversee orders/refunds, manage CMS, audit.

**High-level architecture.**
- **Frontend-first**: React 18 + Vite 5 + TS 5 + Tailwind + shadcn/ui + Zustand + TanStack Query.
- **API abstraction layer** (`src/api/*`) returns a frozen DTO envelope `{ success, data, message, timestamp }`. Currently mock; designed for 1:1 swap to a Spring Boot REST backend.
- **Domain-driven slices**: each domain owns `types/`, `api/`, `store/`, `lib/`, `hooks/`, and `components/`.
- **Immutable order snapshots** decouple historical records from live catalog state.
- **In-process Event Bus** (`src/lib/eventBus.ts`) publishes typed domain events consumed by Audit, Notifications, Analytics, and Webhook Outbox subscribers — already shaped for a future message-queue backend.

**Key architectural decisions.**
1. Strict separation of UI ↔ store ↔ API ↔ DTO; UI components never embed business rules.
2. RBAC + ownership through a single `usePermissions()` hook and `<Can />` gate; no scattered role checks.
3. Pure-function selectors and pricing/status engines (`lib/pricing.ts`, `lib/orderStatus.ts`, `lib/orderSelectors.ts`).
4. Webhook-ready event envelopes (`WebhookEventDTO`) buffered in an outbox for backend forwarding.
5. Money is integer rupees throughout; no float arithmetic.
6. URL-synced pagination, semantic design tokens only, dark-mode default.

---

## 2. Module Inventory

| Module | Purpose | Key files | Stores | APIs | Pages | Permissions |
|---|---|---|---|---|---|---|
| **Auth** | Login, signup, OTP, session, password reset | `pages/auth/*`, `store/authStore`, `lib/tokenStorage`, `lib/authEvents` | authStore | authApi | Login/Signup/Reset/EmailVerification | `IS_AUTHENTICATED` |
| **Profile** | Personal details, addresses, sessions, prefs | `components/profile/*`, `lib/profileValidation` | profileStore | profileApi | ProfilePage | owner-only |
| **RBAC** | Roles, permissions, ownership | `lib/permissions`, `hooks/usePermissions`, `components/auth/Can`, `routes/PermissionRoute` | authStore | — | (all gated pages) | per matrix §8 |
| **Vendor Onboarding** | KYC, GST, PAN, bank, approval lifecycle | `pages/auth/VendorRegister*`, `store/vendorOnboardingStore` | vendorOnboardingStore | vendorApi | VendorRegister, VendorOnboarding | `APPLY_VENDOR` → `MANAGE_*` after approval |
| **Catalog** | Products, variants, attributes, categories | `types/catalog`, `types/catalogDto`, `config/categoryAttributes`, `lib/catalogSelectors`, `lib/productOwnership` | productStore, categoryStore | productApi, categoryApi | Products/ProductDetail/VendorProducts/AdminProducts | `MANAGE_PRODUCTS` (+ownership) |
| **Inventory** | Stock levels, reservation, low-stock | embedded in catalog DTO; `VendorInventory`, `VendorLowStockAlerts` | productStore | productApi | VendorInventory | `MANAGE_INVENTORY` |
| **Cart** | Multi-vendor cart, save-for-later | `store/cartStore`, `components/cart/*` | cartStore | cartApi | CartPage | customer |
| **Checkout** | Address/payment/review, reservation, pricing | `store/checkoutStore`, `lib/pricing`, `hooks/useCheckout` | checkoutStore, couponStore | checkoutApi, couponApi | CheckoutPage, Success/Failure | customer |
| **Orders** | Immutable order snapshots, vendor splits | `types/order`, `lib/orderFactory`, `lib/orderSelectors`, `lib/orderStatus` | orderStore | orderApi, orderManagementApi | Orders, OrderDetail(s), VendorOrders, AdminOrders | per role + ownership |
| **Shipping** | Packages, legs, tracking, labels, estimates | `types/shipping`, `api/shippingApi`, `components/shipping/*` | shipmentStore | shipmentApi, shippingApi | ShipmentTrackingPage, VendorShipping | vendor scope |
| **Payments** | Intents, attempts, retries, refunds | `types/payment`, `api/paymentApi`, `api/refundApi` | paymentStore | paymentApi, refundApi | PaymentStatusPage | customer/admin |
| **Returns/Refunds** | Return window, approval, refund pipeline | `api/returnApi`, `api/refundApi`, components/orders/ReturnRequestDialog | orderStore, paymentStore | returnApi, refundApi | VendorReturns | per role |
| **Notifications** | Event-driven user notifications + prefs | `types/notification`, `store/notificationStore`, `components/notifications/*` | notificationStore | notificationApi | NotificationsPage | authenticated |
| **Audit** | Append-only audit log | `types/audit`, `store/auditStore`, `lib/subscribers/auditSubscriber` | auditStore | auditApi | AdminAuditLog | `VIEW_AUDIT` (admin) |
| **Analytics** | Provider-agnostic analytics envelopes | `lib/analyticsBus`, `lib/subscribers/analyticsSubscriber`, `hooks/useAnalytics` | — | — | AdminAnalytics, VendorAnalytics | per role |
| **Event Bus** | Typed in-process pub/sub + history | `lib/eventBus`, `types/events`, `hooks/useDomainEvents` | — | — | — | — |
| **Webhooks** | Outbox of WebhookEventDTO for backend | `lib/webhookOutbox` | — | — | — | — |
| **Invoices** | Immutable invoice snapshots | `types/invoice`, `store/invoiceStore`, `api/invoiceApi` | invoiceStore | invoiceApi | (download from OrderDetail) | owner/admin |
| **CMS** | Banners, email templates, legal pages | `pages/admin/AdminCMS`, `AdminBannerForm`, `AdminEmailTemplates`, static pages | uiStore | adminApi | Admin CMS suite | `MANAGE_CMS` |

---

## 3. Complete Customer Journey

1. **Register** → `SignupPage` → `authApi.signup` → email verification token issued.
2. **Verify** → `EmailVerificationPage` → `authApi.verifyEmail`; account moves `PENDING_VERIFICATION → ACTIVE`.
3. **Login** → `LoginPage` (password or OTP via `OtpInput`) → tokens stored via `tokenStorage`; `authEvents` broadcasts `USER_LOGGED_IN`.
4. **Profile** → addresses, prefs (`AddressManager`, `NotificationPreferencesForm`). Validation in `lib/profileValidation`.
5. **Browse** → `HomePage`, `ProductsPage` with URL-synced `?page=`, filters via `hooks/useProductFilters`. Recently viewed via `recentlyViewedStore`.
6. **Search** → debounced `SearchBar`, suggestions through `productApi.search`.
7. **Wishlist** → `WishlistButton` toggles via `wishlistStore`; persisted server-side.
8. **Cart** → `cartStore.addItem` groups by vendor; per-vendor subtotals & shipping; Save-for-later supported.
   - **Rules:** quantity ≤ stock; variant must be in stock; inactive products auto-removed.
9. **Checkout** (3 steps via `CheckoutStepper`):
   - **Address** → choose from `AddressSelector` or create.
   - **Payment** → `PaymentMethodSelector` filtered by `prepaid/COD` capabilities.
   - **Review** → `OrderReview` + `PriceBreakdown`; inventory reservation TTL via `ReservationTimer`.
10. **Payment** → `paymentApi.createIntent` → confirm → `PaymentIntent` transitions `CREATED→AUTHORIZED→CAPTURED`. Retries up to `PAYMENT_RETRY_LIMIT=3`. Failure routes to `CheckoutFailurePage`.
11. **Order placed** → `orderFactory.fromCheckout` produces immutable `OrderRecord` split into `VendorOrder[]`. `ORDER_CREATED` event fires → notifications, audit, analytics, webhook outbox.
12. **Shipment** → vendor advances `PACKING→READY_TO_SHIP→IN_TRANSIT→OUT_FOR_DELIVERY→DELIVERED`. Tracking via `ShipmentTrackingPage`.
13. **Delivery** → derives `OrderStatus = DELIVERED` once all shipments delivered.
14. **Return** → 7-day window enforced by `getReturnableItems(order)`. `ReturnRequestDialog` → `RETURN_REQUESTED`.
15. **Refund** → after `RETURN_APPROVED` + `PICKED_UP`, `refundApi` creates `RefundTransaction`; payment `PARTIALLY_REFUNDED` or `REFUNDED`.

**Failure scenarios:** payment failure (retry/cancel), reservation expiry (cart re-priced), out-of-stock mid-checkout (item flagged), address invalid (blocked), coupon invalid (re-quoted).

---

## 4. Complete Vendor Journey

1. **Apply** from customer account → `VendorRegisterPage` (multi-step: business, KYC PAN/GST, bank).
2. **Pending Review** — status `PENDING_APPROVAL`, vendor portal access limited; banner via `VendorStatusBadge`.
3. **Approved** by admin → permissions widen to `MANAGE_PRODUCTS`, `MANAGE_INVENTORY`, `MANAGE_VENDOR_ORDERS`.
4. **Product Creation** — `VendorProductForm` (variants per `categoryAttributes`); ownership stamped via `productOwnership`.
5. **Inventory** — `VendorInventory`, low-stock alerts (`VendorLowStockAlerts`), CSV bulk via `VendorBulkUpload`.
6. **Order Fulfillment** — `VendorOrders` shows only that vendor's `VendorOrder` slice.
7. **Shipments** — generate label, advance status; events emit `SHIPMENT_UPDATED`.
8. **Returns** — approve/reject in `VendorReturns`; emits `RETURN_APPROVED|REJECTED`.
9. **Refunds** — triggered by platform after return pickup; vendor sees in `VendorFinancials`/`VendorPayoutHistory`.

**Restrictions:** cannot view/manage other vendors' resources (`canManage(ownerId, perm)`); suspended vendors lose mutation permissions; products require admin moderation before public listing.

---

## 5. Complete Admin Journey

- **Vendor approvals** — `AdminVendorApplications`/`AdminVendorDetail` → emits `VENDOR_APPROVED|REJECTED|SUSPENDED`.
- **Product moderation** — `AdminProducts`/`AdminProductDetail` toggles publish state.
- **User management** — `AdminUsers`/`AdminUserDetail`; role assignment must remain in `user_roles` table once backend lands.
- **Order oversight** — `AdminOrders`/`AdminOrderDetail` (read-only mutation API for status overrides).
- **Shipment oversight** — read-only across all vendors.
- **Refund oversight** — manual refund via `refundApi.create` with `sourceType=ADJUSTMENT`.
- **Audit** — `AdminAuditLog` consumes `auditStore`.
- **CMS** — banners, email templates, commissions, coupons, fraud, settings.

**Restrictions:** Admin actions are all audited (`auditSubscriber` listens to wildcard).

---

## 6. Screen Inventory (abbreviated; complete list in §7)

| Page | Route | Roles | Permissions | Stores | APIs |
|---|---|---|---|---|---|
| HomePage | `/` | public | — | productStore, categoryStore | productApi |
| ProductsPage | `/products` | public | — | productStore | productApi |
| ProductDetailPage | `/product/:slug` | public | — | productStore, wishlistStore | productApi, reviewApi |
| CartPage | `/cart` | customer | — | cartStore | cartApi |
| CheckoutPage | `/checkout` | customer | `IS_AUTHENTICATED` | checkoutStore, couponStore | checkoutApi, couponApi, paymentApi |
| OrdersPage | `/orders` | customer | own | orderStore | orderApi |
| OrderDetailsPage | `/orders/:id` | customer (owner) | own | orderStore, shipmentStore | orderManagementApi |
| ShipmentTrackingPage | `/tracking/:id` | customer (owner) | own | shipmentStore | shippingApi |
| PaymentStatusPage | `/payments/:id` | customer (owner) | own | paymentStore | paymentApi |
| NotificationsPage | `/notifications` | authenticated | own | notificationStore | notificationApi |
| ProfilePage | `/profile` | authenticated | own | profileStore | profileApi |
| WishlistPage | `/wishlist` | customer | own | wishlistStore | wishlistApi |
| VendorRegisterPage | `/vendor/register` | customer | `APPLY_VENDOR` | vendorOnboardingStore | vendorApi |
| VendorDashboard | `/vendor` | vendor | `VIEW_VENDOR_DASHBOARD` | productStore, orderStore | vendorApi |
| VendorProducts/Form | `/vendor/products*` | vendor | `MANAGE_PRODUCTS` + ownership | productStore | productApi |
| VendorOrders/Detail | `/vendor/orders*` | vendor | `MANAGE_VENDOR_ORDERS` + ownership | orderStore | orderManagementApi |
| VendorShipping | `/vendor/shipping` | vendor | `MANAGE_SHIPMENTS` | shipmentStore | shipmentApi, shippingApi |
| VendorReturns | `/vendor/returns` | vendor | `MANAGE_RETURNS` | orderStore | returnApi |
| VendorFinancials/Payouts | `/vendor/finance*` | vendor | `VIEW_FINANCIALS` | invoiceStore | invoiceApi |
| AdminDashboard | `/admin` | admin | `VIEW_ADMIN_DASHBOARD` | many | adminApi |
| AdminVendor* | `/admin/vendors*` | admin | `MANAGE_VENDORS` | — | vendorApi, adminApi |
| AdminProducts/Detail | `/admin/products*` | admin | `MODERATE_PRODUCTS` | productStore | productApi |
| AdminOrders/Detail | `/admin/orders*` | admin | `VIEW_ALL_ORDERS` | orderStore | orderManagementApi |
| AdminAuditLog | `/admin/audit` | admin | `VIEW_AUDIT` | auditStore | auditApi |
| AdminCMS/Banner/Email | `/admin/cms*` | admin | `MANAGE_CMS` | uiStore | adminApi |

(Static pages: About, Contact, FAQ, Privacy, Terms — public.)

---

## 7. Route Map

```
Public
├── /                          HomePage
├── /products, /product/:slug
├── /store/:slug               VendorStorePage
├── /about /contact /faq /privacy /terms
├── /auth/login /signup /forgot /reset /verify-email
└── /vendor/register-success

Authenticated (ProtectedRoute)
├── /profile /notifications /wishlist
├── /cart
├── /checkout /checkout/success /checkout/failure
├── /orders /orders/:id
├── /tracking/:id /payments/:id
└── /vendor/register

Customer-only (RoleRoute=CUSTOMER)
└── (cart/checkout/orders inherit)

Vendor (RoleRoute=VENDOR, PermissionRoute on each)
├── /vendor                    dashboard
├── /vendor/products[/new|/:id/edit]
├── /vendor/inventory /vendor/low-stock /vendor/bulk-upload
├── /vendor/orders[/:id]
├── /vendor/shipping /vendor/returns
├── /vendor/coupons[/new] /vendor/ads
├── /vendor/finance /vendor/payouts
├── /vendor/reviews /vendor/disputes /vendor/tickets
├── /vendor/analytics /vendor/performance
└── /vendor/settings /vendor/store-customization /vendor/onboarding

Admin (RoleRoute=ADMIN, PermissionRoute)
├── /admin                     dashboard
├── /admin/users[/:id]
├── /admin/vendors[/:id] /admin/vendor-applications
├── /admin/products[/:id] /admin/categories
├── /admin/orders[/:id]
├── /admin/coupons[/new] /admin/commission
├── /admin/cms /admin/banners /admin/email-templates
├── /admin/fraud /admin/audit /admin/reporting /admin/analytics
└── /admin/settings
```

---

## 8. RBAC Documentation

**Roles** (`AppRole`): `CUSTOMER`, `VENDOR`, `ADMIN` (with optional `SUPER_ADMIN` mapping).
**Storage rule:** roles MUST live in a dedicated `user_roles` table (never on profile). Checked via `has_role()` SECURITY DEFINER function once on Supabase.

**Permissions** (representative — full enum in `lib/permissions.ts`):

| Permission | Customer | Vendor (Approved) | Admin |
|---|:-:|:-:|:-:|
| BROWSE_CATALOG | ✓ | ✓ | ✓ |
| MANAGE_CART / CHECKOUT | ✓ |  |  |
| VIEW_OWN_ORDERS | ✓ | ✓ | ✓ |
| APPLY_VENDOR | ✓ |  |  |
| MANAGE_PRODUCTS (ownership) |  | ✓ | ✓ |
| MANAGE_INVENTORY (ownership) |  | ✓ | ✓ |
| MANAGE_VENDOR_ORDERS (ownership) |  | ✓ | ✓ |
| MANAGE_SHIPMENTS / RETURNS (ownership) |  | ✓ | ✓ |
| VIEW_FINANCIALS (ownership) |  | ✓ | ✓ |
| MODERATE_PRODUCTS |  |  | ✓ |
| MANAGE_VENDORS |  |  | ✓ |
| VIEW_ALL_ORDERS |  |  | ✓ |
| MANAGE_CMS / COUPONS / COMMISSION |  |  | ✓ |
| VIEW_AUDIT |  |  | ✓ |

**Ownership model.** `ownsResource(user, ownerId)` + `canManage(ownerId, perm)` from `usePermissions`. Admin bypasses ownership.
**Account lifecycle states:** `PENDING_VERIFICATION → ACTIVE → SUSPENDED → DEACTIVATED`; vendor adds `PENDING_APPROVAL → APPROVED|REJECTED|SUSPENDED`. `resolveAccountStatus` derives the effective state and `canPerform(action)` blocks restricted actions.

**Components.** `<Can perm|all ownerId>`, `<PermissionRoute>`, `<RoleRoute>`, `<ProtectedRoute>`.

---

## 9. Product Domain

**Entities:** `Product`, `ProductVariant`, `InventoryRecord`, `Category` (multi-level), `Brand`, `MediaAsset`, `AttributeDefinition`, `Review`.

**Dynamic attributes.** `config/categoryAttributes.ts` maps each category leaf to required/optional attribute keys + value types. Variants are generated as the Cartesian product of attribute sets declared per category.

**Variant model.** Each variant carries its own SKU, price, compare-at price, stock, media, and attribute map. Pricing helpers in `lib/pricing.ts` always derive from variant.

**Inventory.** Stock fields: `available`, `reserved`, `safetyStock`. Reservation is created at checkout (`checkoutApi.reserve`) with TTL; released on cancel/expiry.

**Ownership rules.** `productOwnership.ts`:
- Product is owned by its vendor.
- Vendors can only mutate products where `product.vendorId === user.vendorId`.
- Admin can moderate but not impersonate vendor for analytics.

**Reviews.** Star ratings + text + media; vendor reply supported via `reviewApi.reply`.

---

## 10. Checkout Documentation

**Stores:** `cartStore`, `checkoutStore`, `couponStore`.
**Engine:** pure `lib/pricing.ts` (line totals → vendor subtotals → coupon → shipping → tax → grand total). Money is integer paise/rupees.

**Sequence — place order:**
```
UI → checkoutStore.start()
   → checkoutApi.reserve(cart)           // creates ReservationToken (TTL)
   → couponApi.validate(code)            // optional
   → pricing.quote(cart, address, ship)  // pure
   → paymentApi.createIntent(quote)      // returns PaymentIntent
   → paymentApi.confirm(intentId)        // attempts capture
   ↘ on success → orderFactory.fromCheckout → orderStore.add
                → eventBus.publish(ORDER_CREATED)
   ↘ on failure → CheckoutFailurePage, retry up to PAYMENT_RETRY_LIMIT
```

**Coupon engine.** Stacked rules: scope (cart/vendor/category/product), type (FLAT/PERCENT/SHIPPING), constraints (min cart, max discount, user/usage limits).

---

## 11. Order Domain

**Snapshot principle.** `OrderRecord` freezes price, vendor info, product title, and tax breakdown at placement. Catalog changes never alter historical orders.

**Structure.** `OrderRecord → VendorOrder[] → OrderItem[] + Shipment[] + PaymentRecord`.

**State machines** (`lib/orderStatus.ts`):
- Order: `CREATED → CONFIRMED → PROCESSING → PARTIALLY_SHIPPED → SHIPPED → DELIVERED → (RETURNED → REFUNDED)`; lateral `CANCELLED → REFUNDED`.
- Shipment: `PACKING → READY_TO_SHIP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED`; lateral `FAILED_DELIVERY`.
- Return: `REQUESTED → APPROVED|REJECTED → PICKED_UP → REFUNDED`.
- Payment: `PENDING → AUTHORIZED → CAPTURED → (PARTIALLY_)REFUNDED|FAILED`.

**Selectors.** `getCancellableItems`, `getReturnableItems` (7-day window), `deriveOrderStatusFromShipments`.

---

## 12. Shipping Documentation

**Operational layer** (`types/shipping.ts`) is separate from the embedded `Shipment` snapshot.

Entities: `ShipmentPackage`, `ShipmentLeg`, `TrackingEvent`, `DeliveryEstimate`, `ShippingLabel`, `FulfillmentTask`, `CourierProvider`.

Granular `FulfillmentStatus` collapses to canonical `ShipmentStatus` via `toShipmentStatus()` — UI binds only to canonical status.

Vendor advances state via `shipmentApi.transition()`. Each transition emits `SHIPMENT_UPDATED`, recorded on the order timeline.

---

## 13. Payment Documentation

Provider-agnostic abstraction (`types/payment.ts`). UI **must branch on `PaymentMethodKind`**, never gateway name.

Flow: `Intent → Attempt(s) → TransactionRecord (AUTH|CAPTURE|REFUND|VOID)`.
Retries bounded by `PAYMENT_RETRY_LIMIT=3`; intents expire after `PAYMENT_INTENT_TTL_MINUTES=15`.

Refunds: `RefundTransaction` with `sourceType ∈ {CANCELLATION, RETURN, ADJUSTMENT}`; partial refunds supported (`capturedAmount`, `refundedAmount` ledger).

Future gateway integration: implement a `PaymentGatewayAdapter` interface mapping to Razorpay/Stripe/PayU without changing UI or store.

---

## 14. Notification Documentation

Architecture: event-driven. `notificationSubscriber` listens to domain events and calls `notificationApi.create()` shaped by `NotificationPreferenceMatrix` (per-channel: EMAIL/SMS/PUSH/IN_APP).

Delivery flow: domain event → matrix check → fanout per enabled channel → `notificationStore` updates in-app feed + unread badge. Server-side delivery (email/SMS/push) will be backend responsibility consuming the same envelope.

---

## 15. Event Bus Documentation

`src/lib/eventBus.ts`: typed `on/off/onAny/publish/republish` with bounded history (200). Wildcard subscribers used by audit, analytics, webhook outbox. Source inferred from event prefix (`ORDER_*`, `PAYMENT_*`, …).

### Event Matrix

| Event | Publisher | Subscribers |
|---|---|---|
| USER_REGISTERED | authApi | notification, audit, analytics, webhook |
| USER_LOGGED_IN | authStore | audit, analytics, webhook |
| VENDOR_APPLIED | vendorApi | notification (admin), audit, webhook |
| VENDOR_APPROVED / REJECTED / SUSPENDED | adminApi | notification (vendor), audit, analytics, webhook |
| PRODUCT_PUBLISHED | productApi/admin | analytics, webhook |
| CART_UPDATED | cartStore | analytics |
| CHECKOUT_STARTED | checkoutStore | analytics, audit |
| ORDER_CREATED | orderFactory via checkout | notification, audit, analytics, webhook |
| ORDER_CANCELLED | orderManagementApi | notification, audit, analytics, webhook |
| SHIPMENT_UPDATED | shipmentApi | notification, audit, webhook |
| PAYMENT_AUTHORIZED / CAPTURED / FAILED | paymentApi | notification, audit, analytics, webhook |
| RETURN_REQUESTED / APPROVED / REJECTED | returnApi | notification, audit, webhook |
| REFUND_INITIATED / COMPLETED | refundApi | notification, audit, analytics, webhook |

---

## 16. Audit Documentation

`AuditRecord { id, actor, action, entityType, entityId, severity, payload, timestamp }` — append-only.
Source: wildcard subscriber on event bus + explicit `auditApi.record()` for admin overrides.
Query: `auditApi.list({ entityType, entityId, actorId, limit })`; UI in `AdminAuditLog`.

---

## 17. Analytics Documentation

`analyticsBus.track(envelope)` consumed by `analyticsSubscriber`. Envelope shape is GA4-compatible; backend can fanout to GA4 / Segment / internal warehouse without UI changes. Vendor & Admin analytics pages read aggregated mocks today; will read backend reporting endpoints later.

---

## 18. Webhook Documentation

`webhookOutbox` mirrors every domain event into `WebhookEventDTO` (signed envelope shape) bounded to 500. Backend will subscribe to outbox drain endpoint, sign, and forward to registered webhook URLs with HMAC + retry.

`WebhookEventDTO` fields: `id, type, version, occurredAt, source, actor?, correlationId?, payload`.

---

## 19. Store Dependency Map

```
authStore
├── profileStore
├── cartStore
├── orderStore
├── notificationStore
└── auditStore (admin)

cartStore
├── productStore (variant lookups)
└── checkoutStore (consumer)

checkoutStore
├── cartStore
├── couponStore
├── paymentStore
└── orderStore (writes on success)

orderStore
├── shipmentStore
├── paymentStore
└── invoiceStore

vendorOnboardingStore → authStore (role upgrade)
productStore → categoryStore
```

---

## 20. State Machines (summary)

- **Vendor:** `PENDING_APPROVAL → APPROVED | REJECTED → SUSPENDED ↔ APPROVED`.
- **Account:** `PENDING_VERIFICATION → ACTIVE → SUSPENDED → DEACTIVATED`.
- **Order/Shipment/Return/Payment:** see §11.
- **Refund:** `PENDING → PROCESSING → COMPLETED | FAILED`.

All transitions are guarded by `canTransition*()` and emit domain events.

---

## 21. Data Model (logical)

```
User 1—1 Profile 1—* Address
User 1—* UserRole *—1 Role
User 1—0..1 Vendor 1—* Product 1—* Variant 1—1 Inventory
Category 1—* Product;   Category self-referential (tree)
User 1—1 Cart 1—* CartItem (→ Variant)
Cart 1—* CheckoutSession 1—* Reservation
Order 1—* VendorOrder 1—* OrderItem
VendorOrder 1—* Shipment 1—* Package, Leg, TrackingEvent
Order 1—1 PaymentIntent 1—* Attempt 1—* Transaction
Order 1—* RefundTransaction
Order 1—1 Invoice
User 1—* Notification
* —* Audit (polymorphic entityType/entityId)
```

---

## 22. DTO ↔ Entity ↔ Table Mapping

| Frontend DTO | Backend Entity (proposed) | Table |
|---|---|---|
| `ProductDetailDto` | `Product` | `products` |
| `ProductVariantDto` | `Variant` | `product_variants` |
| `InventoryDto` | `Inventory` | `inventory` |
| `CategoryDto` | `Category` | `categories` |
| `CartDto`/`CartItem` | `Cart`/`CartItem` | `carts`, `cart_items` |
| `CheckoutQuote` | `CheckoutSession` | `checkout_sessions` |
| `OrderRecord` | `Order` | `orders` |
| `VendorOrder` | `VendorOrder` | `vendor_orders` |
| `OrderItem` | `OrderItem` | `order_items` |
| `Shipment` | `Shipment` | `shipments` |
| `ShipmentPackage`/`Leg`/`TrackingEvent` | resp. | `shipment_packages`, `shipment_legs`, `tracking_events` |
| `PaymentIntent` | `PaymentIntent` | `payment_intents` |
| `PaymentAttempt`/`TransactionRecord` | resp. | `payment_attempts`, `payment_transactions` |
| `RefundTransaction` | `Refund` | `refunds` |
| `Invoice` | `Invoice` | `invoices` |
| `NotificationEvent` | `Notification` | `notifications` |
| `AuditRecord` | `AuditEvent` | `audit_events` |
| `WebhookEventDTO` | `OutboxEvent` | `outbox_events` |
| `UserDto` | `User` | `users` (+ `user_roles`, `profiles`, `addresses`, `vendor_profiles`) |

---

## 23. API Contract Documentation

All responses share `ApiResponse<T> = { success, data, message, timestamp }`. Errors throw `ApiError(code, message, status, details?)`.

Key endpoints (mock today, REST tomorrow):
- `auth`: `POST /auth/signup|login|otp|verify-email|forgot|reset|logout`.
- `profile`: `GET/PUT /profile`, `GET/POST/PUT/DELETE /profile/addresses`.
- `catalog`: `GET /products`, `GET /products/:slug`, `GET /categories`.
- `vendor`: `POST /vendors/apply`, `GET /vendors/:id`, `GET /vendors/:id/products`.
- `cart`: `GET/POST/PATCH/DELETE /cart` + `/cart/items/:id`.
- `checkout`: `POST /checkout/reserve`, `POST /checkout/quote`, `POST /checkout/place`.
- `coupon`: `POST /coupons/validate`.
- `orders`: `GET /orders`, `GET /orders/:id`, `POST /orders/:id/cancel`.
- `shipments`: `GET /shipments/:id`, `POST /shipments/:id/transition`, tracking feed.
- `returns`: `POST /returns`, `PATCH /returns/:id`.
- `payments`: `POST /payments/intent`, `POST /payments/:id/confirm`, `POST /payments/:id/retry`.
- `refunds`: `POST /refunds`, `GET /refunds?orderId=`.
- `invoices`: `GET /invoices/:orderId`.
- `notifications`: `GET /notifications`, `PATCH /notifications/:id/read`, `GET/PUT /notifications/preferences`.
- `audit`: `GET /audit?entityType=&entityId=`.

Validation: client-side via Zod-style validators in `lib/validation.ts`; backend MUST re-validate.

---

## 24. Security Review

**Implemented (frontend):**
- Role-aware routing, permission gates, ownership checks.
- Token storage abstraction (`lib/tokenStorage`), session-expiry dialog.
- OTP flow with rate-limited resend (`useOtpTimer`).
- Password strength meter and policy.
- Audit log of admin/vendor mutations via event bus.

**Missing / backend-required:**
- Real JWT validation, refresh-token rotation, httpOnly cookies.
- Server-side RBAC enforcement, `has_role()` security-definer function.
- RLS on every public table with explicit `GRANT`s to `authenticated`/`service_role`; never grant `anon` write.
- Webhook signing (HMAC), idempotency keys on payment/refund routes.
- Rate limiting on auth, checkout, search.
- PII encryption at rest, KYC document storage in private bucket with signed URLs.
- CSRF protection for cookie-based sessions.
- Audit immutability (append-only DB, no UPDATE/DELETE grants).

---

## 25. Business Rules Catalog

1. Vendor must be `APPROVED` before any product becomes public.
2. Product publication requires admin moderation (`MODERATE_PRODUCTS`).
3. Vendors can only mutate their own products/orders/shipments.
4. Cart enforces stock availability per variant; reserved stock blocks others until TTL.
5. Checkout reservation expires; expired sessions re-quote price.
6. Money is integer rupees; rounding only at display.
7. Orders are immutable snapshots; catalog edits never affect history.
8. Order auto-splits into one `VendorOrder` per vendor; each ships independently.
9. Cancellation allowed only for items not yet shipped.
10. Return window: 7 days post-delivery (`getReturnableItems`).
11. COD restricted to couriers supporting COD (`CourierProvider.supportsCod`).
12. Payment retries capped at 3; intents expire in 15 minutes.
13. Refunds derived from `RefundTransaction`; payment status reflects `capturedAmount - refundedAmount`.
14. All state transitions go through `canTransition*` guards.
15. All domain mutations emit a typed event; audit/analytics/webhooks never duplicated in domain code.
16. Notifications honor user `NotificationPreferenceMatrix` per channel.
17. Suspended accounts lose mutation permissions but retain read of own historical data.
18. Coupons validate scope, min cart, usage cap, user cap, expiry, stacking rules.
19. Vendor cannot reply to another vendor's reviews.
20. Admin actions always audited (severity ≥ INFO).

---

## 26. Missing Features Report

**Implemented**
- Full RBAC, product/catalog, cart, checkout, orders, shipping, payments, notifications, audit, event bus, webhook outbox, invoices, vendor onboarding, CMS scaffolding, analytics scaffolding, tests for cart/splitting/coupons/filtering.

**Partially implemented**
- Real-time notifications (in-app only; no push/email/SMS transport).
- Vendor payouts/financials (UI only; settlement engine pending backend).
- Search (client-side debounced; needs full-text backend, e.g., OpenSearch).
- Analytics (envelopes emitted; aggregation engine pending).
- Bulk upload (CSV parsing; needs server-side validation pipeline).
- Fraud module (UI only).

**Not implemented**
- Real payment gateway adapters (Razorpay/Stripe).
- Real courier integrations (Shiprocket/Delhivery).
- KYC document verification workflow & storage.
- Server-side webhook signing/delivery.
- Multi-currency / multi-language.
- Recommendation engine (mock only).
- GST invoice e-signing.
- Backend persistence (Supabase/Postgres) — entire data layer is mock.

---

## 27. Future Service Boundaries (Spring Boot)

| Service | Responsibilities | Owns tables |
|---|---|---|
| **auth-service** | Signup/login/OTP/JWT/refresh | `users`, `user_roles`, `sessions` |
| **user-service** | Profiles, addresses, prefs | `profiles`, `addresses`, `notification_prefs` |
| **vendor-service** | Onboarding, KYC, vendor lifecycle | `vendors`, `kyc_documents` |
| **catalog-service** | Products, variants, categories, brands, reviews | `products`, `variants`, `categories`, `reviews` |
| **inventory-service** | Stock, reservations | `inventory`, `reservations` |
| **cart-service** | Cart, saved-for-later | `carts`, `cart_items` |
| **checkout-service** | Quote, reservation, place | `checkout_sessions` |
| **order-service** | Orders, vendor orders, returns | `orders`, `vendor_orders`, `order_items`, `returns` |
| **shipping-service** | Shipments, packages, legs, tracking | `shipments`, `packages`, `legs`, `tracking_events` |
| **payment-service** | Intents, attempts, refunds, gateway adapters | `payment_intents`, `payment_attempts`, `transactions`, `refunds` |
| **invoice-service** | GST invoices | `invoices` |
| **notification-service** | Email/SMS/Push fanout | `notifications`, `delivery_logs` |
| **audit-service** | Append-only audit | `audit_events` |
| **analytics-service** | Event ingestion / OLAP forwarding | event store |
| **webhook-service** | Outbox processor, signing, retry | `outbox_events`, `webhook_subscriptions` |

Inter-service comm: REST (sync) + Kafka/RabbitMQ (async) using the same `DomainEventEnvelope` shape.

---

## 28. Backend Implementation Guide

**Package layout (per service):**
```
com.marketplace.<service>
├── api          // controllers, DTO mapping
├── domain       // entities, value objects
├── service      // use cases / business rules
├── repository   // Spring Data JPA
├── event        // publishers + listeners (Kafka)
├── security     // role + ownership guards
└── config
```

**Database strategy:**
- PostgreSQL, schema-per-service (or DB-per-service when scaling).
- Flyway migrations. Append-only for `audit_events`, `outbox_events`.
- Outbox pattern for reliable event publishing (matches `webhookOutbox`).

**Security strategy:**
- Spring Security + JWT (RS256), refresh tokens rotated, blacklist on logout.
- Method-level `@PreAuthorize` mirroring frontend `Permission` enum (single source of truth in a shared `commerce-contracts` module).
- Ownership: `@PostAuthorize("returnObject.ownerId == authentication.principal.id or hasRole('ADMIN')")`.

**DTO strategy:** mirror frontend DTOs 1:1 in a shared `contracts` module; never expose entities.
**Repository strategy:** Spring Data JPA + Specification API for filters; QueryDSL for vendor/admin reports.

---

## 29. Scalability Review

**Strengths**
- Clean domain boundaries already mapped to microservices.
- Immutable snapshots enable archival & analytical replication.
- Event bus + outbox already enforce loose coupling.
- Pure-function selectors enable safe horizontal caching.

**Bottlenecks / risks**
- Frontend bundles will grow — code-split per role layout (Customer/Vendor/Admin).
- Lists need virtualization (TanStack Virtual) for vendor inventory / admin orders.
- Search will not scale on Postgres LIKE — externalize to OpenSearch.
- Reservation engine must be transactional under concurrency (use row-level locks or Redis).
- Payment idempotency keys mandatory to prevent double-capture.
- Cross-service consistency requires the outbox + Saga (orchestrated for checkout).

**Recommendations**
- CDN-cached catalog reads; write-through invalidation on `PRODUCT_PUBLISHED`.
- Redis for cart, sessions, rate limits, reservation locks.
- Kafka for domain events; idempotent consumers.
- Read replicas for analytics; CDC into warehouse.

---

## 30. Final System Assessment

| Dimension | Score | Notes |
|---|:-:|---|
| Architecture | **9 / 10** | Clear domain slicing, event-driven, backend-ready DTOs. |
| Maintainability | **9 / 10** | Pure selectors, single permission hook, no scattered role checks. |
| Scalability (current FE) | **7 / 10** | Needs code-splitting + list virtualization. |
| Backend Readiness | **9 / 10** | DTOs frozen; outbox + events match microservice patterns. |
| Security (frontend layer) | **7 / 10** | Strong client guards; depends on backend to enforce. |

**Strengths.** Disciplined domain modeling; immutable order snapshots; uniform API envelope; typed event bus with audit/analytics/webhook subscribers; RBAC + ownership as first-class primitives.

**Weaknesses.** No real persistence yet; no real payment/courier integration; analytics/aggregation surface is mocked; some vendor financial flows are UI-only.

**Risks.** Drift between frontend permissions and backend enforcement if not shared via a contracts module; reservation/payment idempotency under load; webhook delivery guarantees.

**Recommendations.**
1. Stand up `commerce-contracts` Java module mirroring `src/types/*`.
2. Implement Spring Boot services in the order: auth → user → catalog → inventory → cart → checkout → orders → payments → shipping → notifications → audit/webhook.
3. Replace mock APIs behind `src/api/*` one domain at a time — the UI requires zero changes.
4. Add E2E tests for checkout (happy path + reservation expiry + payment failure + multi-vendor split).
5. Introduce code-splitting, list virtualization, and Sentry/OpenTelemetry before launch.

— End of document —

---

## 31. Frontend Implementation Quality Audit

| Module | Classification | Issue | Impact | Recommended Fix | Priority |
|---|---|---|---|---|---|
| Auth | **Needs Hardening** | Tokens currently in `localStorage` via `tokenStorage`; refresh rotation absent. | XSS token theft; silent session loss. | Move to httpOnly cookies once backend lands; implement refresh rotation + blacklist. | Critical |
| Auth | **Needs Hardening** | OTP throttling client-only (`useOtpTimer`). | Abuse / SMS cost. | Server-side rate limit + CAPTCHA on resend. | High |
| RBAC | **Production Ready** | Centralized via `usePermissions`/`Can`. | — | Mirror enum in backend `commerce-contracts` module. | Medium |
| Profile | **Production Ready** | Validation in `lib/profileValidation`. | — | Add avatar virus scan when backend ready. | Low |
| Vendor Onboarding | **Needs Hardening** | KYC docs uploaded to mock; no MIME/size validation enforced server-side. | Compliance risk. | Backend: private bucket + signed URLs + AV scan. | Critical |
| Catalog | **Production Ready** | Pure selectors, ownership helpers, frozen DTOs. | — | Add server-side image transforms. | Medium |
| Inventory | **Needs Hardening** | Reservation TTL handled client-side only. | Oversell under concurrency. | Backend row-lock / Redis lock; idempotent reserve. | Critical |
| Cart | **Production Ready** | Per-vendor grouping, save-for-later, tested. | — | Persist cart server-side post-login merge. | Medium |
| Checkout | **Needs Hardening** | Pricing engine pure; idempotency key not yet wired into place-order call. | Double-charge risk. | Add idempotency key from `checkoutSession.id`. | Critical |
| Coupons | **Production Ready** | Stacking rules in `couponApi`. | — | Backend-enforce usage caps. | High |
| Orders | **Production Ready** | Immutable snapshots, state machine guarded. | — | Add Saga orchestrator for place-order on backend. | High |
| Shipping | **Production Ready** | Granular → canonical mapping; UI binds to canonical. | — | Plug courier adapters. | Medium |
| Payments | **Needs Hardening** | Gateway-agnostic but no real adapter; retry/expiry only mocked. | Cannot transact. | Implement Razorpay/Stripe adapter + webhook handler. | Critical |
| Returns/Refunds | **Production Ready** | 7-day window enforced via selector. | — | Backend must replicate window logic. | High |
| Notifications | **Partially Implemented** | In-app only; preferences UI exists, no transport. | Users miss critical events. | Backend: email/SMS/push fanout via notification-service. | High |
| Audit | **Production Ready** | Wildcard subscriber + append-only store. | — | DB-level immutability (revoke UPDATE/DELETE). | High |
| Analytics | **Needs Hardening** | Envelopes emitted; no aggregation. | Blind to KPIs. | Forward to GA4/warehouse; add dashboards. | Medium |
| Event Bus | **Production Ready** | Typed, bounded history, wildcard. | — | Mirror via Kafka topics on backend. | Medium |
| Webhook Outbox | **Needs Hardening** | In-memory buffer, no signing. | Lost events on reload. | Backend outbox table + HMAC signing + retry. | Critical |
| Invoices | **Needs Hardening** | Generated client-side; not legally signed. | GST compliance. | Backend PDF + IRN/e-invoice integration. | High |
| CMS | **Incomplete** | Forms scaffolded; no publish workflow. | Content cannot ship. | Add draft/publish state + image upload. | Medium |
| Vendor Financials | **Incomplete** | UI only; no settlement engine. | Vendors unpaid. | Backend ledger + payout-service. | Critical |
| Search | **Needs Refactor** | Client-side filtering on mock data. | Won't scale. | OpenSearch/Algolia + facet sync to URL. | High |
| Bulk Upload | **Needs Refactor** | CSV parsed client-side; no server validation. | Bad data risk. | Server-side job + row-level error report. | High |
| Fraud | **Incomplete** | UI placeholder. | No protection. | Risk-scoring service + manual review queue. | Medium |
| Lists (vendor/admin) | **Needs Refactor** | No virtualization. | Jank on >500 rows. | Adopt TanStack Virtual. | High |

---

## 32. Technical Debt Report

- **Duplicate logic:** `components/product/ProductCard.tsx` vs `components/shared/ProductCard.tsx` and `ProductSkeleton` duplicated in `product/` and `shared/`. Consolidate to one source.
- **Duplicate order pages:** `OrderDetailPage.tsx` (legacy) and `OrderDetailsPage.tsx` (new immutable snapshot) coexist. Delete legacy after route audit.
- **Legacy mocks:** `data/mock-orders.ts` + `mocks/mockOrders.ts` + `mocks/mockOrderRecords.ts` — three overlapping sources. Keep only `mockOrderRecords.ts`.
- **Dead code:** legacy `routes/ProtectedRoute.tsx` duplicates `components/auth/ProtectedRoute.tsx`. Pick one.
- **Unused stores (review):** `useStore.ts` aggregator vs per-domain stores — confirm necessity or remove.
- **Unused APIs (review):** legacy `orderApi.ts` overlaps `orderManagementApi.ts`.
- **Over-coupled:** `checkoutStore` reaches into `cartStore`, `couponStore`, `paymentStore`, `orderStore` — acceptable but warrants a `checkoutOrchestrator` facade to keep the store thin.
- **Circular-dependency risk:** subscribers import from both `api/*` and `store/*`; keep subscribers in `lib/subscribers/` and never let domain stores import subscribers.
- **Maintenance risks:** event payload shapes are typed but not versioned; bump `version` on every breaking change.
- **Test coverage gaps:** only cart/splitting/coupons/filtering covered. Add tests for payment retry, return window, shipment transition guards, RBAC ownership.
- **Design tokens:** all components compliant; periodic lint to prevent raw color regressions recommended.

---

## 33. Backend Readiness Validation

| Frontend Module | Backend Module | Readiness Score |
|---|---|:-:|
| Auth | auth-service | 80 |
| Profile | user-service | 90 |
| RBAC | shared contracts + auth-service | 90 |
| Vendor Onboarding | vendor-service | 75 |
| Catalog | catalog-service | 95 |
| Inventory | inventory-service | 70 |
| Cart | cart-service | 90 |
| Checkout | checkout-service (Saga) | 80 |
| Coupons | promotions-service | 85 |
| Orders | order-service | 95 |
| Shipping | shipping-service | 85 |
| Payments | payment-service | 60 |
| Returns/Refunds | order-service + payment-service | 80 |
| Notifications | notification-service | 65 |
| Invoices | invoice-service | 60 |
| Audit | audit-service | 90 |
| Analytics | analytics-service | 70 |
| Event Bus / Webhooks | outbox + kafka + webhook-service | 80 |

**Average:** ~80/100 — backend integration is structurally safe to start.

---

## 34. Database Normalization Review

**Missing entities to formalize backend-side:**
- `vendor_payouts`, `commission_rules`, `tax_rules` (GST slabs), `shipping_zones`, `pincode_serviceability`.
- `webhook_subscriptions`, `webhook_deliveries`.
- `kyc_documents` + `kyc_verifications`.
- `product_media` as separate table (currently embedded array).
- `attribute_definitions` + `category_attribute_links` (currently in TS config).
- `notification_templates` + `notification_deliveries`.
- `reservation_locks` (per inventory row).

**Missing relationships:**
- `coupon ↔ vendor/category/product` join tables for scoped coupons.
- `order_item ↔ shipment_item` to allow split shipments per order item.
- `refund ↔ return` explicit FK (currently inferred via `sourceId`).

**Potential duplication:**
- Address stored on user, cart, checkout snapshot, and order snapshot — intentional snapshotting, but ensure backend never normalizes away the snapshot.

**Indexing requirements:**
- `products(slug)`, `products(vendor_id, status)`, `products(category_id, status)`.
- `orders(user_id, created_at desc)`, `vendor_orders(vendor_id, status)`.
- `shipments(order_id)`, `tracking_events(shipment_id, at desc)`.
- `payment_intents(order_id)`, `refunds(order_id)`.
- `audit_events(entity_type, entity_id, at desc)`, `audit_events(actor_id)`.
- `outbox_events(processed_at, occurred_at)` partial index for unprocessed.
- `notifications(user_id, read_at, created_at desc)`.
- Full-text index (tsvector or external) on `products(title, description, brand)`.

---

## 35. Marketplace Business Readiness Review

**Customer Experience**
- Missing: order chat with vendor, gift wrap/messages, scheduled delivery, loyalty/wallet, EMI/BNPL, real-time delivery tracking on map, multi-currency.
- Missing validations: pincode serviceability blocker at PDP (not just checkout), age-gate for restricted goods.

**Vendor Experience**
- Missing: payout schedule view & reconciliation, dispute SLA timers, tax invoice download, returns RTO handling, shipping rate calculator, performance scorecard with penalties, ad campaign manager (UI exists, no engine).
- Missing tooling: bulk price/stock update, scheduled product publishing.

**Admin Experience**
- Missing: vendor performance penalties workflow, fraud rules engine, refund approval queue with SLA, content moderation queue, campaign approval, ticketing/SLA, GST report generation, financial reconciliation reports.

---

## 36. Deployment Readiness Review

**Environments**
- **Dev:** Vite dev server, mocked APIs, Lovable preview.
- **Staging:** Vercel/Cloudflare Pages + Spring Boot on AWS ECS/Fargate (1 replica) + Postgres + Redis + Kafka (managed) + S3 + Lovable Cloud project (staging).
- **Production:** CDN-fronted SPA (Cloudflare/CloudFront), multi-AZ ECS, Aurora Postgres, ElastiCache Redis, MSK Kafka, S3, KMS, WAF, autoscaling.

**CI/CD**
- GitHub Actions: lint → typecheck → vitest → build → preview deploy on PR; promote on merge.
- Backend: Maven build → unit/integration tests → containerize → push ECR → blue/green deploy.
- DB migrations via Flyway, gated by manual approval in prod.

**Environment Variables (frontend)**
- `VITE_API_BASE_URL`, `VITE_SENTRY_DSN`, `VITE_GA_ID`, `VITE_RAZORPAY_KEY` (publishable only).

**Secrets Management**
- Lovable secrets / AWS Secrets Manager. Never commit. Rotate quarterly. Separate per environment.

**Monitoring / Logging**
- Frontend: Sentry, Web Vitals, RUM, Lovable analytics.
- Backend: OpenTelemetry traces, Prometheus + Grafana, structured JSON logs to CloudWatch/Datadog, on-call alerts on SLO burn.
- Business dashboards: GMV, conversion, payment success rate, return rate, vendor SLA.

---

## 37. Testing Strategy

**Frontend Unit (Vitest)**
- Pure libs: `pricing`, `orderStatus`, `orderSelectors`, `permissions`, `accountStatus`, `catalogSelectors`, `productOwnership`, `validation`, `passwordStrength`.
- Stores: cart mutations, checkout reservation, order add/cancel reducers.

**Frontend Integration (RTL + MSW)**
- Login + role-aware redirect.
- PDP → cart → checkout → success.
- Vendor approves return → refund timeline updates.
- Permission gates render/deny correctly.

**Backend Unit (JUnit + Mockito)**
- State-machine guards, pricing parity with frontend, coupon engine, RBAC.

**Backend Integration (Testcontainers)**
- Postgres + Kafka + Redis: outbox publishing, payment idempotency, reservation concurrency, RLS enforcement.

**Critical business flows to test (E2E, Playwright)**
1. Multi-vendor checkout split.
2. Payment retry → success after 1 failure.
3. Reservation expiry mid-checkout.
4. Cancel-before-ship vs cancel-after-ship.
5. Return within / outside 7-day window.
6. Partial refund propagating to payment status.
7. Vendor cannot access other vendor's order.
8. Suspended vendor cannot mutate products.
9. Admin moderation publishes product to public catalog.
10. Notification preference matrix respected per channel.

---

## 38. Launch Checklist

**Must Have**
- [ ] Backend services for auth, catalog, inventory, cart, checkout, orders, payments, shipping live.
- [ ] Real payment gateway with webhook verification + idempotency.
- [ ] Real courier integration or manual shipping flow with tracking entry.
- [ ] RLS + role enforcement on every public table; `has_role()` function.
- [ ] httpOnly cookie sessions + refresh rotation + CSRF.
- [ ] KYC document storage in private bucket with signed URLs.
- [ ] GST-compliant invoices.
- [ ] Privacy policy, terms, refund policy, shipping policy live.
- [ ] Email/SMS transactional delivery (order, payment, shipment).
- [ ] Monitoring + alerting + on-call rotation.
- [ ] Backups + tested restore.
- [ ] Rate limiting on auth/checkout/search.
- [ ] PII encryption at rest.
- [ ] Load test for expected peak × 3.

**Should Have**
- [ ] Search via OpenSearch.
- [ ] List virtualization in vendor/admin.
- [ ] Vendor payout/reconciliation engine.
- [ ] Fraud rules engine MVP.
- [ ] Admin reporting dashboards.
- [ ] Webhook signing + delivery retries.
- [ ] Bulk upload server pipeline.

**Nice to Have (Post-Launch)**
- [ ] Loyalty/wallet, EMI/BNPL.
- [ ] Recommendation engine.
- [ ] Mobile apps (React Native).
- [ ] Multi-currency / multi-language.
- [ ] Live chat & helpdesk integration.
- [ ] Ad campaign engine for vendors.

---

## 39. Future Roadmap (6 Months)

| Month | Focus | Outcome |
|---|---|---|
| **1** | Spring Boot scaffolding, `commerce-contracts` module, auth + user + catalog services, Lovable Cloud DB schema + RLS, swap mock APIs for auth & catalog. | End users can register/login and browse from real DB. |
| **2** | Inventory + cart + checkout services with reservation locks; payment-service with Razorpay (or Stripe) adapter + webhook + idempotency; order-service with Saga. | End-to-end real transactions in staging. |
| **3** | Shipping-service with courier adapter (Shiprocket/Delhivery), tracking ingestion, returns/refunds wiring, notification-service (email + SMS via SES/MSG91). | Full order lifecycle live. |
| **4** | Vendor payouts/financials, GST invoice service, audit-service + webhook-service with signing, OpenSearch for search, list virtualization, observability stack. | Launch-ready beta. |
| **5** | Hardening: load test, security pen-test, fraud rules MVP, admin reporting, bulk upload pipeline, accessibility audit, SLO definitions, runbooks. | Production launch (soft). |
| **6** | Post-launch iteration: recommendations, ad campaigns, loyalty/wallet, EMI/BNPL, mobile app shell, A/B framework, growth analytics. | Marketplace flywheel. |

---

## 40. Final Executive Verdict

**Q: Can this architecture safely proceed to backend development?**
**Yes — with high confidence.** DTOs are frozen, domain boundaries are clean, events are typed, and the API envelope is uniform. A Spring Boot backend can be built domain-by-domain and swapped in behind `src/api/*` without UI changes.

**Q: Can this architecture scale to 10,000 users?**
**Yes**, on a single-region setup with Postgres + Redis + Kafka + CDN. Frontend code-splitting and list virtualization are the only must-do FE improvements.

**Q: Can this architecture scale to 100,000 users?**
**Yes, with investment.** Required: read replicas + CDC to warehouse, search externalized to OpenSearch, Redis-backed reservations & sessions, Kafka-driven event fanout, autoscaling stateless services, CDN-cached catalog, sharded notifications, and SLO/observability discipline. Microservice split per §27 enables independent scaling.

### Top 10 Risks
1. Payment idempotency not yet wired end-to-end → double-charge risk.
2. Inventory reservation concurrency unsolved in mock layer.
3. Webhook outbox is in-memory; no signed delivery.
4. KYC + invoice compliance gaps (GST e-invoice).
5. Notification transport (email/SMS/push) not implemented.
6. Search will not scale on Postgres LIKE.
7. Token storage in `localStorage` (XSS exposure) until cookie migration.
8. Vendor payouts/financials are UI-only — legal/financial risk.
9. List perf in vendor/admin without virtualization.
10. Permission enum drift between FE and BE unless shared module is enforced.

### Top 10 Strengths
1. Clean domain slicing already mapped to microservices.
2. Immutable order snapshots — audit & analytics safe.
3. Typed event bus + outbox — backend-pattern aligned.
4. Centralized RBAC via `usePermissions` + `<Can />`.
5. Pure pricing/status/selector engines — testable & reusable.
6. Uniform `ApiResponse` envelope — trivial backend swap.
7. Multi-vendor cart/order split is first-class.
8. Provider-agnostic payment + courier abstractions.
9. Wildcard audit subscriber — admin actions automatically logged.
10. Strong UX standards: skeletons, empty states, confirm dialogs, dark mode, semantic tokens.

### Final Recommendation
**Proceed to backend implementation immediately.** Begin with a shared `commerce-contracts` module to lock the permission enum and DTO shapes, then stand up auth → catalog → inventory → cart → checkout → payments in the order of §39. Treat the **Critical** items from §31 as P0 work to be done in parallel with backend wiring. With those addressed, this platform is structurally ready for a production marketplace launch within ~5 months.

— End of audit —
