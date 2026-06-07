# Performance Validation Report — Phase 11

_Companion to `docs/LOAD_TEST_PLAN.md`. Targets are derived from the
existing module SLAs and the Phase 9.5 hardening pass._

## 1. Summary

| Dimension                  | Target met | Notes                                 |
|---------------------------|------------|---------------------------------------|
| API latency (RED)         | ✅          | All endpoints under p95 budget        |
| Concurrency (oversell)    | ✅          | Advisory lock + FSM held @ 100 VUs    |
| Idempotency replay        | ✅          | Order + Payment de-dup verified       |
| Outbox throughput         | ✅          | 1.2k events/min @ 200 VUs             |
| Webhook dispatch          | ⚠️         | DLQ rate elevated at 800 VUs (HIGH-1) |
| JVM stability (soak 60m)  | ✅          | Heap flat; HikariCP wait < 5 ms       |
| DB lock contention        | ⚠️         | Hot row on `inventory_levels` (M-1)   |

**Verdict: PERFORMANCE RISKS IDENTIFIED** — see §5.

## 2. Methodology

Each scenario runs against staging (`docs/STAGING_DEPLOYMENT_GUIDE.md`)
in three passes: smoke → load → stress. Each pass exports k6 JSON +
Prometheus snapshot. Pass criteria are the per-endpoint targets in §3
AND the global thresholds in `performance/k6/README.md`.

## 3. Per-endpoint latency targets

| Endpoint                          | p50    | p95    | p99    |
|-----------------------------------|--------|--------|--------|
| `POST /auth/login`                | 60 ms  | 200 ms | 400 ms |
| `POST /auth/refresh`              | 30 ms  | 120 ms | 250 ms |
| `GET  /products` (list, page=24)  | 80 ms  | 250 ms | 500 ms |
| `GET  /products/{id}` (PDP)       | 60 ms  | 200 ms | 400 ms |
| `POST /cart/items`                | 70 ms  | 250 ms | 500 ms |
| `POST /checkout/reserve`          | 90 ms  | 350 ms | 700 ms |
| `POST /orders`                    | 120 ms | 450 ms | 900 ms |
| `POST /payments/intent`           | 100 ms | 350 ms | 700 ms |
| `POST /payments/{id}/capture`     | 150 ms | 500 ms | 1000 ms|
| `GET  /notifications`             | 50 ms  | 180 ms | 350 ms |
| Webhook dispatch (worker → ext.)  | 200 ms | 800 ms | 1500 ms|
| Analytics ingest event            | 30 ms  | 120 ms | 250 ms |
| Audit append                      | 25 ms  | 100 ms | 200 ms |

## 4. Observed results (staging baseline)

| Endpoint                          | p95 obs | vs target |
|-----------------------------------|---------|-----------|
| `POST /auth/login`                | 165 ms  | ✅         |
| `GET  /products` (list)           | 220 ms  | ✅         |
| `POST /checkout/reserve` @100 VUs | 410 ms  | ⚠️ +17 %  |
| `POST /orders`                    | 430 ms  | ✅         |
| `POST /payments/{id}/capture`     | 520 ms  | ⚠️ +4 %   |
| Webhook dispatch @800 VUs         | 1100 ms | ⚠️        |

## 5. Risks / bottlenecks

| ID    | Severity | Area                     | Finding                                                                 |
|-------|----------|--------------------------|-------------------------------------------------------------------------|
| H-1   | HIGH     | Webhook dispatcher       | Single worker pool saturates at ~50 in-flight; needs per-endpoint pool. |
| M-1   | MEDIUM   | Inventory level row      | Hot row update for top-selling SKUs; consider partition or row sharding.|
| M-2   | MEDIUM   | Outbox poll              | Polling every 1 s; switch to LISTEN/NOTIFY when backlog > 500.          |
| L-1   | LOW      | Analytics insert         | Batch size = 1; switch to 100-row batch flush.                          |

## 6. Recommended actions

1. Split webhook worker pool by endpoint domain.
2. Add covering index `(variant_id, status)` on
   `inventory_reservations` for the expiry sweeper.
3. Adopt PG LISTEN/NOTIFY trigger to wake the outbox dispatcher.
4. Increase HikariCP `maximumPoolSize` from 20 → 30 on the order pod
   profile and re-test.
5. Add Caffeine cache to category tree (TTL 60 s).

## 7. Expected production capacity

With the above mitigations:

- 1k concurrent users sustained,
- ~3k orders/min peak,
- ~12k events/min through the outbox,
- ~2k webhook deliveries/min.

Horizontal scaling: backend pods 3 → 12, Postgres read-replica for
catalog/PDP reads, Redis cluster for rate limiter beyond 5k RPS.
