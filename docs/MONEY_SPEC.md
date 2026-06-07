# Money Representation Specification (B-01 Resolution)

**Status:** FROZEN — June 7, 2026. Binding across frontend, backend, DB, events, and webhooks.

## 1. Canonical representation

- **Storage unit:** integer **paise** (1 INR = 100 paise).
- **Type:** `BIGINT` in PostgreSQL, `long` in Java, `number` (safe-integer) in TypeScript.
- **Currency:** ISO-4217 code, fixed to `"INR"` for v1. Every monetary field MUST be accompanied by a `currency` field at the aggregate root (order, intent, refund, payout, settlement).
- **No floats. No decimals. No strings.** A field named `amount`, `subtotal`, `total`, `discount`, `tax`, `shipping`, `fee`, `refunded_amount`, `captured_amount`, `commission`, `payout_amount`, etc. is ALWAYS paise.

## 2. Conversion rules

| Direction              | Rule                                                |
|------------------------|-----------------------------------------------------|
| Rupees → paise         | `Math.round(rupees * 100)` (banker's not required)  |
| Paise → rupees (display) | `paise / 100` formatted with `Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 })` |
| External gateway       | Convert at the adapter boundary only                |

The **frontend boundary** for v1 keeps existing rupee-integer DTOs (backwards compatible). A single adapter `toPaise(rupees)` / `toRupees(paise)` lives in `src/lib/money.ts` (to be added at backend cutover). Until cutover, the frontend treats all monetary fields as **integer rupees** and the API gateway performs `* 100` at egress / `/ 100` at ingress.

## 3. Rounding strategy

- All arithmetic performed in **paise (integer)**. No intermediate float.
- Tax, discount distribution, and commission splits use **largest-remainder** allocation: floor each share, then distribute leftover paise to the largest residuals in deterministic (vendorId asc) order so totals reconcile to the cent.
- Per-line rounding happens **once**, after tax and discount are applied.

## 4. Reconciliation invariants

For every order:
```
order.grand_total = Σ child_order.total
child_order.total = subtotal − discount + shipping + tax + surcharge
Σ payment_intent.captured_amount − Σ refund.amount = order.outstanding_balance
Σ settlement_ledger.credit − Σ settlement_ledger.debit = vendor.payable_balance
```
These invariants are asserted by a nightly `reconcile_money` job and on every state-transition trigger.

## 5. API contract

- JSON numbers (safe within ±2^53 paise ≈ ₹90 trillion). Field naming: `amount_paise` is **not** used — the unit is implicit from the spec. The envelope MUST declare `"currency": "INR"`.
- Money fields are non-null `integer >= 0`. Refund deltas use a positive amount + explicit `kind=REFUND`.
- OpenAPI: `format: int64`, `minimum: 0`, `x-unit: paise`.

## 6. Database

```sql
amount        BIGINT NOT NULL CHECK (amount >= 0)
currency      CHAR(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR')
```
CHECK constraints enforce non-negativity. Refund/credit tables use a signed `delta BIGINT` with sign discipline.

## 7. Events / webhooks

Every monetary field in outbox payloads follows the same rule (integer paise + currency). Webhook consumers MUST treat the value as paise.

## 8. Backwards compatibility

The frontend continues to use rupee integers in DTOs until the backend gateway is live. A shim in `apiClient` will translate at the network boundary at cutover; no component code changes required.
*** End Patch
