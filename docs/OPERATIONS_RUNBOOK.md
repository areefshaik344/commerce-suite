# Operations Runbook

Authoritative on-call playbook for the platform. Pair with `DISASTER_RECOVERY_PLAN.md` for catastrophic events.

## 1. Service topology

```text
 [Client] → [CDN] → [Ingress] → [Backend Pods] → [Postgres HA]
                                       │
                                       ├─→ [Redis]   (rate limit, cache)
                                       ├─→ [Outbox dispatcher pool]
                                       └─→ [Webhook dispatcher pool]
 [Outbox] → consumers: Audit | Notification | Analytics | Webhook
```

## 2. Routine health checks

| Check | Source | Threshold |
|-------|--------|-----------|
| Liveness | `/actuator/health/liveness` | 200 |
| Readiness | `/actuator/health/readiness` | 200 |
| Outbox lag | `outbox_pending_age_seconds` (P95) | < 60 s |
| Webhook success | `webhook_delivery_success_ratio` (5 m) | > 0.98 |
| Payment success | `payment_capture_success_ratio` (5 m) | > 0.97 |
| Settlement queue | `settlement_locked_count` | < 500 |

## 3. Standard incidents

### 3.1 Outbox lag rising

1. Check dispatcher pod logs for exceptions.
2. `SELECT status, count(*) FROM outbox_events GROUP BY 1;` — confirm `PROCESSING` not stuck.
3. If a poison message: identify by `aggregate_id`, move to `DEAD_LETTER` via admin API, then file a defect.
4. Scale dispatcher replicas (`kubectl scale deploy/outbox-dispatcher --replicas=N`).

### 3.2 Webhook failures spike

1. Inspect `webhook_attempts` for failing endpoint id.
2. If a single tenant: notify tenant; the FSM retries with backoff.
3. If global: check outbound network egress and TLS.
4. Replay `DEAD_LETTER` rows via `POST /api/v1/admin/webhooks/deliveries/{id}/replay` once root cause resolved.

### 3.3 Payment capture failures

1. Confirm gateway status page.
2. Check `payment_intents` with `status=FAILED` in last 15 m.
3. If gateway-side: enable maintenance banner; do NOT mass-refund.
4. If our side: roll back to previous image; preserve idempotency keys.

### 3.4 Auth abuse / brute-force

1. Inspect rate-limit metrics (`rate_limit_rejected_total{route="/auth/login"}`).
2. Tighten bucket capacity via config hot-reload.
3. Block offending CIDRs at ingress / WAF.

### 3.5 Database CPU saturation

1. `pg_stat_activity` — identify long-running queries.
2. Check missing index alerts (`pg_stat_user_indexes`).
3. Failover to replica if primary degraded.

### 3.6 Settlement reconciliation mismatch

1. Pause payout dispatcher.
2. Re-run calculator for the affected window; compare `calculationHash`.
3. If mismatch persists, escalate to finance lead — do NOT release payouts.

## 4. Maintenance procedures

### Deployment

- Use rolling update with `maxUnavailable=0`.
- Pre-deploy: run Flyway in a one-shot Job; pods only start after migrations succeed.
- Post-deploy: smoke test `/actuator/health` + a synthetic checkout.

### Rollback

- `kubectl rollout undo deploy/backend`.
- Migrations are forward-only; never `flyway undo`. If a migration is bad, write a corrective `V0xx__fix_*.sql`.

### Secret rotation

- JWT signing key: dual-key rotation — publish new `kid`, keep old for refresh-token TTL, then remove.
- Webhook secrets: rotated per-endpoint via admin API; plaintext is returned **once**.

## 5. On-call

- Rotation: weekly, primary + secondary.
- SLOs: ack < 5 min, mitigate < 30 min, post-mortem within 5 business days.
- Channels: `#oncall-prod` (Slack), PagerDuty schedule `commerce-prod`.

## 6. Useful commands

```sh
# Pending outbox by aggregate type
psql -c "SELECT aggregate_type, count(*) FROM outbox_events WHERE status='PENDING' GROUP BY 1;"

# Recent dead-letters
psql -c "SELECT id, event_type, last_error FROM outbox_events WHERE status='DEAD_LETTER' ORDER BY updated_at DESC LIMIT 20;"

# Notification delivery counts last hour
psql -c "SELECT channel, status, count(*) FROM notification_deliveries WHERE created_at > now() - interval '1 hour' GROUP BY 1,2;"
```
---

## DLQ replay (Phase 9.5)

When the on-call sees a dead-letter alert:

1. Identify channel: `outbox`, `notification`, or `webhook`.
2. Read the count: `GET /api/v1/admin/dlq/{channel}/count`.
3. Spot-check a sample row in the DB to confirm the underlying cause is resolved.
4. Replay all: `POST /api/v1/admin/dlq/{channel}/replay`
   Or one: `POST /api/v1/admin/dlq/{channel}/replay/{id}`
5. Watch `outbox.deadletter` / `webhooks.failed` counters return to zero.

Replay is idempotent: downstream consumers deduplicate by event id.

## JWT signing-key rotation

1. Generate new key, store in secret backend under `JWT_SECRET_NEXT`.
2. Roll deployment with dual-`kid` support (next release).
3. After full fleet is on the new build, promote `JWT_SECRET_NEXT` → `JWT_SECRET` and remove old key.
4. Force refresh of all sessions if compromise suspected (truncate `refresh_tokens`).

## MFA reset for an admin

`UPDATE mfa_factors SET status='DISABLED' WHERE user_id = :id;` — user must re-enroll on next login.
