# NewsCore Infrastructure (Helm + ArgoCD)

Kubernetes/OpenShift-Deployment von NewsCore nach dem **GitOps**-Prinzip (Epic 5).
Begründung: [ADR-006](../docs/adr/ADR-006-helm-argocd-gitops.md).

```
infrastructure/
├── helm/newscore/            # Umbrella-Chart (api-gateway + frontend)
│   ├── Chart.yaml
│   ├── values.yaml           # Defaults
│   ├── values-staging.yaml   # Overrides Staging
│   ├── values-prod.yaml      # Overrides Prod
│   └── templates/            # Deployment, Service, HPA, PDB, ConfigMap, Secret,
│                             # Ingress, NetworkPolicy, ServiceAccount
└── argocd/
    ├── newscore-staging.yaml # Application (develop → auto-sync)
    └── newscore-prod.yaml    # Application (main → manuelles Gate)
```

## ✨ Was das Chart erzeugt

Pro Komponente (`api-gateway`, `frontend`) via `range`-Schleife (DRY):
`Deployment` · `Service` · `ConfigMap` · `HorizontalPodAutoscaler` · `PodDisruptionBudget`.
Geteilt: `Ingress` (Frontend), `Secret` (Platzhalter), `ServiceAccount`, drei `NetworkPolicy`
(Default-Deny + erlaubte Flows).

| Aspekt              | Umsetzung                                                              |
|---------------------|-----------------------------------------------------------------------|
| Hochverfügbarkeit   | HPA (CPU) + PDB (`minAvailable`) + Rolling Updates                     |
| Health              | Readiness/Liveness — Gateway via Actuator-Health-Groups, Frontend `/` |
| Sicherheit          | restricted-v2-`securityContext` (runAsNonRoot, drop ALL, seccomp)     |
| Netzwerk            | Default-Deny-Ingress; nur Frontend→Gateway & Ingress→Frontend         |
| Config/Secrets      | `ConfigMap` (nicht-sensitiv) + `Secret` (prod via Vault)              |

## 🚀 Nutzung

```bash
# Lint + Render
helm lint infrastructure/helm/newscore
helm template newscore infrastructure/helm/newscore -f infrastructure/helm/newscore/values-staging.yaml

# Manuelles Install/Upgrade (normalerweise macht das ArgoCD)
helm upgrade --install newscore infrastructure/helm/newscore \
  -n newscore-staging --create-namespace \
  -f infrastructure/helm/newscore/values-staging.yaml \
  --set global.imageTag=$CI_COMMIT_SHA
```

## 🔁 GitOps-Flow

```
git push develop ──► ArgoCD (newscore-staging) ──► auto-sync ──► newscore-staging Namespace
git push main    ──► ArgoCD (newscore-prod)    ──► manuelles Sync-Gate ──► newscore-prod Namespace
```

ArgoCD-Apps registrieren:

```bash
kubectl apply -f infrastructure/argocd/newscore-staging.yaml
kubectl apply -f infrastructure/argocd/newscore-prod.yaml
```

## 🛣 Roadmap

- ES/Kafka/Postgres als eigene Charts/Operatoren (hier als bestehende Services vorausgesetzt)
- Sealed-Secrets / Vault-Integration statt Platzhalter-Secret
- ServiceMonitor (Prometheus-Operator) + Grafana-Dashboards (Epic 6)
