# ADR-007: Observability mit Prometheus, Grafana und Loki

- **Status:** Akzeptiert
- **Datum:** 2026-06-20
- **Entscheider:** Plattform-/SRE-Team
- **Kontext-Epic:** Epic 6 (Monitoring)

## Kontext

Der Betrieb braucht Metriken, Dashboards, Alerting und zentrale Logs, um die Akzeptanzkriterien
(API-P95 < 200 ms, Error-Rate, Consumer-Lag) zu überwachen. Der Tech-Stack gibt Grafana,
Prometheus und Loki vor.

## Entscheidungen

### 1. Metriken: Micrometer → Prometheus
Spring Boot Actuator + `micrometer-registry-prometheus` exponieren `/actuator/prometheus`.
**Histogram-Buckets** für `http.server.requests` sind aktiviert
(`management.metrics.distribution.percentiles-histogram`), damit Prometheus P50/P95/P99 per
`histogram_quantile` berechnen kann.

Scraping:
- **Lokal:** Prometheus-Container (`infrastructure/prometheus/prometheus.yml`) scrapt das Gateway.
- **Cluster:** ein **ServiceMonitor** (Prometheus-Operator) im Helm-Chart (`monitoring.enabled`).

### 2. Alerting: Prometheus-Rules / PrometheusRule
Alert-Regeln (`infrastructure/prometheus/rules.yml`, im Cluster als `PrometheusRule`):
`ApiHighLatencyP95` (>500 ms), `ApiHighErrorRate` (>1 % 5xx), `KafkaConsumerLagHigh` (>1000),
`JvmHeapPressure` (>90 %), `ApiGatewayDown`. Die Liste ist in beiden Formaten konsistent gehalten.

### 3. Dashboards: Grafana, provisioniert as Code
Datasources und Dashboards werden über das Grafana-Provisioning (`infrastructure/grafana/`)
deklarativ geladen — kein manuelles Klicken. Dashboard „NewsCore API Overview" zeigt
Request-Rate, 5xx-Error-Rate, Latenz P50/P95/P99 und JVM-Heap.

### 4. Logs: strukturiertes JSON → Loki
Unter dem **`json`-Profil** schreibt das Gateway via `logstash-logback-encoder` **eine JSON-Zeile
pro Log-Event** (inkl. `application` und MDC-Feldern `traceId`/`spanId`). Promtail entdeckt die
Container, parst das JSON und schickt die Logs an Loki; Grafana hat Loki als Datasource.

### 5. Tracing: Roadmap
OpenTelemetry + Tempo (verteiltes Tracing) sind vorgesehen; die MDC-Felder `traceId`/`spanId`
sind in den Logs bereits vorgesehen. Bewusst noch nicht umgesetzt, um den Scope zu begrenzen.

## Konsequenzen

**Positiv**
- Metriken, Alerts, Dashboards und Logs sind **as Code** versioniert und reproduzierbar.
- Lokal per `docker compose --profile observability up` demonstrierbar; im Cluster via Operator.
- Dieselben Alert-Definitionen lokal und im Cluster.

**Negativ / Trade-offs**
- JSON-Logs sind in `docker compose logs` schlechter lesbar → nur im `json`-Profil aktiv.
- ServiceMonitor/PrometheusRule benötigen die Prometheus-Operator-CRDs (`monitoring.enabled=false`
  per Default).
- Promtail wird langfristig durch Grafana Alloy abgelöst (austauschbar).

## Verifikation

- `promtool check rules infrastructure/prometheus/rules.yml`
- `helm template ... --set monitoring.enabled=true` rendert ServiceMonitor + PrometheusRule.
- Grafana-Dashboard ist valides JSON.
- Lokal: Prometheus scrapt das Gateway (Target „up"); das Gateway gibt unter `json`-Profil
  JSON-Logzeilen aus.

## Verweise

- `apps/api-gateway/src/main/resources/{application.yml, logback-spring.xml}`
- `infrastructure/{prometheus,grafana,loki}/`
- `infrastructure/helm/newscore/templates/{servicemonitor,prometheusrule}.yaml`
- Verwandt: [ADR-005](ADR-005-kafka-eventing.md), [ADR-006](ADR-006-helm-argocd-gitops.md)
