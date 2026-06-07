## Phase 6.5 — Blocker resolution (delivered)

- Migration `V011__phase6_5_blocker_resolution.sql`: enum extensions, FSM triggers, `idempotency_keys`, coupon partial-unique index + `used_count`, REVOKE DELETE on financial tables.
- `com.commercesuite.common.idempotency` package (entity, repository, service).
- `com.commercesuite.common.event.AfterCommitEventPublisher`.
- `CouponRepository.findByCodeForUpdate` + `CouponService.resolve` now write-transactional with pessimistic lock.
- Financial / audit entities stripped of `@SQLDelete` / `@SQLRestriction`.
- Orphan `users` package removed.
- Phase 7 (payments) MUST: (a) call `IdempotencyService.replayOrExecute` from every unsafe endpoint, (b) emit events through `AfterCommitEventPublisher`, (c) wire `pg_advisory_xact_lock` into `InventoryReservationService`, (d) implement largest-remainder allocation across vendor orders, (e) add `@RequiresPermission` to remaining controllers.
## Phase 3 — Catalog (delivered)
- Migration: `V007__catalog_module.sql`
- Package: `com.commercesuite.catalog.{entity,repository,service,dto,controller,event}`
- FSM: `ProductStateMachine` + `ProductStatus` enum
- Ownership: `ProductOwnershipGuard` (vendor-by-userId or admin bypass via `MODERATE_PRODUCTS`)
- Search: `ProductSpecifications` (keyword/category/brand/vendor/status). Public listing forces `status=APPROVED`.
- Money: `ProductVariant.pricePaise` (BIGINT, no floats).
- Tests: `ProductStateMachineTest`, `CategoryHierarchyIT`, `ProductOwnershipIT`, `ProductModerationIT`, `ProductReviewIT`, `CatalogSearchIT`.
# Commerce Suite — Spring Boot Implementation Plan

**Date:** June 7, 2026
**Stack:** Java 21 · Spring Boot 3.5.x · Gradle (Kotlin DSL) · PostgreSQL 15 · Flyway · JWT (jjwt 0.12) · MapStruct · Lombok · Testcontainers
**Sources of truth:** `docs/ARCHITECTURE.md`, `docs/BUSINESS_RULES.md`, `docs/DATABASE_DESIGN.md`, `docs/BACKEND_READINESS.md`

> This document is the authoritative backend blueprint. It does not contain
> code; it specifies exactly what must be built, in what order, with which
> contracts and guarantees. Every artefact references its frontend counterpart
> so the swap from the mock API layer is mechanical.

---

## 1. Implementation phases

Strict left-to-right ordering — later phases assume earlier phases are merged, tested, and deployed to `dev`.

| # | Phase | Goal | Exit criteria |
|---|-------|------|---------------|
| 1 | Auth + RBAC + Users | Login, refresh, roles, ownership, GDPR contract | `/auth/*` + `/me` green; `has_role()` used by every guard |
| 2 | Vendor | Onboarding, KYC, application review, public store | **Phase 2 complete:** Vendor FSM enforced, application apply/approve/reject/suspend/reactivate/deactivate E2E, documents + bank metadata stored, events published. Penny-drop integration deferred (placeholder fields present). See `docs/VENDOR_MODULE.md`. |
| 3 | Catalog | Categories, products, variants, images, reviews | Listing + PDP fully served from API; moderation queue live |
| 4 | Inventory | Stock buckets, reservations, low-stock alerts | TTL sweeper passing chaos test; alerts wired to notifications |
| 5 | Cart | Server cart + anonymous merge + price-drift | Cart merge contract verified; checkout entry triggers reservation |
| 6 | Checkout | Pricing, tax, coupon, shipping selection | `/checkout/quote` matches `lib/pricing.ts` golden tests |
| 7 | Orders | Parent/child split, FSM, cancel, accept window | Vendor accept/reject 24h auto-cancel cron green |
| 8 | Shipping | Shipments, tracking webhooks, labels | Carrier sandbox round-trip; label PDF generation |
| 9 | Payments + Refunds + Returns + Payouts | Intents, idempotency, partial refunds, settlement, payouts | Reconciliation report matches ledger; payout sandbox UTR returned |
|10 | Notifications + Audit + Analytics + GDPR | Channel router, audit triggers, event ingestion, deletion/export jobs | Audit log immutable; GDPR delete job purges within grace |

Each phase ends with: contract tests vs. the frontend mocks → green; OpenAPI published; staging deploy; smoke E2E.

---

## 2. Module design

Each module owns its package, Flyway migration set, REST surface, and integration tests. Modules expose **services** to peers, never repositories.

### 2.1 `auth`
- **Responsibilities:** registration, login (email/password, phone OTP), refresh-token rotation, password reset, session invalidation, JWT signing.
- **Dependencies:** `users`, `notifications` (email/OTP), `audit`.
- **Entities:** `User` (mirror of `auth.users`), `RefreshToken`, `OtpChallenge`, `PasswordResetToken`.
- **DTOs:** `LoginRequest`, `LoginResponse{accessToken,refreshToken,expiresIn,user}`, `RefreshRequest`, `OtpStartRequest`, `OtpVerifyRequest`, `PasswordResetStartRequest`, `PasswordResetConfirmRequest`.
- **Repositories:** `RefreshTokenRepository`, `OtpChallengeRepository`, `PasswordResetTokenRepository`.
- **Services:** `AuthService`, `TokenService`, `OtpService`, `PasswordResetService`.
- **Controllers:** `AuthController` (`/api/v1/auth/*`).
- **Events:** `UserRegistered`, `UserLoggedIn`, `SessionRevoked`.
- **Permissions:** all endpoints public except `/auth/logout` (authenticated).
- **Validation:** BR-AUTH-001/002/005; rate limits 5/min/IP login, 3/15min phone OTP.

