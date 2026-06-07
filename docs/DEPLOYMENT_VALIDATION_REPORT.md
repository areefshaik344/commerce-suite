# Deployment Validation Report

_Phase 10 — Deployment + infrastructure consistency_

## 1. Container image

| Check | Result |
|---|---|
| Multi-stage Dockerfile (build → runtime) | ✅ |
| Base image `eclipse-temurin:21-jre-alpine` (LTS) | ✅ |
| Non-root user (`app:app`) | ✅ |
| `tini` PID 1 (signal forwarding) | ✅ |
| `HEALTHCHECK` hits `/actuator/health/liveness` | ✅ |
| `JAVA_OPTS` honours container memory (`MaxRAMPercentage=75`) | ✅ |
| `.dockerignore` present (build context lean) | ✅ |

## 2. docker-compose

| File | Purpose | Verdict |
|---|---|---|
| `docker-compose.yml` | Dev: app + postgres + adminer | ✅ |
| `docker-compose.prod.yml` | Prod-style: resource limits, restart policy, secret env | ✅ |

Startup order (`depends_on` + healthchecks) verified — app waits for Postgres ready before Flyway migrations run.

## 3. Kubernetes manifests

| Manifest | Verdict | Notes |
|---|---|---|
| `namespace.yaml` | ✅ | `commerce-suite` namespace |
| `configmap.yaml` | ✅ | Non-secret app config |
| `secret.yaml` | ✅ | Template only — populated via secret provider in real envs |
| `deployment.yaml` | ✅ | 3 replicas, rolling strategy, liveness+readiness probes, resource req/lim |
| `service.yaml` | ✅ | ClusterIP port 8080 |
| `ingress.yaml` | ✅ | TLS + host routing |
| `hpa.yaml` | ✅ | CPU 70% target, min 3 / max 10 |
| `pdb.yaml` | ✅ | minAvailable 2 (matches HPA min) |
| `networkpolicy.yaml` | ✅ | Egress restricted to DB + webhook destinations |

Selector/label consistency verified across Deployment → Service → HPA → PDB.

## 4. CI pipeline (`.github/workflows/ci.yml`)

| Job | Verdict |
|---|---|
| `backend` (compile + test) | ✅ — gradlew committed + executable, Testcontainers via Docker on runner |
| `frontend` (vitest + build) | ✅ |
| `docker` (image build) | ✅ — gated on backend + frontend |

## 5. Security workflow (`.github/workflows/security.yml`)

- Trivy filesystem scan, weekly + manual.
- Severity gate HIGH/CRITICAL with `ignore-unfixed`.
- ⚠ Not currently a PR-required check.

## 6. Configuration surface

| Concern | Source of truth |
|---|---|
| App config | `application.yml` with env placeholders |
| Test config | `application-test.yml` (provides `JWT_SECRET`) |
| Runtime secrets | `SecretProvider` (env / aws / vault / azure / gcp) |
| Migrations | `db/migration/V001..V018` |
| Feature flags | `application.yml` `app.*.enabled` toggles |

## 7. Observability

| Capability | Verdict |
|---|---|
| Liveness `/actuator/health/liveness` | ✅ |
| Readiness `/actuator/health/readiness` (+ custom indicators) | ✅ |
| Prometheus `/actuator/prometheus` | ✅ |
| Business metrics (`BusinessMetrics`) | ✅ |
| Structured JSON logs in prod profile | ✅ |

## 8. Disaster Recovery

- Documented in `DISASTER_RECOVERY_PLAN.md`.
- RPO 15m, RTO 60m.
- ⚠ Restore drill outstanding.

## 9. Risks

| Severity | Item | Owner |
|---|---|---|
| Medium | DR restore drill not executed | Ops |
| Medium | Load/soak test pending | QA |
| Low | Security scans not gating PRs | DevSecOps |
| Low | Reconciliation cron not scheduled in staging | Finance Ops |

## 10. Deployment readiness scorecard

| Dimension | Score |
|---|---|
| Container image | 9 |
| Compose stack | 9 |
| Kubernetes manifests | 9 |
| CI/CD | 8.5 |
| Observability | 8.5 |
| Secrets management | 9 |
| DR posture | 7 |
| **Composite** | **8.6 / 10** |

## Verdict

**STAGING VALIDATION PASSED WITH RISKS** — infrastructure is internally
consistent and deployable to staging today. Promotion to production is
gated on the four operational items above, none of which require code
changes.