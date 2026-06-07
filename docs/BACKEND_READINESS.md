## Phase 6.5 — Blocker Resolution (2026-06-07)

- **Order FSM** now includes `PENDING_PAYMENT`, `PARTIALLY_DELIVERED`, `COMPLETED` per `ORDER_FSM.md`. `CREATED`/`CLOSED` retained as transitional aliases.
- **DB FSM enforcement**: `fn_assert_child_transition` + `fn_rollup_parent_order` triggers (V011) guard vendor-order transitions and recompute parent rollup at the DB layer.
- **Idempotency**: `public.idempotency_keys` table + `com.commercesuite.common.idempotency.IdempotencyService.replayOrExecute(...)` ready for Phase 7 wiring on `POST /orders`, `/payments/intents`, `/refunds`, `/payouts`.
- **Coupon concurrency**: `CouponRepository.findByCodeForUpdate` + write-tx `resolve()` + `uq_coupon_usage_open` partial unique index.
- **Financial entities** (`Order`, `VendorOrder`, `OrderItem`, `OrderStatusHistory`, refund tables, return tables, `InventoryReservation`, `TrackingEvent`) are append-only: no `@SQLDelete`, `REVOKE DELETE` on `authenticated`.
- **Event safety**: `AfterCommitEventPublisher` defers domain-event publication until transaction commit. New `@EventListener`s MUST use `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- **Package cleanup**: orphan `com.commercesuite.users` deleted; canonical `com.commercesuite.user` retained.

## Phase 7 — Payments, Commission, Settlement, Payouts (2026-06-07)

- `payment_intents`, `payment_attempts`, `payment_transactions`,
  `commission_rules`/`commission_calculations`, `settlements`/`settlement_lines`,
  `payout_batches`/`vendor_payouts` shipped in migration V012 with
  FSM-enforcing triggers and `REVOKE DELETE` on all financial tables.
- Payment, Refund, Payout endpoints wired to `IdempotencyService` and
  `AfterCommitEventPublisher`.
- Commission engine supports PERCENTAGE, FIXED_AMOUNT and TIERED rules using
  `Math.floorDiv` + largest-remainder allocation; rules are snapshotted in
  `commission_calculations` for reproducibility.
- Settlement calculator hashes inputs (`calculation_hash`) so reruns are
  byte-equal across environments.
- `RefundProcessor` enforces `refundableRemainingPaise` (MoneySpec §4).
- Remaining blockers (5) and (6) above are now CLOSED.
## Phase 3 — Catalog (complete)
Categories, Brands, Products (FSM), Variants (paise), Media metadata, dynamic Attributes, Moderation, Reviews. Migration `V007`. Specification-based search. Public catalog endpoints permitted in `SecurityConfig`. See `docs/CATALOG_MODULE.md`.
# Backend Readiness Report

**Date:** June 7, 2026
**Scope:** Post-hardening assessment for Lovable Cloud / Supabase implementation.

**Update (post B-01..B-04 resolution):** All four blockers resolved via FROZEN specs:
`MONEY_SPEC.md`, `ORDER_FSM.md`, `PAYMENT_IDEMPOTENCY.md`, `RESERVATION_FSM.md`.
See `PRE_BACKEND_AUDIT_DELTA.md`. **Overall readiness raised to 9.0 / 10.**
**Verdict: SAFE TO START BACKEND.**

## Readiness Score

| Dimension                  | Score (10) | Notes |
|----------------------------|-----------:|-------|
| Domain modelling           | 9 | Full DTOs incl. payouts, GDPR, FSMs |
| State machines             | 9 | Order, Shipment, Return, Payment, Refund, Payout formalised |
| API contract clarity       | 8 | `ActorContext`, idempotency, request-id conventions defined |
| Security posture (frontend)| 7 | Token storage hardened; cookie swap pending backend |
| Auth & RBAC                | 7 | `user_roles` + `has_role` RPC contract documented |
| Audit & observability      | 7 | `requestId` everywhere; backend persistence pending |
| Payments                   | 8 | Lifecycle, retry, partial refunds, idempotency contract ready |
| Compliance (GDPR/DPDP)     | 6 | Contracts defined; flows not yet surfaced in UI |
| Inventory safety           | 7 | TTL + release reasons; server sweeper pending |
| **Overall**                | **7.5** | Safe to start backend implementation in parallel |

## Remaining blockers

1. Server-side image moderation pipeline
2. Bank account penny-drop verification (vendor onboarding)
3. Shipping label provider integration
4. Notification channel routing (email/SMS/push) provider selection
5. Pending-payment polling UI on `PaymentStatusPage`
6. Vendor payout console (admin) + visibility page (vendor)
7. Cart server-merge on login + price-drift detection

## Recommended build order

1. **Identity** — Supabase Auth (email + phone OTP), `profiles`, `user_roles`, `has_role()` RPC, RLS scaffolding.
2. **Catalog** — products, variants, categories, inventory (incl. `reserved` vs `available`).
3. **Cart + Reservations** — server-side reservation with TTL sweeper.
4. **Checkout + Orders** — order placement RPC, parent/child split, FSM enforcement.
5. **Payments** — intents, attempts, transactions, idempotency-key table, webhook ingress.
6. **Refunds + Returns** — refund FSM, partial refunds, return FSM with role guards.
7. **Shipping** — shipment FSM, label generation, tracking webhooks.
8. **Payouts** — ledger, settlement sweeper, payout processor.
9. **Notifications** — channel router, preferences, templates.
10. **Compliance** — GDPR deletion (30d grace) + data export workers.
11. **Admin governance** — moderation queues, dispute resolution, commission rules editor.

## Database impact

New tables (public schema): `profiles`, `user_roles`, `addresses`, `categories`, `products`, `product_variants`, `inventory`, `inventory_reservations`, `carts`, `cart_items`, `orders`, `child_orders`, `order_items`, `shipments`, `tracking_events`, `payments` (intents), `payment_attempts`, `transactions`, `refunds`, `returns`, `coupons`, `coupon_redemptions`, `commission_rules`, `tax_rules`, `settlement_ledger`, `settlements`, `payouts`, `notifications`, `notification_preferences`, `audit_log`, `idempotency_keys`, `account_deletion_requests`, `data_export_artifacts`, `webhook_outbox`.

Every public-schema table requires explicit `GRANT` to `authenticated` and `service_role` (anon only for fully public reads), then `ENABLE ROW LEVEL SECURITY`, then policies. See `docs/ARCHITECTURE.md` §Data layer.

## API contract impact

- Every mutating endpoint accepts `ActorContext` in the request body (`actor: { actorId, actorRole, requestId, idempotencyKey? }`).
- Every response echoes `X-Request-Id` in headers and the standard envelope `{ success, data, message, timestamp }`.
- Payment + refund + payout + order-placement endpoints REQUIRE `Idempotency-Key` header.
- All state-transition endpoints validate via the matching FSM (`assertTransition`) server-side.

## Security requirements

- Move refresh token to httpOnly, SameSite=strict, Secure cookie; keep access token in memory only.
- All RLS policies use `auth.uid()` + `public.has_role()` (security-definer, `search_path = public`).
- `user_roles` never readable by `anon`; only `authenticated` + `service_role`.
- Reservation release endpoint requires either ownership or `SYSTEM`/`ADMIN` role.
- Idempotency keys scoped per actor; replay returns cached response for 24h.
- Webhook outbox signed with HMAC, includes `requestId` for end-to-end correlation.
- GDPR deletion enforces a 30-day grace period; user can cancel during grace.
- Audit log is append-only (`INSERT`-only RLS, no `UPDATE`/`DELETE` for any role except `service_role`).
---

## Phase 1 Hardening (applied)

| Area                  | Change                                                                                                         |
|-----------------------|----------------------------------------------------------------------------------------------------------------|
| Entity base classes   | `BaseEntity` (UUID id + @Version), `AuditableEntity` (createdAt/updatedAt/createdBy/updatedBy + deletedAt).    |
| JPA auditing          | `@EnableJpaAuditing` + `AuditorAware<UUID>` backed by `ActorContextHolder`.                                    |
| Clock abstraction     | `Clock` bean (`Clock.systemUTC()`); all services use `Instant.now(clock)` — no raw `Instant.now()` in domain.  |
| JWT secret hardening  | `application.yml` no longer provides a default. `JwtTokenService` rejects null/blank/< 32-byte secrets.        |
| Soft delete           | `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")` on `User`, `Profile`, `Address`, `RefreshToken`.        |
| Migration V005        | Adds `created_by`/`updated_by` to user-facing tables; adds `deleted_at` to `profiles` and `refresh_tokens`.    |
| Integration tests     | `AuthControllerIT`, `RefreshTokenRotationIT`, `RBACPermissionIT` via Spring Boot + Testcontainers Postgres 16. |

