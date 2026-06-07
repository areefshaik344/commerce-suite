# Performance Optimization Report — Phase 12

_Implements mitigations from `docs/PERFORMANCE_VALIDATION_REPORT.md`._

## 1. Scope

Pure performance / infrastructure work — no business behaviour, no
domain model changes, no new modules.

Targets addressed:

| ID  | Area                | Mitigation                          |
|-----|---------------------|--------------------------------------|
| H-1 | Webhook saturation  | Per-endpoint concurrency gate        |
| M-1 | Inventory hot row   | Covering partial indexes             |
| M-2 | Outbox poll latency | LISTEN/NOTIFY trigger + tuned poll   |
| L-1 | Analytics batch     | Configurable batch, claim coverage   |

## 2. Files changed

| File | Change |
|------|--------|
| `backend/src/main/resources/db/migration/V019__perf_optimizations.sql` | NEW — additive indexes + `pg_notify` trigger |
| `backend/src/main/java/com/commercesuite/common/outbox/OutboxDispatcher.java` | `batch-size` now `@Value`-injected (default 100, was 50) |
| `backend/src/main/java/com/commercesuite/webhooks/service/WebhookDispatcher.java` | Per-endpoint `Semaphore` gate; saturated endpoints leave rows QUEUED for next tick instead of blocking the worker |
| `backend/src/main/resources/application.yml` | New `outbox.*` and `webhooks.*` tuning blocks with env overrides |

## 3. Why each change is safe

* **V019 migration** — only `CREATE INDEX IF NOT EXISTS` and a NOTIFY
  trigger; no schema rewrites, no data migrations, no constraint
  changes. Reversible by drop.
* **Outbox batch size** — bound is configurable; default raised from 50
  → 100. Claim still uses `FOR UPDATE SKIP LOCKED`, so larger batches
  do not increase lock contention.
* **Webhook per-endpoint gate** — uses `tryAcquire` (non-blocking). A
  saturated endpoint releases its row back to the next tick rather than
  blocking the dispatcher thread. Failure isolation guarantees from
  Phase 8 are preserved.
* **NOTIFY trigger** — fire-and-forget `pg_notify`; payload is a UUID
  string only. Existing pollers still work unchanged — the channel is
  opt-in for future LISTEN-driven wake-ups.

## 4. Before / After (staging baseline expectations)

| Metric                                     | Before  | After (target) |
|-------------------------------------------|---------|----------------|
| Outbox dispatch throughput (events/min)   | 1.2 k   | 3.0 k          |
| Outbox p95 publish lag                    | 1.4 s   | 0.6 s          |
| Webhook deliveries/min @ 800 VUs          | 1.0 k   | 2.2 k          |
| Webhook DLQ rate @ 800 VUs                | 4.1 %   | < 1.5 %        |
| `inventory_reservations` expiry sweep p95 | 380 ms  | 90 ms          |
| `POST /checkout/reserve` p95              | 410 ms  | < 320 ms       |

These numbers are derived from index-only scan estimates, the doubled
batch size, and the 2x→4x parallelism unlock from the per-endpoint
gate. They must be re-validated against the k6 scenarios in
`performance/k6/scenarios/08_webhook_dispatch.js` and
`performance/k6/scenarios/10_concurrency_oversell.js`.

## 5. Tuning surface (env vars)

```
OUTBOX_DISPATCHER_DELAY_MS=500     # was 1000
OUTBOX_DISPATCHER_BATCH=100        # was 50
WEBHOOKS_DISPATCHER_DELAY_MS=1000  # was 2000
WEBHOOKS_DISPATCHER_BATCH=50       # was 25
WEBHOOKS_PER_ENDPOINT_CONCURRENCY=4
```

All optional — defaults are production-safe.

## 6. New expected capacity

After mitigations:

* 1.5 k concurrent users sustained (was 1.0 k),
* ~4 k orders/min peak (was ~3 k),
* ~18 k events/min through the outbox (was ~12 k),
* ~3 k webhook deliveries/min (was ~2 k).

## 7. Remaining bottlenecks

| ID  | Severity | Item                                                      |
|-----|----------|-----------------------------------------------------------|
| R-1 | LOW      | LISTEN-side wake-up loop not yet wired (trigger only).    |
| R-2 | LOW      | Caffeine cache for category tree still pending (Phase 11). |
| R-3 | LOW      | Hot SKU inventory sharding deferred — fix requires schema rewrite. |

These are tracked for a follow-up sprint and do not block staging
re-validation.

## 8. Verdict

**OPTIMIZATIONS APPLIED** — re-run the k6 stress + soak profiles to
confirm the projected numbers and close H-1 / M-1 / M-2.
