# k6 Load Test Suite

Realistic load + concurrency scenarios for the commerce platform backend
(Spring Boot, Phase 9.5 hardened build). All scripts target the public
REST surface; no internal endpoints are exercised.

## Layout

```
performance/k6/
├── lib/                Shared helpers (auth, http, data, checks)
└── scenarios/
    ├── 01_auth.js                 login + refresh rotation
    ├── 02_catalog_browse.js       category → list → PDP
    ├── 03_vendor_workflow.js      product create / inventory update
    ├── 04_cart.js                 add / update / remove
    ├── 05_checkout.js             3-step checkout + reservation
    ├── 06_order_create.js         order placement + idempotency replay
    ├── 07_payment.js              intent → capture
    ├── 08_webhook_dispatch.js     webhook delivery probe
    ├── 09_notification.js         notification fetch probe
    ├── 10_concurrency_oversell.js 100 VUs race 1 SKU
    └── 11_full_journey.js         60-min soak
```

## Running

```bash
K6_BASE_URL=https://staging.example.com/api/v1 k6 run performance/k6/scenarios/01_auth.js
LOAD=load   k6 run performance/k6/scenarios/05_checkout.js
LOAD=stress k6 run performance/k6/scenarios/11_full_journey.js
```

## Profiles (`lib/config.js`)

| Profile | VUs        | Duration | Purpose                    |
|---------|------------|----------|----------------------------|
| smoke   | 5          | 1m       | sanity in CI               |
| load    | 200        | 15m      | nominal peak               |
| stress  | 800        | 20m      | breaking point             |
| spike   | 50 → 1000  | 5m       | flash-sale burst           |
| soak    | 150        | 60m      | leak / pool stability      |

## Global thresholds

- `http_req_failed`           < 1 %
- `http_req_duration p(95)`   < 800 ms
- `http_req_duration p(99)`   < 1500 ms
- `checks`                    > 99 %

Per-endpoint targets live in `docs/PERFORMANCE_VALIDATION_REPORT.md`.