### 2.2 `users`
- **Responsibilities:** profile, addresses, role assignment, account deletion + data export.
- **Dependencies:** `audit`, `notifications`.
- **Entities:** `Profile`, `UserRole`, `Address`, `AccountDeletionRequest`, `DataExportArtifact`.
- **DTOs:** `ProfileResponse`, `UpdateProfileRequest`, `AddressDto`, `RoleAssignmentRequest`, `AccountDeletionRequest`, `DataExportRequest`.
- **Services:** `ProfileService`, `RoleService`, `AddressService`, `GdprService`.
- **Controllers:** `MeController` (`/api/v1/me/*`), `AdminUserController` (`/api/v1/admin/users/*`).
- **Events:** `ProfileUpdated`, `AddressChanged`, `RoleGranted`, `AccountDeletionRequested`, `DataExportReady`.
- **Permissions:** owner-only on `/me/*`; `ROLE_ADMIN` on admin endpoints.
- **Validation:** BR-USER-001/002/003/004; max 10 addresses; deletion 30-day grace.

### 2.3 `vendor`
- **Responsibilities:** onboarding state machine, KYC, bank verification, vendor public profile, admin moderation.
- **Dependencies:** `users`, `audit`, third-party penny-drop provider.
- **Entities:** `Vendor`, `VendorApplication`, `VendorBankAccount`.
- **DTOs:** `VendorApplicationDraft`, `SubmitApplicationRequest`, `ReviewDecisionRequest`, `VendorPublicProfileResponse`, `BankAccountRequest`.
- **Services:** `VendorOnboardingService`, `VendorApplicationService`, `BankVerificationService`, `VendorProfileService`.
- **Controllers:** `VendorOnboardingController`, `VendorProfileController`, `AdminVendorController`.
- **Events:** `VendorApplicationSubmitted`, `VendorApproved`, `VendorRejected`, `VendorSuspended`, `BankAccountVerified`.
- **Permissions:** owner OR `ROLE_ADMIN`; suspended vendors blocked at service layer (CR-04 from BUSINESS_RULES).
- **Validation:** BR-VENDOR-001/002/003; PAN/GST/IFSC regex; penny-drop result required before approval.

### 2.4 `catalog`
- **Responsibilities:** categories, products, variants, images (with moderation), reviews.
- **Dependencies:** `vendor`, `inventory`, `audit`, image-moderation provider.
- **Entities:** `Category`, `Product`, `ProductVariant`, `ProductImage`, `Review`.
- **DTOs:** `ProductListItem`, `ProductDetailResponse`, `ProductCreateRequest`, `ProductUpdateRequest`, `VariantDto`, `ReviewRequest`, `ReviewResponse`, `VendorReviewReplyRequest`.
- **Services:** `CategoryService`, `ProductService`, `VariantService`, `ImageModerationService`, `ReviewService`, `SearchService`.
- **Controllers:** `CatalogPublicController`, `VendorCatalogController`, `AdminCatalogController`, `ReviewController`.
- **Events:** `ProductSubmitted`, `ProductApproved`, `ProductRejected`, `ProductArchived`, `ReviewPosted`, `ReviewReplied`.
- **Permissions:** public read for `APPROVED`; owning vendor edits drafts; `ROLE_ADMIN` approves.
- **Validation:** BR-CAT-001/002/003/004/005; unique `(vendor_id, sku)`; `mrp ≥ price`.

### 2.5 `inventory`
- **Responsibilities:** stock buckets, reservations with TTL, stock movements ledger, low-stock alerts.
- **Dependencies:** `catalog`, `notifications`.
- **Entities:** `Inventory`, `InventoryReservation`, `StockMovement`.
- **DTOs:** `InventorySnapshot`, `ReserveRequest`, `ReserveResponse`, `ReleaseReservationRequest`.
- **Services:** `InventoryService`, `ReservationService`, `ReservationSweepJob` (Spring `@Scheduled`), `LowStockAlertJob`.
- **Controllers:** internal only (service-to-service); admin read endpoints.
- **Events:** `StockReserved`, `StockReleased`, `StockDecremented`, `LowStockDetected`.
- **Permissions:** writes restricted to owning vendor + `ROLE_ADMIN`; service-to-service uses signed `SYSTEM` context.
- **Validation:** BR-INV-001..005; `reserved ≤ on_hand`; `available ≥ qty` for reservation; release reason ∈ enum.

### 2.6 `cart`
- **Responsibilities:** authenticated and anonymous carts, save-for-later, server merge on login, price-drift detection at checkout entry.
- **Dependencies:** `catalog`, `inventory`, `coupons`.
- **Entities:** `Cart`, `CartItem`.
- **DTOs:** `CartResponse`, `AddCartItemRequest`, `UpdateCartItemRequest`, `CartMergeRequest`, `PriceDriftReport`.
- **Services:** `CartService`, `CartMergeService`, `PriceDriftService`.
- **Controllers:** `CartController` (`/api/v1/cart`).
- **Events:** `CartItemAdded`, `CartItemRemoved`, `CartMerged`, `PriceDriftDetected`.
- **Permissions:** owner-only; anonymous via opaque cookie token; merge enforces ownership transfer.
- **Validation:** BR-CART-001..005; quantity bounds re-applied server-side.

### 2.7 `checkout`
- **Responsibilities:** quote calculation, address selection, shipping method per vendor, coupon application, pricing breakdown, reservation acquisition.
- **Dependencies:** `cart`, `inventory`, `shipping`, `tax`, `coupons`.
- **Entities:** none persisted (transient quote cached in Redis with TTL = 5 min).
- **DTOs:** `QuoteRequest`, `QuoteResponse{breakdown, vendorGroups, reservationId, expiresAt}`, `PlaceOrderRequest`.
- **Services:** `QuoteService`, `PricingService` (mirrors `lib/pricing.ts`), `TaxService`, `CouponEvaluator`, `ShippingEstimator`.
- **Controllers:** `CheckoutController`.
- **Events:** `CheckoutStarted`, `CheckoutAbandoned`, `OrderPlaced`.
- **Permissions:** authenticated owner only.
- **Validation:** BR-CHK-001..006; pricing invariant; coupon caps; COD eligibility.

