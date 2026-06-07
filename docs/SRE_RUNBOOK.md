# SRE Runbook

Operational procedures for the on-call engineer. Pairs with
`docs/OPERATIONS_RUNBOOK.md` (developer-facing) and
`docs/DISASTER_RECOVERY_PLAN.md`.

## 1. SLO catalogue

| Service          | SLI                                  | SLO (30d) | Error budget |
|------------------|--------------------------------------|-----------|--------------|
| API availability | non-5xx / total                      | 99.9 %    | 43 min/30d   |
| API latency      | p95 < 800 ms on key endpoints        | 99.0 %    | 7.2 h/30d    |
| Checkout success | placed / attempted                   | 99.5 %    | 3.6 h/30d    |
| Payment capture  | captured / authorized                | 99.7 %    | 2.2 h/30d    |
| Outbox lag       | dispatched within 5 s                | 99.0 %    | 7.2 h/30d    |
| Webhook delivery | delivered ≤ 5 attempts               | 99.0 %    | 7.2 h/30d    |

Burn-rate alerts (multi-window) per Google SRE handbook §5.

## 2. Alert response

### HighHttp5xxRate (page)

1. Open Grafana → Spring Boot 3 → Error % by URI.
2. Top URI → tail logs (`kubectl logs -l app=commerce -f --tail=200`).
3. If a single dependency (DB, PSP, Redis) is down → trigger DR §3.
4. If a code regression → roll back the last Deployment revision:
   `kubectl rollout undo deployment/commerce-api -n commerce`.

### OutboxBacklogGrowing (page)

1. Check `outbox_pending_count` panel.
2. Inspect worker pods: `kubectl top pod -l role=worker`.
3. If CPU-bound → scale HPA min: `kubectl scale --replicas=6 …`.
4. If DB-bound (locks) → check `pg_stat_activity` for waits on
   `outbox_messages`; vacuum/analyze if bloat > 20 %.

### WebhookDeadLetterSpike (page)

1. Open DLQ admin: `GET /admin/dlq/webhooks`.
2. Sample 5 DEAD_LETTER rows; inspect `last_error`.
3. If endpoint hard-fail (4xx) → notify integration owner; suppress.
4. If endpoint flap (5xx/timeout) → replay via
   `POST /admin/dlq/webhooks/replay`.

### ReconciliationDriftDetected (page)

Follow `RECONCILIATION_STRATEGY.md §5`. Freeze payouts first.

### DBConnectionsSaturated (page)

1. `kubectl exec` into a pod and dump pool: `/actuator/metrics/hikaricp.connections.active`.
2. Identify the slow query: `pg_stat_statements` top by total_time.
3. Add `statement_timeout` if a runaway query; kill via `pg_terminate_backend`.

## 3. Routine ops

| Cadence  | Task                                                  |
|----------|-------------------------------------------------------|
| Daily    | Review reconciliation drift dashboard                 |
| Weekly   | Review DLQ counts (outbox + webhook + notification)   |
| Monthly  | Rotate JWT signing keys (procedure in SECURITY doc)   |
| Monthly  | Restore-from-backup drill (DR plan §4)                |
| Quarter  | Full DR game-day; chaos test (kill leader pod)        |

## 4. Postmortem template

`docs/postmortems/YYYY-MM-DD-<slug>.md`:
Impact · Timeline · Root cause · Detection · Response · Action items
(with owners + due dates) · Whether SLO budget was burned.

## 5. Escalation

| Severity | Response time | Notify                              |
|----------|---------------|-------------------------------------|
| Sev-1    | 5 min         | On-call → Eng lead → CTO            |
| Sev-2    | 30 min        | On-call → Eng lead                  |
| Sev-3    | next BD       | Ticket queue                        |
