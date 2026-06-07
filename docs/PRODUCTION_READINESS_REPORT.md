# Production Readiness Report

_Generated: Phase 9 audit — June 2026. Source of truth: implemented code, migrations V001–V017, and module docs (`docs/*_MODULE.md`)._

## 1. Scope

All implemented phases reviewed:

| Phase | Modules | Status |
|-------|---------|--------|
| 1     | Common, Security, Auth, RBAC, Users | Implemented |
| 2     | Vendor onboarding & KYC | Implemented |
| 3     | Catalog (categories, products, variants, media) | Implemented |
| 4     | Inventory (reservation FSM, low-stock) | Implemented |
| 5     | Cart, Checkout (3-step) | Implemented |
| 6     | Orders, Shipping, Returns, Refunds | Implemented |
| 6.5   | Hardening — idempotency, FSM enforcement, money spec | Implemented |
| 7     | Payments, Commission, Settlement, Payouts | Implemented |
| 8.1   | Durable outbox (`V013`) | Implemented |
| 8.2   | Notifications (`V014`) | Implemented |
| 8.3   | Audit expansion (`V015`) | Implemented |
| 8.4   | Analytics (`V016`) | Implemented |
| 8.5   | Webhooks (`V017`) | Implemented |

## 2. Production Scorecard (0–10)

| Dimension | Score | Notes |
|-----------|-------|-------|
| Architecture | 9 | Clear modular boundaries; outbox decouples cross-cutting concerns; FSMs enforced. |
| Security | 7 | JWT + RBAC + ownership guards in place. Rate-limiting, brute-force lockout, and security-header middleware not yet hardened. See §4. |
| Performance | 7 | Indexed hot paths; pagination URL-synced. N+1 risks in settlement line aggregation and audit search. |
| Scalability | 7 | Outbox + webhook dispatcher use `FOR UPDATE SKIP LOCKED`; horizontal-scale-safe. Notification batching not yet sharded. |
| Maintainability | 9 | Strong docs/module pattern; tests per module; clear FSM contracts. |
| Observability | 5 | Correlation IDs propagated; structured logs minimal; no metrics/tracing exporter wired (Micrometer/OTel TBD). |
| Operability | 6 | Outbox/webhook FSMs visible; missing runbook automation and DLQ replay tooling. |
| Deployment Readiness | 6 | App boots on a single JVM; no Dockerfile or k8s manifests committed; HA Postgres not specified. |
| Business Continuity | 5 | Backup/restore procedure undocumented; RPO/RTO undefined until §6. |
| Documentation | 9 | Module docs, FSM specs, blueprint, completion report all present. |
| **Overall** | **7.0** | Solid foundation; gaps are operational, not architectural. |

## 3. Findings by severity

### BLOCKER — must fix before production

1. **No deployment manifests.** No `Dockerfile`, no `docker-compose` for non-dev, no k8s descriptors. Impact: cannot reproducibly deploy. Remediation: add a multi-stage Dockerfile, k8s `Deployment`/`Service`/`HorizontalPodAutoscaler`, and a managed-Postgres connection profile.
2. **No backup/restore validation.** Migrations exist but no documented PITR procedure, no tested restore. Remediation: enable managed-Postgres PITR; run a quarterly restore drill (see `DISASTER_RECOVERY_PLAN.md`).
3. **Secret management is environment-implicit.** `application.yml` references env vars; no Vault/Secrets-Manager integration documented. Remediation: codify secrets sourcing (e.g. AWS Secrets Manager / SOPS) and rotate JWT signing keys.

### HIGH

4. **No global rate limiting / brute-force protection** on `/auth/login`, `/auth/password/*`, `/auth/email/verify`. Add a bucket-based limiter (Bucket4j + Redis) keyed by IP + email.
5. **Security headers middleware absent.** `SecurityConfig` does not set `Content-Security-Policy`, `Strict-Transport-Security`, `X-Frame-Options`, `Referrer-Policy`. Add a `HeaderWriter` chain.
6. **No observability stack.** No Micrometer registry, OTel exporter, or `/actuator/prometheus`. Add Micrometer + Prometheus and OTel HTTP exporter; emit RED metrics per controller.
7. **Outbox + webhook DLQ has no replay UI/API.** Operators cannot requeue `DEAD_LETTER` rows without DB writes. Add an admin endpoint guarded by `SUPER_ADMIN`.
8. **No dependency-scan job** in CI for backend (`gradle dependencyCheck` or Snyk). Frontend has `npm audit` available but is not enforced.

### MEDIUM

9. CSRF disabled globally — acceptable for stateless JWT but document the decision and ensure cookies are never used for auth tokens.
10. Analytics aggregations rely on a scheduled job; missing watchdog if the scheduler misses a window.
11. Notification template rendering lacks an HTML sanitiser for user-supplied variables (XSS risk in email previews).
12. Audit retention policies defined but no scheduled `AuditRetentionJob` invocation verified end-to-end in IT.
13. No load-test baseline. Add k6 scripts for checkout, payment capture, and webhook fan-out.

