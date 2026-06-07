# Reconciliation Strategy

Money invariants from `docs/MONEY_SPEC.md` must hold continuously.
Reconciliation is the safety net that proves they do.

## 1. Scope

| Domain      | Source of truth        | Mirror                  |
|-------------|------------------------|-------------------------|
| Payments    | `payments`             | PSP report (per-day)    |
| Refunds     | `refunds`              | PSP refund report       |
| Settlements | `settlements`          | Computed from orders    |
| Payouts     | `payouts`              | Bank transfer file      |

## 2. Job: `ReconciliationJob` (nightly, 02:30 IST)

Pseudocode:

```
for day in [yesterday]:
  expected = sum(order.lines.subtotal - commission - refunds) per vendor
  recorded = sum(settlement.lines.amount) per vendor
  drift    = expected - recorded
  emit metric reconciliation_drift_amount{domain="settlement"} drift
  if abs(drift) > 0:
    write reconciliation_exceptions row, raise alert
```

Identical pattern for payments↔PSP and payouts↔bank file.

## 3. Tables (additive — no schema change required for V019)

```
reconciliation_runs(id, domain, run_at, status, drift_amount, sample)
reconciliation_exceptions(id, run_id, entity_type, entity_id, expected, recorded, delta, resolved_at)
```

## 4. Monitoring

- `reconciliation_drift_amount{domain=...}` — Prometheus gauge, 0 in
  steady state.
- Alert: `ReconciliationDriftDetected` (see
  `performance/prometheus/alerts.yml`).
- Grafana panel "Money drift (7d)" on `commerce-biz` dashboard.

## 5. Operator playbook

If drift is non-zero:

1. Open the latest `reconciliation_exceptions` rows.
2. Cross-check the offending entity in the source and mirror.
3. If the mirror is wrong → re-import the report.
4. If the source is wrong → open a P1 ticket, freeze payouts for the
   affected vendor, page Finance.
5. Resolve by writing a compensating ledger entry (NEVER mutate
   historical financial rows — append-only by policy).