### 2.8 `orders`
- **Responsibilities:** parent/child splitting, FSM enforcement, cancellation, vendor accept/reject window (24h), invoice generation.
- **Dependencies:** `checkout`, `payments`, `shipping`, `inventory`, `audit`.
- **Entities:** `Order`, `ChildOrder`, `OrderItem`, `OrderStatusTransition`, `Invoice`.
- **DTOs:** `PlaceOrderRequest`, `OrderResponse`, `ChildOrderResponse`, `CancelOrderRequest`, `VendorAcceptRequest`, `VendorRejectRequest`.
- **Services:** `OrderPlacementService`, `OrderQueryService`, `OrderTransitionService` (calls `FsmService.assertTransition`), `InvoiceService`, `VendorAcceptanceJob` (scheduled).
- **Controllers:** `CustomerOrderController`, `VendorOrderController`, `AdminOrderController`.
- **Events:** `OrderPlaced`, `OrderConfirmed`, `OrderCancelled`, `ChildOrderAccepted`, `ChildOrderRejected`, `OrderShipped`, `OrderDelivered`.
- **Permissions:** customer reads own; vendor reads own child; admin reads all. Cancel by customer only ≤ `CONFIRMED` (precedence rule CR-01).
- **Validation:** BR-ORD-001..006; FSM guards; idempotency on `POST /orders`.

### 2.9 `shipping`
- **Responsibilities:** shipment lifecycle, tracking ingestion via carrier webhooks, label generation, pincode serviceability.
- **Dependencies:** `orders`, carrier provider SDK.
- **Entities:** `Shipment`, `TrackingEvent`, `PincodeServiceability`.
- **DTOs:** `ShipmentResponse`, `CreateShipmentRequest`, `TrackingEventDto`, `PincodeCheckResponse`.
- **Services:** `ShipmentService`, `LabelService`, `TrackingIngestService`, `PincodeService`.
- **Controllers:** `VendorShipmentController`, `CustomerTrackingController`, `CarrierWebhookController` (HMAC verified).
- **Events:** `ShipmentCreated`, `LabelGenerated`, `TrackingUpdated`, `Delivered`, `DeliveryFailed`.
- **Permissions:** vendor on own; customer reads own tracking; webhook signature-verified.
- **Validation:** BR-SHP-001..004; shipment FSM; tracking ordering.

### 2.10 `payments`
- **Responsibilities:** intents, attempts, transactions, idempotency, gateway webhooks, retry policy.
- **Dependencies:** `orders`, gateway SDK (Razorpay/Stripe).
- **Entities:** `PaymentIntent`, `PaymentAttempt`, `Transaction`, `PaymentMethod`, `IdempotencyKey`.
- **DTOs:** `CreateIntentRequest`, `IntentResponse`, `ConfirmRequest`, `WebhookPayload`, `IdempotencyEnvelope`.
- **Services:** `PaymentIntentService`, `AttemptService`, `GatewayAdapter` (interface; one impl per gateway), `WebhookProcessor`, `IdempotencyService`.
- **Controllers:** `PaymentController`, `PaymentWebhookController`.
- **Events:** `PaymentAuthorized`, `PaymentCaptured`, `PaymentFailed`, `PaymentCancelled`.
- **Permissions:** owner-only on intent confirm/retry; webhook signature-verified (`SYSTEM`).
- **Validation:** BR-PAY-001..005; max 3 attempts; intent TTL 15 min; `Idempotency-Key` header required on POST.

### 2.11 `refunds` & `returns`
- **Responsibilities:** return RMA flow, refund processing (full/partial), refund FSM, source attribution.
- **Dependencies:** `orders`, `payments`, `shipping`, `notifications`.
- **Entities:** `Return`, `Refund`, `RefundLine` (future).
- **DTOs:** `CreateReturnRequest`, `ReturnDecisionRequest`, `RefundRequest`, `RefundResponse`.
- **Services:** `ReturnService`, `RefundService`, `RefundCapTrigger` (DB).
- **Controllers:** `CustomerReturnController`, `VendorReturnController`, `AdminRefundController`.
- **Events:** `ReturnRequested`, `ReturnApproved`, `ReturnRejected`, `ReturnPickedUp`, `RefundInitiated`, `RefundCompleted`, `RefundFailed`.
- **Permissions:** customer creates; vendor approves; admin/system completes refund.
- **Validation:** BR-RET-001..004, BR-REF-001..004; Σ refunds ≤ captured; FSM precedence over Return.REFUNDED (CR-03).

### 2.12 `payouts`
- **Responsibilities:** ledger accrual, weekly settlement, payout scheduling, bank file generation, reversal.
- **Dependencies:** `orders`, `refunds`, `commission_rules`, `tax_rules`.
- **Entities:** `SettlementLedgerEntry`, `Settlement`, `Payout`, `CommissionRule`.
- **DTOs:** `PayoutSummary`, `PayoutListItem`, `SettlementReport`.
- **Services:** `LedgerService`, `CommissionResolver`, `SettlementSweepJob` (weekly cron), `PayoutProcessor`, `BankAdapter`.
- **Controllers:** `VendorPayoutController`, `AdminPayoutController`.
- **Events:** `LedgerEntryRecorded`, `SettlementClosed`, `PayoutScheduled`, `PayoutPaid`, `PayoutFailed`, `PayoutReversed`.
- **Permissions:** vendor reads own; admin manages all.
- **Validation:** BR-PYT-001..006; resolution priority vendor > category > global; payout FSM.

### 2.13 `coupons` & `marketing`
- **Responsibilities:** coupon CRUD, redemption tracking, banners, CMS pages.
- **Entities:** `Coupon`, `CouponRedemption`, `Banner`, `CmsPage`.
- **DTOs:** `CouponDto`, `RedeemCouponRequest`, `BannerDto`, `CmsPageDto`.
- **Services:** `CouponService`, `CouponEvaluator` (shared with `checkout`), `CmsService`.
- **Permissions:** admin creates global; vendor creates vendor-scoped; public reads valid active coupons by code.

