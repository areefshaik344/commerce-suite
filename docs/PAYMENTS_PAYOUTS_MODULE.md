# Phase 7 — Payments, Refund Processing, Commission, Settlement, Payouts

**Status:** IMPLEMENTED. Source-of-truth specs: `MONEY_SPEC.md`,
`PAYMENT_IDEMPOTENCY.md`, `ORDER_FSM.md`, `BLOCKER_RESOLUTION_REPORT.md`.

## 1. Payment FSM
`CREATED → AUTHORIZED → CAPTURED → {PARTIALLY_REFUNDED → REFUNDED}`.
Terminal: `FAILED`, `CANCELLED`, `REFUNDED`. `FAILED` payments require a new
intent (no in-place retry from terminal). DB enforcement via
`fn_assert_payment_transition` trigger (V012). Application FSM in
`PaymentStateMachine`; mirror of trigger.

## 2. Payment idempotency
Per `PAYMENT_IDEMPOTENCY.md`. `POST /api/v1/payments/intents` requires the
`Idempotency-Key` header and is wired through `IdempotencyService`
(`(actor, endpoint, key)` UNIQUE, 24h TTL, SHA-256 payload hash, conflict on
payload divergence, replay header `Idempotent-Replay`).
`PaymentIntent.idempotency_key` is also UNIQUE per `(customer, key)` as a
defence in depth.

## 3. Payment attempts & ledger
- `payment_attempts` — one row per gateway call; carries request/response JSON,
  failure code/message, attempt_number unique per intent.
- `payment_transactions` — immutable ledger; types `AUTHORIZATION`, `CAPTURE`,
  `REFUND`, `REVERSAL`, `ADJUSTMENT`. Append-only (REVOKE DELETE).
- `payment_status_history` — FSM audit trail.

## 4. Refund processing
`RefundProcessor.process(refundId, amountPaise, actor)`:
1. Validates `amount > 0` and `cumulativeCompleted + amount ≤ request.amount`.
2. Resolves the captured `PaymentIntent` for the order.
3. Advances Refund FSM `PENDING → APPROVED → PROCESSING`.
4. `PaymentService.applyRefund` records a `REFUND` `PaymentTransaction`, bumps
   `refunded_paise`, and rolls FSM to `PARTIALLY_REFUNDED` / `REFUNDED`.
5. On full satisfaction, advances Refund to `COMPLETED` and emits
   `RefundCompletedEvent`.
Supports multiple partial refund transactions per RefundRequest.
Remaining refundable balance: `PaymentIntent.refundableRemainingPaise()`.

## 5. Commission engine
Tables: `commission_rules`, `commission_calculations`.
Types: `PERCENTAGE` (`floor(taxable * bps / 10000)`), `FIXED_AMOUNT`, `TIERED`
(first `up_to_paise` match wins). `min_fee_paise` / `max_fee_paise` clamp.
Scopes: `GLOBAL`, `VENDOR`, `CATEGORY`. `CommissionRuleService.resolveFor`
returns VENDOR scope before GLOBAL within active effective window.
Snapshots: `CommissionCalculation` is written once per `vendor_order_id`
with the resolved rule frozen as JSON for reproducibility.

## 6. Settlement engine
FSM: `PENDING → CALCULATED → LOCKED → PAID` (DB trigger
`fn_assert_settlement_transition`).
`SettlementCalculator.compute(vendorId, periodStart, periodEnd, settlementId)`:
- Iterates `vendor_orders` in `[periodStart, periodEnd)` ordered by `id`.
- Per line: `gross = vendor_order.total`, `refund = Σ refunds in
  {APPROVED, PROCESSING, COMPLETED}`, `commission = CommissionCalculator(rule,
  gross − refund)`, `net = gross − refund − commission − platformFee`.
- Writes `settlement_lines` and produces a deterministic
  `calculation_hash = SHA-256(line stream)` for reproducibility.

Deterministic guarantees: integer-paise math, sorted by `id`, single rule
resolved at calculation time, snapshot persisted.

## 7. Payout engine
FSM: `CREATED → PROCESSING → COMPLETED` (or `FAILED → PROCESSING|CANCELLED`)
with DB trigger `fn_assert_payout_transition`.
`PayoutBatchService.generate(notes, actor)` collects every LOCKED settlement
without a payout, sorted by id, and creates one `VendorPayout` per settlement.
`PayoutService.markCompleted` advances the payout, sets bank reference, and
invokes `SettlementService.markPaid(settlementId, payoutId)` which transitions
the settlement to `PAID` and emits `SettlementPaidEvent`.

## 8. Ownership
- `PaymentOwnershipGuard` — customer owns intent; admin / finance override.
- `SettlementOwnershipGuard` — vendor owns own settlements; admin override.
- `PayoutOwnershipGuard` — vendor read; admin/finance manage.

## 9. API contracts
Customer payments:
- `POST /api/v1/payments/intents` (idempotent) → `201 Created`
- `GET  /api/v1/payments`           list mine
- `GET  /api/v1/payments/{id}`      get one
- `POST /api/v1/payments/{id}/retry`   produce new attempt
- `POST /api/v1/payments/{id}/confirm` sandbox auth+capture
- `POST /api/v1/payments/{id}/cancel`

Vendor:
- `GET /api/v1/vendor/settlements`
- `GET /api/v1/vendor/payouts`
- `GET /api/v1/vendor/payouts/{id}`

Admin:
- `GET  /api/v1/admin/payments`, `GET …/transactions`
- `GET  /api/v1/admin/settlements`
- `POST /api/v1/admin/settlements/calculate`
- `POST /api/v1/admin/settlements/{id}/lock`
- `POST /api/v1/admin/payouts/generate`
- `POST /api/v1/admin/payouts/{id}/process|complete|fail`

## 10. Events
All events publish via `AfterCommitEventPublisher`:
`PaymentCreatedEvent`, `PaymentAuthorizedEvent`, `PaymentCapturedEvent`,
`PaymentFailedEvent`, `PaymentCancelledEvent`, `PaymentRefundedEvent`,
`PaymentStateChangedEvent`, `RefundCompletedEvent`,
`CommissionCalculatedEvent`, `CommissionRuleChangedEvent`,
`SettlementCalculatedEvent`, `SettlementLockedEvent`, `SettlementPaidEvent`,
`SettlementStateChangedEvent`, `PayoutCreatedEvent`, `PayoutCompletedEvent`,
`PayoutFailedEvent`, `PayoutStateChangedEvent`, `PayoutBatchCreatedEvent`,
`PayoutBatchCompletedEvent`.

## 11. Database tables (V012)
`payment_intents`, `payment_attempts`, `payment_transactions`,
`payment_status_history`, `payment_methods`, `commission_rules`,
`commission_calculations`, `settlements`, `settlement_lines`,
`settlement_status_history`, `payout_batches`, `vendor_payouts`,
`payout_status_history`. All financial tables: `REVOKE DELETE FROM
authenticated`. All enums named; all FSM transitions enforced at the DB.

## 12. Money compliance
- All amounts `BIGINT paise` with `CHECK ≥ 0`.
- All aggregates carry `currency = 'INR'`.
- All arithmetic uses `Math.floorDiv` / integer math; no floats.
- Reconciliation invariants from `MONEY_SPEC §4` apply unchanged.