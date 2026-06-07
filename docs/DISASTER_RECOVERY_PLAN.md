# Disaster Recovery Plan

## 1. Objectives

| Metric | Target |
|--------|--------|
| **RPO** (max data loss) | ≤ 5 minutes (Postgres PITR / WAL shipping) |
| **RTO** (max downtime) | ≤ 60 minutes for application; ≤ 4 hours for full DB restore |
| Backup retention | 35 days PITR + 12 monthly snapshots |
| Cross-region | Daily encrypted snapshot replicated to secondary region |

## 2. Backup strategy

| Asset | Method | Frequency | Retention | Cross-region |
|-------|--------|-----------|-----------|--------------|
| Postgres | Managed PITR + automated daily snapshot | continuous + 1/d | 35 d / 12 mo | yes |
| Object storage (invoices, media) | Versioning + replication | continuous | 7 y | yes |
| Secrets manager | Provider replication | continuous | provider default | yes |
| Outbox / webhook state | Lives in Postgres — covered above | — | — | — |
| Application config | Git + GitOps (Argo CD) | per-commit | infinite | yes |

## 3. Restore procedures

### 3.1 Point-in-time restore (data corruption)

1. Identify last-known-good timestamp.
2. Provision a new Postgres instance from PITR snapshot at T.
3. Re-point app via DNS-aliased connection string.
4. Run integrity checks:
   - `SELECT count(*) FROM outbox_events WHERE status='PROCESSING';` — expect 0; reset stuck rows.
   - Re-run `calculator.compute` for active settlements and compare `calculationHash`.
5. Resume outbox + webhook dispatchers.
6. Notify finance to validate ledger.

### 3.2 Region failure

1. Promote cross-region replica to primary.
2. Apply latest container image from cross-region registry mirror.
3. Re-issue webhook signing keys if HSM-region-bound; rotate via admin API.
4. Update DNS to secondary region.
5. Run smoke + reconciliation suite.

### 3.3 Outbox poisoning

1. Pause dispatcher (`kubectl scale deploy/outbox-dispatcher --replicas=0`).
2. Identify poison rows via `last_error` patterns.
3. Move to `DEAD_LETTER` with reason; file defect.
4. Resume dispatcher.
5. Replay sanitized rows via admin API after fix is shipped.

### 3.4 Webhook backlog

1. The state machine drains naturally with backoff.
2. If a tenant endpoint is dead, disable subscription and notify.
3. For mass replay (post fix on our side), use `POST /admin/webhooks/deliveries/replay?since=...`.

### 3.5 Catastrophic data loss

1. Restore from latest snapshot in secondary region.
2. Accept RPO ≤ 24 h.
3. Communicate impact to finance and legal.
4. Open post-mortem; mandate root-cause within 5 business days.

## 4. Test schedule

| Drill | Frequency | Owner |
|-------|-----------|-------|
| PITR restore to scratch | Quarterly | SRE |
| Region failover (game day) | Semi-annual | SRE + Eng |
| Outbox poisoning simulation | Quarterly | Platform Eng |
| Secret rotation rehearsal | Quarterly | Security |
| Tabletop incident response | Quarterly | All |

## 5. Data retention

| Class | Retention | Notes |
|-------|-----------|-------|
| Auth tokens | 30 d after expiry | Hashed |
| Audit logs | 7 y (compliance) | Append-only |
| Financial records (orders, payments, refunds, settlements, payouts) | 7 y | Append-only |
| Customer PII (profile) | Until deletion request | Pseudonymise on deletion; keep financial linkage |
| Notification deliveries | 180 d | Aggregated thereafter |
| Analytics raw events | 400 d | Aggregations retained indefinitely |
| Webhook deliveries | 90 d | History 30 d |

## 6. Communication plan

- Status page update within 10 min of confirmed incident.
- Customer email within 4 h for any data-impacting event.
- Regulator notification per GDPR Art. 33 (≤ 72 h) where applicable.

## 7. Sign-off

This plan is reviewed by Eng Lead, SRE, Security, and Finance every 6 months and after any production-impacting incident.