---

## Phase 2 — Vendor module (applied)

| Area              | Change                                                                                                  |
|-------------------|---------------------------------------------------------------------------------------------------------|
| Migration         | `V006__vendor_module.sql` — 7 tables + 4 enums + grants (`anon` SELECT only on `vendor_profiles`).      |
| Entities          | `Vendor`, `VendorProfile`, `VendorApplication`, `VendorVerification`, `VendorBankAccount`, `VendorDocument`, `VendorStatusHistory`. |
| State machine     | `VendorStateMachine` enforces `VendorStatus` FSM; every transition recorded in `vendor_status_history`. |
| Permissions       | Added `MANAGE_VENDOR_PROFILE`, `VIEW_VENDOR_PAYOUTS`. Catalog updated for vendor role.                  |
| Services          | `VendorService`, `VendorApplicationService`, `VendorBankService`, `VendorVerificationService`, `VendorAdminService`. |
| Ownership         | `VendorOwnershipGuard` derives vendor from `ActorContext.userId()` on every self-service call.          |
| Controllers       | `VendorController` (10 endpoints), `AdminVendorController` (7 endpoints).                                |
| Events            | `VendorApplied/Approved/Rejected/Suspended/Reactivated/Deactivated` via `ApplicationEventPublisher`.    |
| Tests             | `VendorStateMachineTest`, `VendorApplicationIT`, `VendorApprovalIT`, `VendorOwnershipIT`, `VendorPermissionIT`. |
| Docs              | `docs/VENDOR_MODULE.md`.                                                                                |

