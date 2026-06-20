# ADR-009: Distributed Tracing mit OpenTelemetry und Tempo

- **Status:** Akzeptiert
- **Datum:** 2026-06-20
- **Entscheider:** SRE-Team
- **Kontext-Epic:** Epic 6 (Monitoring) — Tracing-Ausbau

## Kontext

Metriken (ADR-007) zeigen *dass* etwas langsam ist, aber nicht *wo* in der Request-Kette. Für die
Ursachenanalyse über Service-Grenzen (Frontend → Gateway → ES/Kafka) braucht es verteiltes Tracing.
ADR-007 hatte `traceId`/`spanId` bereits in den JSON-Logs vorgesehen.

## Entscheidung

**Micrometer Tracing → OpenTelemetry → OTLP → Grafana Tempo.**

- Das Gateway nutzt `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`. Spring Boot
  instrumentiert Web-/Client-Spans automatisch und propagiert den W3C-`traceparent`.
- Spans werden per **OTLP/HTTP** an **Tempo** (`/v1/traces`) exportiert; Tempo ist die
  Trace-Datasource in Grafana.
- **Trace↔Logs-Korrelation:** `traceId`/`spanId` stehen im JSON-Log (Loki) und im Span; die
  Grafana-Tempo-Datasource ist via `tracesToLogsV2` mit Loki verknüpft.
- **Sampling:** Default `0.0` (aus), per `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` pro Umgebung
  hochgedreht (lokal/Compose 1.0, Prod z. B. 0.1) — kein Overhead/kein Export ohne Backend.

OpenTelemetry als vendor-neutraler Standard; Tempo, weil es nahtlos in den vorhandenen
Grafana/Loki-Stack passt und kostengünstig (Objektstorage) skaliert.

## Konsequenzen

**Positiv**
- End-to-End-Latenz pro Request sichtbar; Sprung von einem langsamen Trace direkt zu seinen Logs.
- Vendor-neutral (OTLP) — Tempo gegen Jaeger/andere austauschbar.
- Kein Overhead im Default (Sampling 0), bewusst pro Umgebung aktivierbar.

**Negativ / Trade-offs**
- Zusätzliche Komponente (Tempo) im Observability-Stack.
- Sinnvolle Sampling-Rate in Prod nötig (Kosten/Volumen vs. Aussagekraft).

## Verifikation

- Backend-Build/Tests grün mit den Tracing-Abhängigkeiten (Sampling 0, kein Export).
- Lokal: Gateway mit `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0` → Requests erzeugen Spans;
  `traceId`/`spanId` erscheinen in den Logs und die Traces landen in Tempo
  (`docker compose --profile observability up`).

## Verweise

- `apps/api-gateway/src/main/resources/application.yml` (`management.tracing` / `management.otlp`)
- `infrastructure/tempo/tempo.yaml`, Grafana-Tempo-Datasource
- Verwandt: [ADR-007](ADR-007-observability.md)
