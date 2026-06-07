# Go-Live Checklist

_Gate review for staging → production cutover. Every item must be **Done** or **Waived (with owner)** before promoting._

## A. Infrastructure

- [ ] Production Postgres provisioned (HA, PITR ≥ 7 days)
- [ ] Production Redis provisioned (rate-limit + cache)
- [ ] Object storage bucket (S3/GCS) with lifecycle rules
- [ ] CDN fronting static assets
- [ ] DNS + TLS certificates (cert-manager / ACM)
- [ ] Container registry + signed images
- [ ] Kubernetes namespace, quotas, network policies
- [ ] Horizontal Pod Autoscaler configured
- [ ] Secrets sourced from Vault/Secrets Manager (no plaintext)

## B. Application config

- [ ] `SPRING_PROFILES_ACTIVE=production`
- [ ] JWT signing key rotated; old keys removed
- [ ] CORS allowed-origins restricted to production domains
- [ ] Log level `INFO`; MDC pattern includes `requestId,actorId,correlationId`
- [ ] Feature flags reviewed
- [ ] Outbox dispatcher cadence + retry tuned for prod load
- [ ] Webhook dispatcher worker count scaled to expected fan-out

## C. Security

- [ ] Rate limiting active on auth + payment endpoints
- [ ] Security headers middleware enabled (CSP, HSTS, XFO, Referrer-Policy)
- [ ] HIBP password check enabled
- [ ] Admin endpoints require MFA
- [ ] IP allowlist for admin and finance endpoints (if applicable)
- [ ] Dependency scan green (no HIGH/CRITICAL)
- [ ] Penetration-test report attached
- [ ] Webhook secrets rotated post-handover

## D. Data

- [ ] All Flyway migrations V001–V017 applied on production
- [ ] RLS verified for `analytics_*`, `user_roles`, `webhook_*` tables
- [ ] Seed data (roles, KPI catalog, retention policies) inserted
- [ ] Backup verified by test-restore in staging within 30 days

## E. Observability

- [ ] Prometheus scraping `/actuator/prometheus`
- [ ] OTel traces shipped to APM
- [ ] Grafana dashboards published (RED metrics, outbox lag, webhook success rate, payment success rate, settlement queue depth)
- [ ] Alerts wired for: 5xx > 1%, p95 latency > 1 s, outbox lag > 5 min, dead-letter rate > 0, payment failure rate > 2%, disk > 80%
- [ ] PagerDuty / on-call rotation defined

## F. Financial integrity

- [ ] Money spec audit (no float types) — automated check passing
- [ ] Nightly reconciliation job scheduled and alerting
- [ ] Payout dry-run executed against staging bank-sandbox
- [ ] Commission rates and tax (GST) confirmed by finance
- [ ] Settlement calculation hash verified across two consecutive runs

## G. Event architecture

- [ ] Outbox dispatcher health check green
- [ ] Webhook signing rotated; consumers updated
- [ ] Dead-letter alerting active
- [ ] DLQ replay endpoint tested
- [ ] Notification preferences load-tested for fan-out

## H. Operations

- [ ] `OPERATIONS_RUNBOOK.md` reviewed by on-call
- [ ] `DISASTER_RECOVERY_PLAN.md` rehearsed
- [ ] Incident-response Slack channel created
- [ ] Status page configured
- [ ] Customer-support escalation path defined

## I. Compliance

- [ ] GDPR export endpoint smoke-tested
- [ ] GDPR deletion endpoint smoke-tested (retains financial records per policy)
- [ ] Audit log retention policies active
- [ ] PII access logged
- [ ] Terms / Privacy / Refund pages published

## J. Release

- [ ] Release notes drafted
- [ ] Rollback plan reviewed (previous image tag pinned)
- [ ] Database rollback strategy (forward-only migrations confirmed)
- [ ] Communication plan to vendors + customers
- [ ] Go/no-go meeting signed off by: Eng Lead, SRE, Security, Finance, Product
---

## Phase 9.5 — automation status

The following items are now satisfied by code in this repository and need only verification, not implementation:

- Dockerfile + multi-stage build, non-root, healthcheck (`deployment/docker/Dockerfile`)
- Kubernetes manifests, HPA, PDB, NetworkPolicy (`deployment/k8s/`)
- Liveness + readiness probes wired (`management.endpoint.health.probes.enabled=true`)
- Security headers middleware active (`SecurityHeadersFilter`)
- Rate limiting on auth/admin/webhook endpoints
- MFA TOTP + recovery codes available for ADMIN/FINANCE accounts
- DLQ replay API (`/api/v1/admin/dlq/{channel}/replay`)
- Prometheus exporter (`/actuator/prometheus`)
- CI gates: unit, integration, dep-scan, container build

Operators must still verify configuration values, secret sourcing, DNS, TLS, and run the staging restore drill.