Out of Phase 2 scope (still pending): file storage for documents, penny-drop bank verification, public storefront read API, vendor payouts.

## Phase 4 — Inventory (complete)
- Items, Movements, Reservations (FSM), Adjustments, Snapshots, LowStockRules.
- Oversell-safe via advisory lock + PESSIMISTIC_WRITE.
- Reservation sweeper (Spring `@Scheduled`).
- Migration: `V008__inventory_module.sql`. See `docs/INVENTORY_MODULE.md`.

## Phase 5 — Cart + Checkout Foundation (complete)
- Cart (`carts`, `cart_items`, `saved_for_later_items`) with one-active-cart-per-user partial unique index; guest-cart merge hook.
- Coupon engine (`coupons`, `coupon_usage`) with PERCENTAGE / FIXED_AMOUNT / FREE_SHIPPING types and active-window + per-user/global usage limits.
- `PricingEngine` — integer-paise only, BigDecimal HALF_UP for percentage/BPS, deterministic.
- `CheckoutStateMachine` (`CREATED → ADDRESS_SELECTED → SHIPPING_SELECTED → PAYMENT_SELECTED → READY_FOR_ORDER → CONVERTED`; CANCELLED/EXPIRED terminal).
- Inventory integration via `InventoryReservationService.reserveForCustomer(...)` + `releaseBySystem(...)`; reservations remain RESERVED until order COMMIT (RESERVATION_FSM.md).
- `CheckoutSweeperService` (Spring `@Scheduled`) expires stale sessions and releases reservations as `ABANDONED`.
- Idempotency on `POST /checkout/start` and `POST /checkout/cancel` via `Idempotency-Key` header (PAYMENT_IDEMPOTENCY.md key shape).
- Migration `V009__cart_checkout_module.sql`. Docs: `docs/CART_CHECKOUT_MODULE.md`.

## Phase 6 — Orders / Shipping / Returns / Refunds ✅
- Migration `V010__orders_shipping_returns_refunds.sql` adds 11 tables.
- Parent-Order / Vendor-Order split with deterministic rollup (`OrderRollupService`).
- Reservation commit wired via `InventoryReservationService.commitBySystem` (RESERVATION_FSM.md compliant).
- Customer / Vendor / Admin API surfaces under `/api/v1/orders`, `/api/v1/vendor/orders`, `/api/v1/shipments`, `/api/v1/returns`, `/api/v1/admin/*`.
- Immutable snapshots (address, vendor, product, pricing) stored as JSONB on creation.
- 5 FSMs (Order, VendorOrder, Shipment, Return, Refund) with server-enforced transitions.
- See `docs/ORDERS_SHIPPING_MODULE.md`.

---

## Phase 8.1 — Platform Foundation (Outbox + Auth Events + Audit + Notification Preferences)

