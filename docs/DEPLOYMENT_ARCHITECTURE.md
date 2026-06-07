# Deployment Architecture

Target reference architecture for staging and production.

## 1. Topology

```text
                    ┌──────────────┐
   Customers ─────► │   CDN (TLS)  │
                    └──────┬───────┘
                           ▼
                    ┌──────────────┐
                    │  Ingress /   │   WAF + rate limiting
                    │  Load Bal.   │
                    └──────┬───────┘
                           ▼
         ┌─────────────────┴──────────────────┐
         │                                    │
   ┌─────▼──────┐                      ┌──────▼─────┐
   │ Backend    │   ...horizontal      │ Backend    │
   │ Pod (JVM)  │   (HPA on CPU/RPS)   │ Pod (JVM)  │
   └─────┬──────┘                      └──────┬─────┘
         │                                    │
   ┌─────▼────────────────────────────────────▼─────┐
   │            PostgreSQL primary (HA)              │
   │              ↑ streaming replica                │
   └─────┬────────────────────────────────────┬─────┘
         │                                    │
   ┌─────▼─────┐   ┌──────────┐   ┌───────────▼────┐
   │ Redis     │   │ Object   │   │ Outbox / WHook │
   │ (cache /  │   │ Storage  │   │ Dispatcher pods│
   │ rate lim) │   │ (S3)     │   │ (separate HPA) │
   └───────────┘   └──────────┘   └────────────────┘
```

## 2. Components

| Component | Recommendation |
|-----------|----------------|
| Container runtime | OCI image (distroless or Eclipse Temurin JRE 21) |
| Orchestrator | Kubernetes 1.29+ |
| Backend | Spring Boot, stateless, 2 vCPU / 2 GiB request per pod |
| Database | Managed Postgres 15 (RDS / Cloud SQL) with PITR ≥ 7 d |
| Cache | Managed Redis 7 (Elasticache / Memorystore) |
| Object storage | S3 / GCS with SSE + lifecycle |
| CDN | CloudFront / Cloudflare in front of static + signed URLs for invoices |
| Secrets | AWS Secrets Manager / Vault, mounted via CSI |
| TLS | cert-manager + ACME (Let's Encrypt) or ACM |
| Observability | Prometheus + Grafana, Loki for logs, Tempo/Jaeger for traces |
| CI/CD | GitHub Actions → Container registry → Argo CD |

## 3. Environments

| Env | Purpose | Data |
|-----|---------|------|
| `dev`     | Lovable Cloud preview | Synthetic |
| `staging` | Pre-prod, identical infra at smaller size | Anonymised prod snapshot weekly |
| `prod`    | Live | Real |

Each environment has isolated DB, secrets, and webhook signing keys.

## 4. Networking

- Pods → DB over private subnet only.
- Outbound webhook egress via NAT with allowlistable static IP (so tenants can firewall).
- Admin endpoints behind a separate ingress with IP allowlist + mTLS optional.

## 5. Scaling guidelines

| Workload | Scale signal | Limit |
|----------|--------------|-------|
| Backend pods | RPS, CPU 70% | HPA 3 → 30 |
| Outbox dispatcher | `outbox_pending` queue depth | 2 → 10 |
| Webhook dispatcher | `webhook_pending` queue depth | 2 → 20 |
| Postgres | vertical first, then read-replica for analytics | up to db.r6g.4xl |

## 6. Build & release

```text
  PR → CI (build, unit + IT, dep scan, SAST) → image
       → staging deploy (Argo CD auto-sync)
       → smoke + load tests
       → tag release → prod deploy (manual approval)
```

## 7. Disaster posture

See `DISASTER_RECOVERY_PLAN.md`. Multi-AZ DB; daily snapshot to cross-region bucket; quarterly restore drill.
---

## Phase 9.5 deployment artifacts

- `deployment/docker/Dockerfile` — multi-stage, non-root, tini PID 1, `/actuator/health/liveness` probe
- `deployment/docker/docker-compose.yml` — local dev (Postgres + Redis + app)
- `deployment/docker/docker-compose.prod.yml` — reference for non-k8s deploys
- `deployment/k8s/` — Deployment (3 replicas, rolling, readOnlyRootFilesystem), Service (ClusterIP), Ingress (nginx + cert-manager), HPA (3–20 pods, CPU 70 / mem 80), PDB (`minAvailable: 2`), NetworkPolicy (allow ingress-nginx, egress to Postgres/Redis/443/DNS)

### Pod-level hardening

- runAsNonRoot, runAsUser 1000, drop all capabilities, allowPrivilegeEscalation false, readOnlyRootFilesystem true
- Resource requests + limits (`500m`/`768Mi` → `2`/`2Gi`)
- Prometheus annotations on pod for scrape discovery
