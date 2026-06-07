# k6 Load Test Suite

Realistic load + concurrency scenarios for the commerce platform backend
(Spring Boot, Phase 9.5 hardened build). All scripts target the public
REST surface; no internal endpoints are exercised.

## Layout

```
performance/k6/
├── lib/                Shared helpers (auth, http, data, checks)
│   ├── config.js       Env-driven base URL, VU profile, thresholds
│   ├── http.js         Authenticated fetch + correlation-id propagation
│   ├── auth.js         Login / refresh / logout flows
│   └── data.js         Fixture loader + idempotency-key generator
└── scenarios/
    ├── 01_auth.js              login + refresh rotation
    ├── 02_catalog_browse.js    category → list → PDP
    ├── 03_vendor_workflow.js   product create / inventory update
    ├── 04_cart.js              add / update / remove
    ├── 05_checkout.js          3-step checkout, soak the reservation FSM
    ├── 06_order_create.js      end-to-end order placement
    ├── 07_payment.js           intent → capture → settle
    ├── 08_webhook_dispatch.js  outbound webhook fan-out probe
    ├── 09_notification.js      notification dispatch probe
    ├── 10_concurrency_oversell.js   100 VUs racing 1 SKU
    └── 11_full_journey.js      soak: browse→checkout→pay (60 min)
```

## Running

```bash
# Smoke (default: 5 VUs, 1 min)
K6_BASE_URL=https://staging.example.com/api/v1 \
  k6 run performance/k6/scenarios/01_auth.js

# Load profile (env LOAD=load|stress|soak|spike)
LOAD=load k6 run performance/k6/scenarios/05_checkout.js

# Full regression
LOAD=load k6 run performance/k6/scenarios/11_full_journey.js
```

## Profiles (see `lib/config.js`)

| Profile | VUs        | Duration | Purpose                    |
|---------|------------|----------|----------------------------|
| smoke   | 5          | 1m       | sanity in CI               |
| load    | 200        | 15m      | nominal peak               |
| stress  | 800        | 20m      | find breaking point        |
| spike   | 50 → 1000  | 5m       | flash-sale burst           |
| soak    | 150        | 60m      | leak / FD / pool stability |

## Thresholds (global)

- `http_req_failed`           < 1 %
- `http_req_duration{p(95)}`  < 800 ms
- `http_req_duration{p(99)}`  < 1500 ms
- `checks`                    > 99 %

Per-endpoint targets live in `docs/PERFORMANCE_VALIDATION_REPORT.md`.