**Status:** delivered. Resolves BLOCKER R-01 + HIGH risks R-02, R-03, R-04 from `PLATFORM_INTEGRATION_AUDIT.md`.

Modules:
- `common/outbox` — durable transactional outbox with scheduled dispatcher, exponential-backoff retry, dead-letter handling, SKIP-LOCKED batch claiming, and per-attempt diagnostic trail.
- `auth/event/AuthEvents` — 8 canonical Auth events published via `OutboxPublisher` (registered, logged-in, logged-out, password-changed/reset-requested/reset-completed, email-verified, refresh-token reuse detected).
- `common/audit/log` — append-only `audit_log` (REVOKE UPDATE/DELETE), `AuditService`, `AuditPublisher` subscribes to dispatched outbox events.
- `notifications/preferences` — per-user `(channel, category)` matrix, owner-scoped RLS, REST endpoints under `/api/v1/me/notification-preferences`.

Migration: `V013__platform_foundation.sql` (4 tables, 4 enums, 9 indexes, RLS + grants + append-only enforcement).
Tests: `OutboxPersistenceIT`, `OutboxDispatcherIT`, `OutboxRetryPolicyTest`, `AuditLogIT`, `NotificationPreferenceIT`, `AuthEventPublicationIT`.

Docs: `docs/OUTBOX_ARCHITECTURE.md`, `docs/AUDIT_FOUNDATION.md`, `docs/NOTIFICATION_FOUNDATION.md`.

Phase 8.1 does NOT implement notification delivery, analytics persistence, or webhook delivery — those are sprints 8.2 / 8.4 / 8.5 per `PHASE8_IMPLEMENTATION_BLUEPRINT.md`.

---

## Phase 8.2 — Notification Module

**Status:** delivered. Implements notification domain, templates, delivery tracking, preference-driven suppression, in-app inbox, and event-driven consumers — all on top of the Phase 8.1 durable outbox.

Modules:
- `notifications/domain` — `Notification`, `NotificationTemplate`, `NotificationDelivery`, `NotificationBatch`, `NotificationStatusHistory`, `NotificationStatus`.
- `notifications/service` — `NotificationStateMachine`, `TemplateRenderer`, `NotificationTemplateService`, `NotificationPreferenceEvaluator`, `NotificationService`, `NotificationDeliveryService`, `NotificationInboxService`.
- `notifications/delivery` — strategy interface + `InAppDeliveryStrategy` (real) and `Email/Sms/PushDeliveryStrategy` (stubs).
- `notifications/consumer/NotificationConsumer` — `OutboxDispatchEvent` listener mapping 16 business event types to template codes.
- `notifications/controller` — `NotificationController` (inbox), `NotificationTemplateController` (admin).
- `notifications/event/NotificationEvents` — 6 events published through the outbox.

Migration: `V014__notification_module.sql` (1 enum, 5 tables, indexes, grants, RLS, append-only history, 16 seed templates).
Tests: `NotificationStateMachineTest`, `TemplateRenderingTest`, `NotificationInboxIT`, `NotificationDeliveryIT`, `NotificationPreferenceSuppressionIT`, `NotificationConsumerIT`.
Docs: `docs/NOTIFICATION_MODULE.md`.

Phase 8.2 does NOT integrate real EMAIL/SMS/PUSH providers, webhooks, or analytics — those land in Sprints 8.4/8.5.

## Phase 8.3 — Audit Expansion (delivered)

- V015 migration: adds `audit_category` enum, `HIGH` severity, +3 tables (`audit_event_mappings`, `audit_retention_policies`, `audit_export_requests`); seeded with 32 event mappings + 11 retention policies.
- Registry-driven `AuditConsumer` replaces the hardcoded `AuditPublisher`.
- `AuditSearchService` (criteria-based JPA `Specification`).
- `AuditExportService` (PENDING request + outbox `audit.export_requested`).
- `AuditRetentionPolicyService` (per-category days; no purger yet).
- `AuditCoverageValidator` boots-up check + outbox `audit.coverage_warning`.
- Admin REST: `GET/POST /api/v1/admin/audit*` under `hasRole('ADMIN')`.
- Tests: AuditConsumerIT, AuditSearchIT, AuditRegistryTest, AuditCoverageValidatorTest, AuditExportIT.
- Audit remains append-only (REVOKE UPDATE/DELETE from authenticated).

