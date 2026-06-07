# Grafana dashboards

| Dashboard         | Source              | Panels                                       |
|-------------------|---------------------|----------------------------------------------|
| JVM (Micrometer)  | grafana.com #4701   | heap, GC, threads, uptime                    |
| Spring Boot 3     | grafana.com #17175  | HTTP req rate / p95, error %                 |
| HikariCP          | grafana.com #6083   | active / pending / wait                      |
| Postgres          | grafana.com #9628   | TPS, locks, cache hit, slow query            |
| commerce-biz      | custom (local)      | GMV, orders/min, reservation conflict,       |
|                   |                     | outbox lag, webhook DLQ, refund rate         |

Custom panels map 1:1 to the metrics in
`docs/PRODUCTION_OPERATIONS_GUIDE.md` §3.