### 2.14 `notifications`
- **Responsibilities:** notification feed, preferences, channel routing, provider adapters.
- **Entities:** `Notification`, `NotificationPreference`, `WebhookOutbox`.
- **DTOs:** `NotificationDto`, `PreferenceDto`, `MarkReadRequest`.
- **Services:** `NotificationService`, `ChannelRouter`, `EmailAdapter`, `SmsAdapter`, `PushAdapter`, `WebhookDispatcherJob`.
- **Events:** consumes domain events; emits `NotificationDelivered`, `NotificationFailed`.
- **Permissions:** owner-only.
- **Validation:** BR-NOT-001..004; transactional always sent.

### 2.15 `audit`
- **Responsibilities:** append-only `audit_log`, request correlation, destructive-action coverage.
- **Entities:** `AuditLog`, `RequestAudit`.
- **Services:** `AuditService`, DB triggers for `vendors/products/orders/payments/refunds/payouts/user_roles/coupons`.
- **Permissions:** insert-only; admin-only read.
- **Validation:** BR-AUD-001..004; `request_id` mandatory.

### 2.16 `analytics`
- **Responsibilities:** event ingestion, aggregation views, PII scrub validator.
- **Entities:** `AnalyticsEvent` (partitioned by month), materialised views for GMV/AOV.
- **Services:** `AnalyticsIngestService`, `AggregationJob`.
- **Permissions:** vendor reads own aggregates; admin reads all.

---

## 3. Package structure

```
com.commercesuite
├── CommerceSuiteApplication
├── common
│   ├── api          (GlobalExceptionHandler, ApiResponseEnvelope, RequestIdFilter, IdempotencyFilter)
│   ├── audit        (AuditService, @Auditable, AuditAspect)
│   ├── events       (DomainEvent, DomainEventPublisher, OutboxRelay)
│   ├── fsm          (Fsm, TransitionRule, FsmRegistry, InvalidTransitionException)
│   ├── idempotency  (IdempotencyService, @Idempotent, IdempotencyKeyRepository)
│   ├── money        (Money, INR)
│   ├── pagination   (PageRequestSpec, PageResponse)
│   ├── security     (JwtService, JwtAuthFilter, SecurityConfig, ActorContext, @CurrentActor, OwnershipGuard, RoleHierarchy)
│   ├── time         (Clock bean, IstZone)
│   └── validation   (Validators, regex constants, RateLimiter)
│
├── auth             (entity, dto, service, controller, repository, mapper, event)
├── users
├── vendor
├── catalog
├── inventory
├── cart
├── checkout
├── orders
├── shipping
├── payments
│   ├── intent
│   ├── refund
│   └── gateway (Razorpay, Stripe adapters behind GatewayAdapter)
├── returns
├── payouts
│   ├── ledger
│   ├── settlement
│   └── disbursal
├── coupons
├── cms
├── notifications
│   └── channel (email, sms, push, inapp)
├── audit
├── analytics
└── jobs             (Scheduled jobs: ReservationSweep, VendorAcceptance, SettlementSweep, WebhookDispatcher, IdempotencyExpiry, AccountDeletion)

src/main/resources
├── application.yml
├── application-dev.yml
├── application-staging.yml
├── application-prod.yml
└── db/migration/V*__*.sql
```

Each module package contains: `domain` (entities + value objects), `dto`, `repository`, `service`, `mapper` (MapStruct), `controller`, `event`, `config`. Cross-module access goes through `service` only.

---

## 4. Security architecture

### 4.1 JWT strategy
- Access token: JWT (HS256 or RS256 in prod), TTL 15 min, claims `{sub, roles[], activeRole, sid, exp, iat, type:"access"}`.
- Refresh token: opaque random 256-bit, stored hashed (`sha256`) in `refresh_tokens`, TTL 7 days, **rotating** (one use → new token, old marked revoked).
- Signing key managed via secret `JWT_SIGNING_KEY` (prod: rotate every 90 days; keep `previous` key for grace verification).
- Refresh transport: httpOnly + Secure + SameSite=strict cookie. Access token returned in JSON body (frontend keeps in memory).

### 4.2 Refresh-token strategy
- Endpoint `POST /auth/refresh` reads cookie, validates hash, revokes old, issues new pair.
- Detect re-use: if a revoked token is presented, revoke **all** sessions for that user (token theft mitigation).
- Cross-tab sync remains client-side via `mh.sync` storage event.

### 4.3 RBAC implementation
- Spring Security `Role` mapping from `user_roles` table via `UserDetailsService`.
- Method-level `@PreAuthorize("hasRole('ADMIN')")` for admin endpoints; `@PreAuthorize("@ownershipGuard.canManage(#id, authentication)")` for owner-scoped writes.
- DB layer mirrors guards: every JPA repository write call goes through service that calls `OwnershipGuard.assertOwned(...)`.

### 4.4 Ownership implementation
- `OwnershipGuard` resolves `(resourceType, resourceId) → ownerId` through per-resource resolvers (`ProductOwnershipResolver`, `OrderOwnershipResolver`, …).
- Admin always passes; vendors pass only when `vendor.status = APPROVED` (resolves CR-04).
- Service layer additionally re-derives `actorId` from JWT — never trusts request-body actor (defense-in-depth).

### 4.5 Multi-role strategy
- `ActorContext` injected via `@CurrentActor` argument resolver; carries `{userId, roles[], activeRole, requestId, idempotencyKey?}`.
- `activeRole` selected from request header `X-Active-Role`; defaults to highest-privileged role in JWT.
- Permission checks evaluate `activeRole`; ownership checks consider full `roles[]`.

### 4.6 Session management
- `sessions` table tracks active refresh tokens, device fingerprint, IP, last used.
- Logout deletes current session; "logout everywhere" revokes all rows for `userId`.
- Idle timeout enforced by access-token expiry; absolute via refresh-token TTL.
- `SessionExpiredException` → HTTP 401 with `code: "SESSION_EXPIRED"` → frontend `authEvents.emitSessionExpired()`.

