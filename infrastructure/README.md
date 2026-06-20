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

## 📊 Observability (Epic 6)

```
infrastructure/
├── prometheus/   # prometheus.yml (Scrape) + rules.yml (Alerts)
├── grafana/      # provisioning/ (Datasources) + dashboards/ (API Overview)
└── loki/         # promtail-config.yml (Log-Aggregation)
```

- **Metriken:** Gateway-`/actuator/prometheus` (mit Histogram-Buckets) → Prometheus.
  Im Cluster via `ServiceMonitor` (`monitoring.enabled=true`), lokal via Prometheus-Container.
- **Alerts:** `rules.yml` bzw. `PrometheusRule` (P95-Latenz, 5xx-Rate, Kafka-Lag, Heap, Down).
- **Dashboards:** Grafana lädt Datasources + Dashboard deklarativ (Provisioning).
- **Logs:** Gateway schreibt unter dem `json`-Profil strukturierte JSON-Logs → Promtail → Loki.

Lokal: `docker compose --profile observability up` (Grafana auf <http://localhost:3001>,
Prometheus auf <http://localhost:9090>). Details: [ADR-007](../docs/adr/ADR-007-observability.md).

## ⚡ Caching-Edge (Epic 3)

```
infrastructure/
├── varnish/default.vcl   # TTLs (Assets 24h, Artikel 5m, Start 1m), PURGE-ACL, X-Cache
└── apache/               # Reverse-Proxy: TLS, gzip, Security-Header, Rate-Limit
```

Pfad: **Client → Apache (`:8088`) → Varnish → frontend**. Event-getriebene Invalidierung: der
Gateway-Consumer sendet bei `article.*`-Events `PURGE` an Varnish (siehe
[ADR-008](../docs/adr/ADR-008-varnish-caching.md)).

Lokal: `docker compose --profile edge up` → `curl -I http://localhost:8088/` zeigt `X-Cache`
(MISS→HIT) und die Security-Header.

## 🛣 Roadmap

- ES/Kafka/Postgres als eigene Charts/Operatoren (hier als bestehende Services vorausgesetzt)
- Sealed-Secrets / Vault-Integration statt Platzhalter-Secret
- OpenTelemetry + Tempo (verteiltes Tracing)
