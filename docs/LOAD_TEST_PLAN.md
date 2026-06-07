# Load Test Plan — Phase 11

_Source of truth: `performance/k6/`, this document, and
`docs/PERFORMANCE_VALIDATION_REPORT.md`._

## 1. Objectives

Prove that the platform sustains the documented production targets under:

- nominal load (200 concurrent users for 15 min),
- stress (800 VUs for 20 min, looking for the knee),
- spike (50 → 1000 VUs in 90 s, flash-sale),
- soak (150 VUs for 60 min, leak/connection drift),
- contention (100 VUs racing a single SKU, oversell guard).

## 2. Environment

| Component   | Spec                                                             |
|-------------|------------------------------------------------------------------|
| Backend     | 3× pods, 2 vCPU / 2 GiB, JVM `-Xms1g -Xmx1500m`, HPA 3–10        |
| Postgres    | Managed, 4 vCPU / 16 GiB, `max_connections=200`, PITR on        |
| Redis       | 1× node, 1 GiB, AOF on (rate limiter + token cache)              |
| Load gen    | 2× k6 runners, ≥ 4 vCPU each, on the same VPC as the backend     |
| Network     | k6 → ingress via private LB; baseline ping < 1 ms                |

Staging environment is provisioned per `docs/STAGING_DEPLOYMENT_GUIDE.md`.

## 3. Scenarios

| # | Script                       | Goal                              | Profile |
|---|------------------------------|-----------------------------------|---------|
| 1 | `01_auth.js`                 | login + refresh                   | load    |
| 2 | `02_catalog_browse.js`       | browse path                       | load    |
| 3 | `03_vendor_workflow.js`      | vendor writes                     | load    |
| 4 | `04_cart.js`                 | cart mutate                       | load    |
| 5 | `05_checkout.js`             | 3-step checkout                   | load    |
| 6 | `06_order_create.js`         | order + idempotency replay        | load    |
| 7 | `07_payment.js`              | intent + capture                  | load    |
| 8 | `08_webhook_dispatch.js`     | webhook fan-out probe             | load    |
| 9 | `09_notification.js`         | notification fetch                | load    |
|10 | `10_concurrency_oversell.js` | reservation contention            | race    |
|11 | `11_full_journey.js`         | end-to-end soak                   | soak    |

## 4. Acceptance criteria

- All scenarios meet global thresholds (`performance/k6/README.md`).
- Endpoint targets in `PERFORMANCE_VALIDATION_REPORT.md §3` met.
- Concurrency scenario: total reserved ≤ stock (no overselling).
- Idempotent replay: duplicate POST returns same id, no second row.
- Soak: HikariCP `connections_active` flat; JVM heap not climbing.

## 5. Reporting

k6 JSON summary → `performance/results/<scenario>-<timestamp>.json`,
ingested into Grafana via Loki/promtail or the k6 Cloud uploader.
