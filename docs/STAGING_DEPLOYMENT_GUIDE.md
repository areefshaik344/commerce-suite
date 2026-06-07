# Staging Deployment Guide

This guide promotes a build to the staging environment. Production deployment is documented separately in `PRODUCTION_DEPLOYMENT_GUIDE.md`.

## Prerequisites

- Kubernetes cluster reachable via `kubectl`
- Container registry credentials in CI
- Postgres (HA, PITR enabled) reachable from the cluster
- Secrets backend configured (`SECRET_PROVIDER`: `env|aws|vault|azure|gcp`)
- DNS record for staging hostname

## 1. Build & push image

```bash
docker build -f deployment/docker/Dockerfile -t $REGISTRY/commerce-suite:$SHA .
docker push $REGISTRY/commerce-suite:$SHA
```

CI does this automatically in `.github/workflows/ci.yml` job `docker`.

## 2. Apply manifests

```bash
kubectl apply -f deployment/k8s/namespace.yaml
kubectl apply -f deployment/k8s/configmap.yaml
kubectl apply -f deployment/k8s/secret.yaml      # template — replace values first
kubectl apply -f deployment/k8s/deployment.yaml
kubectl apply -f deployment/k8s/service.yaml
kubectl apply -f deployment/k8s/ingress.yaml
kubectl apply -f deployment/k8s/hpa.yaml
kubectl apply -f deployment/k8s/pdb.yaml
kubectl apply -f deployment/k8s/networkpolicy.yaml
```

## 3. Verify

```bash
kubectl -n commerce-suite rollout status deploy/commerce-suite
kubectl -n commerce-suite exec deploy/commerce-suite -- curl -fsS localhost:8080/actuator/health/readiness
```

## 4. Smoke tests

Run the staging smoke suite (auth, checkout happy path, payment capture, webhook signature verify, DLQ replay endpoint).

## 5. Sign-off gate

All items in `GO_LIVE_CHECKLIST.md` sections A–E must be Done or Waived before staging is considered production-ready.
