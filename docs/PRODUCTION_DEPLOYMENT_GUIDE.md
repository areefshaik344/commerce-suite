# Production Deployment Guide

Promotes a staging-verified build into production. Read `STAGING_DEPLOYMENT_GUIDE.md` first.

## 0. Pre-flight

- `GO_LIVE_CHECKLIST.md` fully green (signed off by Eng Lead, SRE, Security, Finance, Product)
- Penetration test report attached
- Backup restore drill executed within last 30 days (`DISASTER_RECOVERY_PLAN.md` §4)
- Rollback image tag pinned (previous N-1)

## 1. Secret sourcing

Set `SECRET_PROVIDER` to one of `aws`, `vault`, `azure`, `gcp` and remove plaintext secrets from the Kubernetes `Secret` template. The active `SecretProvider` bean reads on demand and never logs values.

| Provider | Required env |
|----------|--------------|
| aws      | `AWS_REGION`, IAM role on pod via IRSA |
| vault    | `VAULT_ADDR`, `VAULT_ROLE`, sidecar/agent injector |
| azure    | `AZURE_KEY_VAULT_URL`, managed identity |
| gcp      | `GCP_PROJECT`, Workload Identity |

## 2. Cutover

```bash
kubectl -n commerce-suite set image deploy/commerce-suite app=$REGISTRY/commerce-suite:$SHA
kubectl -n commerce-suite rollout status deploy/commerce-suite --timeout=10m
```

Rolling update preserves availability (PDB `minAvailable: 2`).

## 3. Post-deploy verification

- `/actuator/health/readiness` returns UP
- `/actuator/prometheus` scraped by Prometheus
- Outbox dispatcher lag < 5 s (Grafana)
- Webhook success rate > 99%
- Payment success rate within baseline ±1%
- Admin dashboard renders and MFA challenge fires for admin login

## 4. Rollback

```bash
kubectl -n commerce-suite rollout undo deploy/commerce-suite
```

Forward-only migrations: no DB rollback. If a migration is incompatible, re-deploy the previous image and apply a compensating migration in the next release.

## 5. Communication

- Update status page
- Notify vendors via in-app banner (CMS)
- Post in #incidents if any 5xx blip exceeds 0.5% during cutover