## Phase 8.4 — Analytics & BI Foundation (delivered)

- V016 migration: 4 enums (`analytics_category`, `analytics_period`, `analytics_metric_type`, `dashboard_scope`), 5 tables (`analytics_events`, `analytics_metrics`, `analytics_aggregations`, `analytics_snapshots`, `dashboard_metrics`), full GRANT + RLS + append-only enforcement, seeded with 27-metric KPI catalog.
- `AnalyticsConsumer` subscribes to `OutboxDispatchEvent`; `AnalyticsService.record` runs in `REQUIRES_NEW` and the consumer swallows all exceptions — analytics CANNOT impact transactional flows (Orders, Payments, Inventory, Checkout).
- `AnalyticsEventClassifier` maps 22 outbox event types to category + KPI metric codes (single source of truth).
- `AnalyticsAggregator` rolls every classified event into DAY / WEEK / MONTH / LIFETIME buckets across ADMIN / VENDOR / CUSTOMER scopes and upserts `dashboard_metrics`.
- `KpiService` computes deterministic, division-by-zero-safe KPIs (checkout conversion, refund rate, AOV).
- `AnalyticsQueryService` powers time-series reads for dashboards.
- Admin + Vendor REST controllers: `/api/v1/admin/analytics/{overview,revenue,orders,vendors}` and `/api/v1/vendor/analytics/{overview,orders,revenue}`. Ownership enforced at controller AND RLS.
- 3 new domain events via durable outbox: `analytics.event_recorded`, `analytics.aggregation_completed`, `analytics.dashboard_updated`.
- `AnalyticsRetentionPolicy` declares retention SLAs (no purger yet).
- Tests: `AnalyticsConsumerIT`, `AnalyticsAggregationIT`, `KpiServiceIT`, `DashboardMetricsIT`, `AnalyticsQueryIT`, `AnalyticsPeriodTest`.
- Docs: `docs/ANALYTICS_MODULE.md`.

## Phase 8.5 — Webhooks & External Integration Foundation (delivered)

- Migration **V017** adds `webhook_endpoints`, `webhook_subscriptions`,
  `webhook_secrets`, `webhook_deliveries`, `webhook_attempts`,
  `webhook_status_history`, `external_integrations` (RLS + append-only).
- `WebhookConsumer` subscribes to `OutboxDispatchEvent`; deliveries are
  materialised per active subscription (`REQUIRES_NEW`, idempotent on
  `(subscription_id, source_event_id)`).
- `WebhookDispatcher` (scheduled) signs payloads (HMAC-SHA256 + ts +
  nonce), POSTs, runs `WebhookStateMachine` transitions, and persists
  per-attempt diagnostics.
- `WebhookRetryService` provides exponential backoff with cap.
- `WebhookSigner` + `WebhookSignatureVerifier` enforce replay
  protection and secret rotation (active + previous window).
- `ExternalIntegrationRegistry` exposes the `ExternalIntegrationProvider`
  abstraction; placeholders only.
- Admin API at `/api/v1/admin/webhooks/**`.
- Tests: `WebhookSignatureTest`, `WebhookStateMachineTest`,
  `WebhookRetryIT`, `WebhookSubscriptionIT`, `WebhookDeliveryIT`,
  `ExternalIntegrationRegistryTest`.
- Docs: `docs/WEBHOOK_MODULE.md`, `docs/PLATFORM_COMPLETION_REPORT.md`.

This closes the final Phase 8 sprint and resolves the webhook gap in
`PLATFORM_INTEGRATION_AUDIT.md`.

## Phase 9.5 — production hardening

- Containerization, k8s manifests, HPA/PDB/NetworkPolicy
- Pluggable `SecretProvider` (env / aws / vault / azure / gcp)
- `RateLimitFilter` + `RateLimitService` on auth, admin, webhook routes
- `SecurityHeadersFilter` (CSP, HSTS, XFO, COOP, CORP, Permissions-Policy)
- MFA module (TOTP + recovery codes); mandatory for ADMIN/FINANCE via `MfaEnforcement`
- `BusinessMetrics` + Micrometer Prometheus exporter
- Custom health indicators: outbox, webhooks, notifications + liveness/readiness probes
- `DeadLetterReplayService` + admin API for outbox/notification/webhook channels
- CI workflows: build, test, dependency scan, security scan, docker build
- Migration V018: `mfa_factors`, `mfa_recovery_codes`, `auth_lockouts`
