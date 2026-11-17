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
Alert-Regeln (heute `infrastructure/helm/newscore/files/prometheus-rules.yml`, im Cluster als `PrometheusRule`):
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

- `promtool check rules infrastructure/helm/newscore/files/prometheus-rules.yml`
- `helm template ... --set monitoring.enabled=true` rendert ServiceMonitor + PrometheusRule.
- Grafana-Dashboard ist valides JSON.
- Lokal: Prometheus scrapt das Gateway (Target „up"); das Gateway gibt unter `json`-Profil
  JSON-Logzeilen aus.

## Verweise

- `apps/api-gateway/src/main/resources/{application.yml, logback-spring.xml}`
- `infrastructure/{prometheus,grafana,loki}/`
- `infrastructure/helm/newscore/templates/{servicemonitor,prometheusrule}.yaml`
- Verwandt: [ADR-005](ADR-005-kafka-eventing.md), [ADR-006](ADR-006-helm-argocd-gitops.md)

## Nachtrag 2026-11-17

Am laufenden Stack gemessen:

- `KafkaConsumerLagHigh` fragt `kafka_consumer_fetch_manager_records_lag` ab, den Lag pro Partition.
  Eine erste Messung auf einem gerade gestarteten Stack fand 0 Serien und führte zu einem Wechsel auf
  `…_records_lag_max`. Das war falsch: Micrometer registriert die Metriken pro Partition erst bei
  seinem Abgleich alle 60 s, danach sind sie da. `…_lag_max` ist dagegen ein Maximum über ein
  Zeitfenster und im Ruhezustand NaN; ein Alarm darauf löst nie aus. Die unabhängige Prüfung des PR
  hat das gefunden; die Regel nutzt wieder `…_records_lag`. Sie deckt nur den Consumer des Gateways ab
  und sieht keinen Consumer, der gar nicht mehr fetcht; der analytics-service exportiert keine
  Metriken.
- Die `PrometheusRule` im Helm-Chart hatte eine eigene, kürzere Kopie der Regeln (4 statt 5). Beide
  nutzen jetzt `infrastructure/helm/newscore/files/prometheus-rules.yml`.
- Im Compose-Stack lief das Gateway nie mit dem Profil `json`. `stack-full.env` schaltet es ein.

Verifikation jetzt: `infrastructure/prometheus/rules.test.yml` (`promtool test rules`, CI, mit den
Labels, die das Gateway wirklich exportiert) und `e2e/tests/observability.spec.ts`: Jede Metrik, die
eine Regel oder das Dashboard abfragt, muss exportiert werden, und jede Serie einer Alarm-Metrik muss
einen Zahlenwert haben (kein NaN).
- Mit `monitoring.enabled=true` rendert das Chart eine NetworkPolicy, die Prometheus aus dem Namespace
  `monitoring.prometheusNamespace` den Scrape erlaubt; ohne sie blockierte die Default-Deny-Policy ihn.
