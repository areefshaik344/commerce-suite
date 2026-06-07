# Production Operations Guide

End-to-end operator manual. Builds on
`OPERATIONS_RUNBOOK.md`, `SRE_RUNBOOK.md`, `DISASTER_RECOVERY_PLAN.md`,
`RECONCILIATION_STRATEGY.md`, `PERFORMANCE_VALIDATION_REPORT.md`.

## 1. Topology snapshot

```
              ┌───────────────┐
  k8s ingress │  commerce-api │  (HPA 3–10, PDB minAvailable=2)
              └──────┬────────┘
                     │
       ┌─────────────┼──────────────┬──────────────┐
       ▼             ▼              ▼              ▼
   Postgres        Redis        Outbox          PSP / SMTP
   (PITR)       (RL+cache)     workers          (external)
```

## 2. Daily checklist

- [ ] Grafana `commerce-biz`: GMV, orders/min, refund %, drift = 0
- [ ] `outbox_pending_count` flat
- [ ] DLQ counters: outbox / notification / webhook = 0 (or known)
- [ ] HikariCP active < 70 % of max
- [ ] No active P-severity pages
- [ ] Backups: last PITR snapshot < 24 h old

## 3. Metric catalogue (Micrometer / Prometheus)

| Metric                                    | Type      | Use                          |
|-------------------------------------------|-----------|------------------------------|
| `http_server_requests_seconds`            | histogram | RED per controller           |
| `hikaricp_connections_active`             | gauge     | pool saturation              |
| `jvm_memory_used_bytes`                   | gauge     | heap pressure                |
| `outbox_pending_count`                    | gauge     | event backlog                |
| `outbox_dispatch_total`                   | counter   | throughput                   |
| `outbox_dead_letter_total`                | counter   | DLQ                          |
| `notification_dispatch_lag_seconds`       | gauge     | email/SMS lag                |
| `webhook_dispatch_total`                  | counter   | webhook throughput           |
| `webhook_dead_letter_total`               | counter   | webhook DLQ                  |
| `inventory_reservation_conflict_total`    | counter   | advisory-lock contention     |
| `payment_capture_total` / `..._failed`    | counter   | capture success rate         |
| `reconciliation_drift_amount{domain=...}` | gauge     | money drift                  |
| `business_orders_placed_total`            | counter   | GMV-class metric             |
| `business_gmv_paise_total`                | counter   | GMV                          |

Alert rules: `performance/prometheus/alerts.yml`.

## 4. Deploy procedure

1. CI green on `main` (`backend`, `frontend`, `docker`).
2. Tag release: `vYYYY.MM.DD-N`.
3. `kubectl set image deployment/commerce-api api=...:tag -n commerce`.
4. Watch rollout: `kubectl rollout status deployment/commerce-api`.
5. Smoke run: `LOAD=smoke k6 run performance/k6/scenarios/01_auth.js`.
6. Watch error rate / latency panels for 30 min.

Rollback: `kubectl rollout undo deployment/commerce-api`.

## 5. Capacity planning

| Tier      | Concurrent users | Pods | DB CPU | Redis |
|-----------|------------------|------|--------|-------|
| baseline  | 200              | 3    | 25 %   | 1 GB  |
| target    | 1 000            | 6    | 55 %   | 2 GB  |
| peak      | 3 000 (spike)    | 10   | 75 %   | 4 GB  |
| burst cap | 5 000 (5 min)    | 12   | 90 %   | Redis cluster |

Beyond peak, add a read-replica for catalog/PDP and shard the outbox
worker pool by aggregate type.

## 6. Change management

All schema changes via Flyway migrations (V0xx_...). NEVER edit a
shipped migration; add a new one. All financial tables are append-only;
corrections are compensating entries.

## 7. References

- `PRODUCTION_DEPLOYMENT_GUIDE.md` — initial deploy
- `STAGING_DEPLOYMENT_GUIDE.md`   — staging environment
- `DISASTER_RECOVERY_PLAN.md`     — restore drills, RPO/RTO
- `SECURITY_HARDENING_REPORT.md`  — security controls