### 4.7 Other security controls
- HTTPS-only, HSTS, secure cookies.
- CORS allowlist driven by `app.cors.origins`.
- CSRF: enabled for cookie-authenticated state-changing endpoints (refresh, logout).
- Rate limiting via Bucket4j on `/auth/*`, `/payments/*`, `/checkout/*`.
- Argon2id password hashing.
- Field-level encryption (pgcrypto) for PAN, GSTIN, bank account numbers.
- HIBP check on password set/change.
- `X-Request-Id` filter generates UUID if missing; echoes on response and audit row.
- HMAC verification on every inbound webhook (gateway, carrier).
- All RPC functions `SECURITY DEFINER SET search_path = public`.

---

## 5. Database migration plan (Flyway)

Migrations in `src/main/resources/db/migration`, sequential `V###__name.sql`. Each table follows the `CREATE TABLE → GRANT → ENABLE RLS → CREATE POLICY` order.

| # | File | Contents |
|---|------|----------|
| V001 | `extensions` | `pgcrypto`, `uuid-ossp`, `pg_trgm`, `unaccent` |
| V002 | `enum_types` | All enums from §2 of DATABASE_DESIGN |
| V003 | `identity` | `profiles`, `user_roles`, `has_role()` function, triggers |
| V004 | `gdpr` | `account_deletion_requests`, `data_export_artifacts` |
| V005 | `vendors` | `vendors`, `vendor_applications`, `vendor_bank_accounts` |
| V006 | `catalog_categories` | `categories` |
| V007 | `catalog_products` | `products`, `product_variants`, `product_images`, FTS index |
| V008 | `reviews` | `reviews` |
| V009 | `inventory` | `inventory`, `inventory_reservations`, `stock_movements`, available view |
| V010 | `cart` | `carts`, `cart_items` |
| V011 | `tax_commission` | `tax_rules`, `commission_rules` |
| V012 | `coupons` | `coupons`, `coupon_redemptions` |
| V013 | `orders` | `orders`, `child_orders`, `order_items`, `order_status_transitions` |
| V014 | `shipping` | `shipments`, `tracking_events`, `pincode_serviceability` |
| V015 | `payments_catalog` | `payment_methods` seed |
| V016 | `payments_intents` | `payment_intents`, `payment_attempts`, `transactions`, `idempotency_keys` |
| V017 | `returns_refunds` | `returns`, `refunds`, `refund_lines` |
| V018 | `payouts` | `settlement_ledger`, `settlements`, `payouts`, `vw_payout_summary` |
| V019 | `notifications` | `notifications`, `notification_preferences`, `webhook_outbox` |
| V020 | `audit` | `audit_log` (append-only), `request_audit`, audit triggers |
| V021 | `cms` | `banners`, `cms_pages` |
| V022 | `policies` | RLS policies (separated for review clarity) |
| V023 | `triggers_fsm_guards` | Transition guards via `BEFORE UPDATE` triggers calling `assert_*_transition()` SQL fns |
| V024 | `seed_dev` | Demo accounts (dev only, gated by `application-dev.yml`) |

`R__refresh_views.sql` (repeatable) refreshes analytics materialised views nightly.

---

## 6. API implementation plan

Conventions: `/api/v1/...`. Response envelope `{ success, data, message, timestamp, requestId }`. Errors use RFC-7807 problem+json embedded in `message`. Pagination via `?page=X&pageSize=Y` returning `PageResponse`.

### 6.1 Auth
| Method | Path | Request | Response | Errors |
|--------|------|---------|----------|--------|
| POST | `/auth/register` | `RegisterRequest{email,password,name}` | `LoginResponse` | 409 email-taken, 422 weak-password |
| POST | `/auth/login` | `LoginRequest` | `LoginResponse` | 401 invalid, 423 locked |
| POST | `/auth/refresh` | cookie | `LoginResponse` (rotated) | 401 expired/revoked |
| POST | `/auth/logout` | — | 204 | — |
| POST | `/auth/otp/start` | `{phone}` | 204 | 429 throttled |
| POST | `/auth/otp/verify` | `{phone, code}` | `LoginResponse` | 401 wrong, 410 expired |
| POST | `/auth/password/reset/start` | `{email}` | 204 | 404 unknown (silent) |
| POST | `/auth/password/reset/confirm` | `{token,password}` | 204 | 410 expired, 422 weak |

### 6.2 Users / Me
| Method | Path | Request | Response | Errors |
|--------|------|---------|----------|--------|
| GET | `/me` | — | `ProfileResponse` | 401 |
| PATCH | `/me` | `UpdateProfileRequest` | `ProfileResponse` | 422 |
| GET/POST/PATCH/DELETE | `/me/addresses[/:id]` | `AddressDto` | `AddressDto[]` | 409 cap reached |
| POST | `/me/deletion-request` | `AccountDeletionRequest` | `AccountDeletionStatus` | 409 already requested |
| DELETE | `/me/deletion-request` | — | 204 | 410 grace passed |
| POST | `/me/data-export` | `DataExportRequest` | `DataExportArtifact` | 429 |

