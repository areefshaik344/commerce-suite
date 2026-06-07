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