### LOW

14. Some controllers return raw entities in tests; ensure DTO boundary always enforced.
15. `application.yml` log level defaults to `INFO`; add `MDC` keys `requestId`, `actorId`, `correlationId` to log pattern.
16. README lacks a "production deploy" section pointing to `DEPLOYMENT_ARCHITECTURE.md`.

## 4. Security audit summary

See `SECURITY_HARDENING_REPORT.md` for the detailed table. Highlights:

- ✅ BCrypt(12), JWT access+refresh rotation, role-based `@PreAuthorize`, ownership guards, append-only audit & financial tables, HMAC-SHA256 webhook signing with timestamp+nonce.
- ❌ Rate limiting, security headers, HIBP password check, MFA for admins, account lockout, IP allowlist for admin endpoints.

## 5. Financial integrity

- `Money.java` enforces integer paise; DB CHECK `amount >= 0`; currency pinned to INR.
- Settlement calculator is deterministic; `calculationHash` stored for reproducibility.
- Payment, Refund, Settlement, Payout FSMs each have unit tests for illegal transitions.
- Append-only enforcement via `REVOKE DELETE` on financial history tables.
- **Gap:** no nightly reconciliation job wired (`reconcile_money` mentioned in `MONEY_SPEC.md` §4 but not implemented).

## 6. Event architecture

- Outbox is the single durable hop; subscribers are idempotent and run in `REQUIRES_NEW`.
- No subscriber currently re-publishes to the same outbox aggregate type → no observed event loops.
- Audit, Notification, Analytics, Webhook consumers each consume `OutboxDispatchEvent`; failures retry with exponential backoff; `DEAD_LETTER` is terminal.
- **Gap:** No cross-module replay tool; manual SQL is required to reset `attempt_count`.

## 7. Final verdict

> **FIX BEFORE PRODUCTION**

The platform is architecturally production-grade and feature-complete, but the **deployment, observability, and DR layers are not yet operable**. Resolve the three BLOCKERS and the four HIGH findings, then re-audit. Target: **READY FOR STAGING immediately** once Dockerfile + secrets sourcing land; **READY FOR PRODUCTION** after items 1–8 close.

## 8. Required actions (ordered)

1. Land Dockerfile + k8s manifests + secrets sourcing (BLOCKERS 1, 3).
2. Enable managed-Postgres PITR; document and rehearse restore (BLOCKER 2).
3. Add rate limiting + security headers + HIBP toggle (HIGH 4, 5).
4. Wire Micrometer + OTel exporters; ship Grafana dashboards (HIGH 6).
5. Build DLQ replay API and admin UI (HIGH 7).
6. Enforce dependency scanning in CI (HIGH 8).
7. Implement nightly `reconcile_money` job (§5 gap).
8. Author k6 load-test suite and capture baseline (MEDIUM 13).

After (1)–(8), reissue this report.
---

## Addendum — Phase 9.5 hardening pass

The Phase 9.5 implementation closed the BLOCKER and most HIGH findings:

| Item | Status |
|------|--------|
| Dockerfile + compose                                | Resolved (`deployment/docker/`)           |
| Kubernetes manifests + HPA + PDB + NetworkPolicy    | Resolved (`deployment/k8s/`)              |
| Secret provider abstraction (env/AWS/Vault/Azure/GCP)| Resolved (`common.secrets`)              |
| Rate limiting on auth + admin + webhook endpoints   | Resolved (`common.ratelimit`)             |
| Security headers (CSP, HSTS, XFO, COOP, CORP, etc.) | Resolved (`SecurityHeadersFilter`)        |
| MFA (TOTP + recovery codes)                         | Resolved (`mfa` module, V018)             |
| Observability — Prometheus exporter, biz metrics    | Resolved (Micrometer + `BusinessMetrics`) |
| Liveness/Readiness probes + custom health           | Resolved (`common.health`)                |
| DLQ replay API for outbox/notifications/webhooks    | Resolved (`common.dlq`)                   |
| CI: build, test, dep-scan, security-scan, image     | Resolved (`.github/workflows/`)           |

### Updated scorecard

| Dimension          | Before | After |
|--------------------|--------|-------|
| Security           | 7      | 9     |
| Observability      | 5      | 8     |
| Operability        | 6      | 8     |
| Deployment         | 6      | 9     |
| Business Continuity| 5      | 7     |
| **Overall**        | 7.0    | 8.4   |

### Remaining items (MEDIUM)

- Nightly money reconciliation job
- k6 load-test baseline
- HIBP password check (toggle via Lovable Cloud setting)
- Step-up auth on PII export
- Documented JWT signing-key rotation drill

### Final verdict

> **READY FOR STAGING.** Promote to production once the MEDIUM list above is closed and the staging restore-from-backup drill (per `DISASTER_RECOVERY_PLAN.md` §4) is signed off.