### 6.3 Vendor
| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/vendor/applications` | `VendorApplicationDraft` | `VendorApplicationResponse` |
| PATCH | `/vendor/applications/me` | `SubmitApplicationRequest` | `VendorApplicationResponse` |
| POST | `/vendor/bank-accounts` | `BankAccountRequest` | `BankAccountResponse` (triggers penny-drop) |
| GET | `/vendor/profile/me` | — | `VendorProfileResponse` |
| PATCH | `/vendor/profile/me` | `VendorProfileUpdate` | `VendorProfileResponse` |
| GET (public) | `/store/:slug` | — | `VendorPublicProfileResponse` |
| GET | `/admin/vendor-applications` | filters | `Page<VendorApplicationListItem>` |
| POST | `/admin/vendor-applications/:id/decision` | `ReviewDecisionRequest` | `VendorApplicationResponse` |
| POST | `/admin/vendors/:id/suspend` | `{reason}` | `VendorResponse` |

### 6.4 Catalog
| Method | Path | Notes |
|--------|------|-------|
| GET | `/products` | filters, sort, `?page` |
| GET | `/products/:idOrSlug` | PDP |
| GET | `/categories` | tree |
| POST | `/vendor/products` | draft create |
| PATCH | `/vendor/products/:id` | only owner |
| POST | `/vendor/products/:id/submit` | DRAFT → PENDING_REVIEW |
| POST | `/admin/products/:id/approve` `/reject` | moderation |
| POST | `/products/:id/reviews` | verified buyer only |
| POST | `/vendor/reviews/:id/reply` | owning vendor |

### 6.5 Inventory
| Method | Path |
|--------|------|
| GET | `/vendor/inventory` |
| PATCH | `/vendor/inventory/:variantId` |
| GET | `/admin/inventory/movements` |

### 6.6 Cart
| Method | Path |
|--------|------|
| GET | `/cart` |
| POST | `/cart/items` |
| PATCH | `/cart/items/:id` |
| DELETE | `/cart/items/:id` |
| POST | `/cart/items/:id/save-for-later` |
| POST | `/cart/merge` (post-login) |
| POST | `/cart/coupon` (apply) |

### 6.7 Checkout
| Method | Path |
|--------|------|
| POST | `/checkout/quote` (returns reservation + breakdown) |
| POST | `/checkout/place` (idempotent — `Idempotency-Key` required) |

### 6.8 Orders
| Method | Path |
|--------|------|
| GET | `/orders` (customer) |
| GET | `/orders/:id` |
| POST | `/orders/:id/cancel` |
| GET | `/vendor/orders` |
| POST | `/vendor/orders/:childId/accept` |
| POST | `/vendor/orders/:childId/reject` |
| GET | `/admin/orders` |

### 6.9 Shipping
| Method | Path |
|--------|------|
| POST | `/vendor/shipments` |
| GET | `/vendor/shipments/:id` |
| POST | `/vendor/shipments/:id/label` |
| GET | `/shipments/:trackingNumber/tracking` (public) |
| POST | `/webhooks/carriers/:carrier` (HMAC verified) |

### 6.10 Payments
| Method | Path |
|--------|------|
| POST | `/payments/intents` (`Idempotency-Key`) |
| GET | `/payments/intents/:id` |
| POST | `/payments/intents/:id/confirm` |
| POST | `/payments/intents/:id/retry` |
| POST | `/payments/intents/:id/cancel` |
| POST | `/webhooks/payments/:gateway` |

### 6.11 Refunds & Returns
| Method | Path |
|--------|------|
| POST | `/orders/:id/returns` |
| POST | `/vendor/returns/:id/approve` `/reject` |
| POST | `/admin/refunds` (`Idempotency-Key`) |
| GET | `/admin/refunds` |

### 6.12 Payouts
| Method | Path |
|--------|------|
| GET | `/vendor/payouts/summary` |
| GET | `/vendor/payouts` |
| GET | `/admin/settlements` |
| POST | `/admin/payouts/:id/hold` `/release` `/reverse` |

### 6.13 Notifications & CMS
| Method | Path |
|--------|------|
| GET | `/notifications` |
| POST | `/notifications/:id/read` |
| GET/PATCH | `/notifications/preferences` |
| GET | `/cms/banners` `/cms/pages/:slug` |
| POST | `/admin/cms/*` |

### 6.14 Common error scenarios

| HTTP | code | Meaning |
|------|------|---------|
| 400 | `VALIDATION_FAILED` | Bean Validation errors |
| 401 | `SESSION_EXPIRED` / `INVALID_CREDENTIALS` | |
| 403 | `PERMISSION_DENIED` / `OWNERSHIP_REQUIRED` | |
| 404 | `RESOURCE_NOT_FOUND` | |
| 409 | `CONFLICT` / `IDEMPOTENT_REPLAY` / `INVALID_TRANSITION` | |
| 410 | `EXPIRED` | tokens, intents |
| 422 | `BUSINESS_RULE_VIOLATION` | refund cap, qty cap, coupon |
| 423 | `LOCKED` | account locked |
| 429 | `RATE_LIMITED` | |
| 502 | `GATEWAY_ERROR` | upstream provider |

---

## 7. Event implementation plan

Publisher: Spring `ApplicationEventPublisher` → transactional `@EventListener(phase=AFTER_COMMIT)` consumers AND row in `webhook_outbox` for external delivery (transactional-outbox pattern).

| Event | Publisher | Internal subscribers | External webhook |
|-------|-----------|----------------------|------------------|
| `UserRegistered` | auth | notifications (welcome), analytics | — |
| `UserLoggedIn` | auth | audit, analytics | — |
| `VendorApplicationSubmitted` | vendor | notifications (admin), audit | — |
| `VendorApproved/Rejected/Suspended` | vendor | notifications, audit | partner |
| `ProductSubmitted/Approved/Rejected/Archived` | catalog | notifications, audit, search-indexer | — |
| `ReviewPosted/Replied` | catalog | notifications, analytics | — |
| `StockReserved/Released/Decremented/LowStockDetected` | inventory | notifications (vendor low-stock), analytics | — |
| `CartMerged/PriceDriftDetected` | cart | analytics | — |
| `CheckoutStarted/Abandoned` | checkout | analytics, inventory (release on abandon) | — |
| `OrderPlaced` | orders | payments (intent link), notifications, analytics, audit | partner |
| `OrderConfirmed/Cancelled` | orders | inventory, notifications, audit | partner |
| `ChildOrderAccepted/Rejected` | orders | shipping (auto-create shipment), notifications, payouts (ledger) | partner |
| `OrderShipped/Delivered` | shipping → orders | notifications, payouts (ledger eligibility), analytics | partner |
| `PaymentAuthorized/Captured/Failed/Cancelled` | payments | orders (status drive), inventory (decrement), notifications, audit | partner |
| `RefundInitiated/Completed/Failed` | refunds | orders, returns, payouts (adjustment), notifications, audit | partner |
| `ReturnRequested/Approved/Rejected/PickedUp` | returns | notifications, shipping (reverse), audit | partner |
| `LedgerEntryRecorded` | payouts | analytics | — |
| `SettlementClosed` | payouts | notifications, audit | partner |
| `PayoutScheduled/Paid/Failed/Reversed` | payouts | notifications, audit | partner |
| `NotificationDelivered/Failed` | notifications | analytics, audit | — |
| `AccountDeletionRequested/Completed` | users | audit, notifications | — |

Outbox dispatcher (`WebhookDispatcherJob`) polls every 5s, signs payload (HMAC-SHA256), retries with exponential backoff up to 8 attempts, then `DEAD_LETTERED`.

---

## 8. Testing strategy

| Layer | Framework | Coverage target |
|-------|-----------|----------------|
| Unit (services, FSMs, pricing, coupon eval, commission resolver, idempotency) | JUnit 5 + AssertJ + Mockito | ≥90% on `service/` + `lib/` packages |
| Slice (Web/JPA/JSON) | `@WebMvcTest`, `@DataJpaTest` | each controller, each repository custom query |
| Integration (DB + Flyway + Spring) | Testcontainers (Postgres 15) | full request-response with real DB |
| Security | `spring-security-test` + custom JWT helpers | every endpoint × every role matrix |
| Contract (frontend ↔ backend) | Pact (consumer in FE repo) + Spring Cloud Contract verifier | one verifier per controller |
| FSM property | jqwik | random walks vs. `assertTransition` |
| Reconciliation | scenario suite | refund cap, payout ledger sum, inventory invariants |
| Performance | Gatling | login, product list, checkout, payment confirm; p95 budgets |
| Chaos | Toxiproxy | gateway latency, DB failover, webhook flood |
| End-to-end | Playwright (FE) against staging | smoke + critical paths nightly |

Mandatory negative tests: invalid FSM transition, expired idempotency replay, ownership escape, refund > captured, coupon over-redemption, reservation exhaustion, webhook signature mismatch, expired JWT, replayed refresh token, RLS bypass attempt.

CI gates: build + unit + slice on every PR; integration + security on merge to `main`; performance + contract nightly.

---

## 9. Production deployment plan

### 9.1 Environments

| Env | Purpose | Data | URL pattern |
|-----|---------|------|-------------|
| dev | Active development | synthetic seed | `api.dev.commerce-suite.example` |
| staging | Pre-prod parity | anonymised prod snapshot weekly | `api.staging.…` |
| prod | Live | real | `api.…` |

### 9.2 Environment variables (per env)

```
SPRING_PROFILES_ACTIVE
DATABASE_URL                       (jdbc:postgresql://…)
DATABASE_USERNAME / DATABASE_PASSWORD
REDIS_URL                          (cache + rate limit + quote)
JWT_SIGNING_KEY / JWT_PREVIOUS_KEY
REFRESH_COOKIE_DOMAIN
CORS_ORIGINS
APP_BASE_URL
SMS_PROVIDER_KEY / EMAIL_PROVIDER_KEY / PUSH_PROVIDER_KEY
PAYMENT_GATEWAY_KEY / PAYMENT_WEBHOOK_SECRET
CARRIER_API_KEY / CARRIER_WEBHOOK_SECRET
BANK_API_KEY / BANK_WEBHOOK_SECRET
OBJECT_STORAGE_BUCKET / OBJECT_STORAGE_KEY / OBJECT_STORAGE_SECRET
OTEL_EXPORTER_OTLP_ENDPOINT
SENTRY_DSN
ENCRYPTION_KEY                     (pgcrypto-compatible)
FEATURE_FLAGS_URL
```

### 9.3 Secrets management
- AWS Secrets Manager (or Lovable Cloud secret store) per env.
- Rotation: JWT signing key 90d; payment & carrier keys per provider policy; DB passwords 180d.
- No secret in repo; CI injects via OIDC.

### 9.4 Logging
- JSON logs to stdout, shipped to OpenSearch via Fluent Bit.
- Required fields: `timestamp, level, logger, message, request_id, user_id?, actor_role?, span_id, trace_id`.
- Log level: `INFO` default, `WARN` in prod for noisy packages, `DEBUG` gated by feature flag.
- Never log: passwords, tokens, full card numbers, OTPs, raw webhook bodies (log hash + provider id only).

### 9.5 Monitoring
- Metrics: Micrometer → Prometheus. Dashboards: latency p50/p95/p99 per endpoint, error rate, gateway success rate, webhook lag, reservation TTL backlog, payout queue depth, FSM rejection counts.
- Tracing: OpenTelemetry SDK, span per DB query + per outbound call.
- Health: `/actuator/health`, `/actuator/info`, custom `/health/dependencies` (DB, Redis, gateway ping).
- Alerts (PagerDuty): p95 > SLO 5m, 5xx > 1% 5m, outbox DLQ growth, reservation sweep failure, settlement-job failure, refund cap violation attempts spike (security signal).

### 9.6 Backup strategy
- Postgres: daily base backup + WAL streaming; PITR window 14d in prod, 7d in staging.
- Object storage: versioning on; cross-region replication for prod.
- Secrets: vault snapshot weekly.
- Restore drill quarterly; RTO 1h, RPO 5m for prod.

### 9.7 Deployment
- Containerised (Distroless Java 21). Helm charts to k8s.
- Blue/green for the API; rolling for jobs.
- Flyway runs as init-container, fail-fast on validation error.
- Feature flags via Unleash for risky cutovers (e.g. switch from mock to real payment gateway).

---

## 10. Final execution roadmap (week-by-week)

Assumes 2 backend engineers + 1 SRE + 0.5 QA. Each week ends green on CI, deploys to staging, and ships an OpenAPI delta.

| Week | Focus | Deliverables |
|------|-------|--------------|
| 1 | Scaffold & platform | Gradle multi-module skeleton; CI; Postgres + Redis in Docker; Flyway V001–V003; security filter chain; `ApiResponseEnvelope`; `RequestIdFilter`; OpenAPI gen |
| 2 | Auth | Register/login/refresh/logout; JWT + refresh rotation; password reset; rate limits; auth contract tests vs FE mocks |
| 3 | Users + GDPR contract + RBAC end-to-end | `/me`, addresses, role grants, account deletion (request only), data export stub, `OwnershipGuard`, `@CurrentActor` |
| 4 | Vendor onboarding | Applications, KYC fields, bank-account form, penny-drop adapter (sandbox), admin review |
| 5 | Catalog (read) | Categories, products, variants, images, FTS; public PDP + listing API; vendor product CRUD draft path |
| 6 | Catalog (moderation) + reviews | Submit/approve/reject products; review post + verified-buyer check; vendor review reply |
| 7 | Inventory | Buckets, movements ledger, low-stock alerts, reservation create/release; TTL sweep job + chaos test |
| 8 | Cart | Cart CRUD, save-for-later, anonymous → auth merge, coupon attach; price-drift detection at checkout entry |
| 9 | Checkout | Quote engine, tax service (`effectiveFrom` resolution), coupon evaluator, shipping estimator; reservation acquisition |
| 10 | Orders | Place order (idempotent), parent/child split, FSM enforcement, cancel, vendor accept/reject, invoice gen, 24h auto-cancel job |
| 11 | Shipping | Shipment lifecycle, label adapter (sandbox), carrier webhook ingest, tracking timeline |
| 12 | Payments — intents | Intent CRUD, attempts, idempotency table, gateway adapter (Razorpay sandbox), webhook |
| 13 | Payments — retry & refunds | Retry policy, refund FSM, partial-refund cap trigger, refund webhook |
| 14 | Returns | Return RMA flow, vendor approval, reverse-pickup integration with shipping |
| 15 | Payouts — ledger & settlement | Ledger entries on `Delivered`, commission resolver, weekly settlement sweep job |
| 16 | Payouts — disbursal | Payout FSM, bank file generation, admin hold/release/reverse, vendor visibility API |
| 17 | Notifications | Channel router, email/SMS/push adapters, preferences, transactional vs marketing classification |
| 18 | Audit + Analytics + GDPR completion | Audit triggers + `request_audit`; analytics ingestion + materialised views; GDPR delete worker + export packager |
| 19 | Hardening | Performance test pass, chaos pass, security pen-test fixes, RLS audit, dependency scan |
| 20 | Cutover | Feature-flag switch FE from mock to real API per module; staging sign-off; blue/green to prod; monitor 72h |

**Buffer policy:** Weeks 21–22 reserved for spillover and post-cutover stabilisation. Do not start new modules in buffer weeks.

---

## 11. Cross-reference

- Architecture decisions: `docs/ARCHITECTURE.md`
- Business rule IDs referenced above: `docs/BUSINESS_RULES.md`
- Schema, indexes, RLS template: `docs/DATABASE_DESIGN.md`
- Readiness scoring & blockers: `docs/BACKEND_READINESS.md`
- FSM source (transition tables to mirror server-side): `src/lib/fsm.ts`
- Actor/idempotency/request-id contracts: `src/types/actor.ts`, `src/lib/idempotency.ts`, `src/lib/requestId.ts`
---

## Phase 1 Hardening Addendum

The original Phase 1 deliverables are extended (no new business features) with the following baseline that every later phase MUST adopt:

1. **Persistence base classes** — every JPA entity extends `BaseEntity` (UUID id, `@Version`) and, when audit columns are required, `AuditableEntity` (audit + soft-delete). Phase ≥ 2 entities are not allowed to redeclare these fields.
2. **JPA auditing** — `@EnableJpaAuditing` is bootstrapped by `JpaAuditingConfig`; `AuditorAware<UUID>` reads from `ActorContextHolder.current()`. Services MUST NOT set `createdAt`/`updatedAt`/`createdBy`/`updatedBy` by hand.
3. **Clock injection** — domain services MUST inject `java.time.Clock` and call `Instant.now(clock)`. Direct `Instant.now()` is forbidden in domain code (allowed only in logging/diagnostic envelopes).
4. **Secrets** — `JWT_SECRET` is mandatory at startup; no default. The same rule applies to every later secret (payment, webhook signing, S3 keys).
5. **Soft delete strategy** — for any entity holding user data, declare `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")`. Hard deletes are reserved for purge jobs and require an explicit native query.
6. **Test baseline** — every controller phase MUST ship at minimum: one happy-path controller IT, one security/RBAC IT, and one negative-path validation IT, all running on Testcontainers Postgres.

## Phase 4 — Inventory ✅
Implemented `com.commercesuite.inventory.*` with entities, repositories, services (`InventoryService`, `InventoryReservationService`, `InventoryAdjustmentService`, `InventorySnapshotService`, `InventoryLowStockService`, `InventoryStateMachine`, `InventoryAllocator`, `InventoryOwnershipGuard`), controllers (`InventoryController`, `AdminInventoryController`), events, `InventoryReservationSweeper`, Flyway `V008`, and Testcontainers ITs. Aligns with `RESERVATION_FSM.md`.

## Phase 5 — Cart + Checkout Foundation ✅
Implemented `com.commercesuite.cart.*`, `com.commercesuite.coupon.*`, and `com.commercesuite.checkout.*`. Entities (Cart, CartItem, SavedForLaterItem, Coupon, CouponUsage, CheckoutSession, CheckoutReservationLink), repositories, DTOs, events, services (`CartService`, `CartValidationService`, `CouponService`, `PricingEngine`, `CheckoutService`, `CheckoutReservationService`, `CheckoutStateMachine`, `CheckoutSweeperService`), controllers (`CartController`, `CouponController`, `CheckoutController`), Flyway `V009`, and tests (`CheckoutStateMachineTest`, `PricingEngineTest`, `CheckoutIT`). Reservation integration extends `InventoryReservationService` additively without redesigning Phase 4. Fully compliant with `MONEY_SPEC.md`, `RESERVATION_FSM.md`, and `PAYMENT_IDEMPOTENCY.md`.

## Phase 6 — Delivered
- Packages: `orders`, `shipping`, `returns`, `refunds`.
- 13 entities, 11 repositories, 5 state machines, 8 services, 6 controllers.
- 4 event groups (`OrderEvents`, `ShippingEvents`, `ReturnEvents`, `RefundEvents`).
- Minimal Phase-4 extension: added `InventoryReservationService.commitBySystem` to satisfy `RESERVED → COMMITTED` for order creation.
- Permissions reused (`PLACE_ORDER`, `MANAGE_VENDOR_ORDERS`, `MANAGE_VENDOR_RETURNS`, `MANAGE_PAYOUTS`).
