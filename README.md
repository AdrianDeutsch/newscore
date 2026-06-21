<h1 align="center">NewsCore</h1>

<p align="center">
  <img src="docs/images/banner.svg" alt="NewsCore – cloud-natives Nachrichtenportal" width="760">
</p>

<p align="center">
  <a href=".gitlab-ci.yml"><img src="https://img.shields.io/badge/pipeline-passing-brightgreen" alt="Pipeline"></a>
  <a href="apps/api-gateway"><img src="https://img.shields.io/badge/backend%20coverage-98%25-brightgreen" alt="Backend Coverage"></a>
  <a href="apps/frontend"><img src="https://img.shields.io/badge/frontend%20coverage-95%25-brightgreen" alt="Frontend Coverage"></a>
  <img src="https://img.shields.io/badge/Nuxt-3-00DC82?logo=nuxt.js&logoColor=white" alt="Nuxt 3">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/GraphQL-BFF-E10098?logo=graphql&logoColor=white" alt="GraphQL">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="MIT License"></a>
</p>

<p align="center">
  <strong>NewsCore</strong> ist ein cloud-natives, hochverfügbares Nachrichtenportal: redaktionelle
  Inhalte aus einem CMS, ausgeliefert über eine GraphQL-API (Backend-for-Frontend) an ein
  server-seitig gerendertes Nuxt-3-Frontend — ausgelegt für High-Traffic und GitOps-Betrieb auf
  OpenShift/Kubernetes.
</p>

<p align="center">
  <img src="docs/images/hero-demo.gif" alt="Live-Demo: Startseite → Artikeldetail → Volltextsuche" width="760">
  <br>
  <em>Ein echter End-to-End-Lauf: Startseite mit Ressort-Filter → Artikeldetail → Volltextsuche —
  Nuxt 3 SSR über die GraphQL-API (auch als <a href="docs/images/hero-demo.mp4">MP4</a>).</em>
</p>

## 📑 Inhaltsverzeichnis

- [Highlights](#-highlights)
- [Screenshots](#-screenshots)
- [Features](#-features)
- [Architektur](#-architektur)
- [Quick Start](#-quick-start)
- [Teststrategie](#-teststrategie)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Projektstruktur](#-projektstruktur)
- [Defect Management](#-defect-management)
- [Architecture Decision Records](#-architecture-decision-records)
- [Roadmap](#-roadmap)

## ⭐ Highlights

- **Lauffähiger Vertical Slice in einem Befehl** — `docker compose up --build` startet GraphQL-Gateway + SSR-Frontend.
- **N+1-frei by design** — nested `author`/`category` werden per DataLoader (`@BatchMapping`) gebatcht.
- **Austauschbare Datenquelle** — Resolver kennen nur Service-Interfaces; In-Memory-Mock, **Sophora-CMS** und ElasticSearch sind reine Profil-Schalter, ohne Resolver-Änderung.
- **Hohe Testabdeckung, in CI erzwungen** — Backend **98 %**, Frontend **96 %** (Gate ≥ 80 %).
- **Dokumentierte Entscheidungen** — jede Technologiewahl als [ADR](docs/adr/).

## 📸 Screenshots

<table>
  <tr>
    <td width="33%"><img src="docs/images/screenshot-home.png" alt="Startseite"></td>
    <td width="33%"><img src="docs/images/screenshot-article.png" alt="Artikeldetail"></td>
    <td width="33%"><img src="docs/images/screenshot-search.png" alt="Suche"></td>
  </tr>
  <tr>
    <td align="center"><em>Startseite — Ressort-Filter + Artikel-Grid</em></td>
    <td align="center"><em>Artikeldetail — Breadcrumb, Autor, Cover</em></td>
    <td align="center"><em>Suche — <code>search("klima")</code> → 2 Treffer</em></td>
  </tr>
</table>

## ✨ Features

| Feature                         | Status | Technologie                              | Anmerkung |
|---------------------------------|:------:|------------------------------------------|-----------|
| GraphQL-BFF-Gateway             |   ✅   | Spring Boot 3, `spring-graphql`, Java 21 | Schema-first |
| N+1-Vermeidung                  |   ✅   | DataLoader via `@BatchMapping`           | author + category |
| Application-Cache               |   ✅   | Caffeine (`@Cacheable`)                  | [ADR-003](docs/adr/ADR-003-caffeine-caching.md) |
| SSR-Frontend                    |   ✅   | Nuxt 3, Vue 3, TypeScript                | `useAsyncData` |
| GraphQL-Client                  |   ✅   | urql (`ssrExchange`)                     | [ADR-001](docs/adr/ADR-001-graphql-client-urql.md) |
| Edge-Caching                    |   ✅   | Varnish (VCL) + Apache (TLS/gzip/Header) | [ADR-008](docs/adr/ADR-008-varnish-caching.md), event-getriebenes PURGE |
| Volltextsuche                   |   ✅   | ElasticSearch 8 (Profil-gesteuert)       | [ADR-004](docs/adr/ADR-004-elasticsearch-search.md), Testcontainers |
| CMS-Datenquelle                 |   ✅   | Sophora-HTTP-Adapter (Profil `sophora`)  | [ADR-010](docs/adr/ADR-010-sophora-cms-source.md), Default In-Memory |
| Event-Streaming                 |   ✅   | Kafka + **Avro/Schema-Registry** + DLQ   | [ADR-011](docs/adr/ADR-011-avro-schema-registry.md), EmbeddedKafka + Mock-Registry |
| Aktive Cache-Invalidierung      |   ✅   | `article.events` → ES-Reindex + Evict    | schließt den Loop zu ADR-003 |
| Analytics → PostgreSQL          |   ✅   | analytics-service (Node, kafkajs + pg)   | `search.events` + `user.events` (Pageviews) → Postgres |
| Observability-Endpoint          |   ✅   | Actuator + `/actuator/prometheus`        | Grafana folgt |
| Lokales Stack-Setup             |   ✅   | docker-compose (ES + Kafka + Postgres)   | ein Befehl |
| CI-Pipeline                     |   ✅   | GitLab CI (5 Stages)                     | Coverage-Gate |
| K8s/OpenShift + GitOps          |   ✅   | Helm-Umbrella-Chart + ArgoCD             | [ADR-006](docs/adr/ADR-006-helm-argocd-gitops.md), HPA/PDB/NetworkPolicy |
| Observability                   |   ✅   | Prometheus + Grafana + Loki              | [ADR-007](docs/adr/ADR-007-observability.md), Dashboards/Alerts/JSON-Logs |
| Distributed Tracing             |   ✅   | OpenTelemetry + Tempo (OTLP)             | [ADR-009](docs/adr/ADR-009-distributed-tracing.md), Trace↔Logs-Korrelation |

## 🏗 Architektur

```mermaid
flowchart TD
    User([Browser]) -->|HTTPS| Apache[Apache Reverse Proxy<br/>TLS · Rate-Limit]
    Apache --> Varnish[Varnish Cache]
    Varnish -->|Cache-Miss| Nuxt[Nuxt 3 SSR<br/>urql]
    Nuxt -->|GraphQL| GW[GraphQL API Gateway<br/>Spring Boot · BFF<br/>DataLoader · Caffeine]
    GW --> CMS[(Sophora CMS)]
    GW --> ES[(ElasticSearch 8)]
    GW --> PG[(PostgreSQL)]
    GW --> Kafka{{Apache Kafka}}
    Kafka --> Analytics[Analytics-Service]
    GW --> Prom[Prometheus / Grafana / Loki]

    classDef done fill:#ecfdf5,stroke:#00a862,color:#0b1220;
    classDef soon fill:#f1f5f9,stroke:#94a3b8,color:#475569,stroke-dasharray:4 3;
    class Nuxt,GW,ES,Kafka,Analytics,PG,Prom,Apache,Varnish done;
    class CMS soon;
```

Grün = in dieser Iteration umgesetzt · gestrichelt = vorgesehen (Roadmap).
Begründung der zentralen Schicht: [ADR-002 — GraphQL BFF](docs/adr/ADR-002-graphql-bff-pattern.md).

## 🚀 Quick Start

### Variante A — Komplettes Stack (Docker)

```bash
docker compose up --build
# Frontend:  http://localhost:3000
# GraphiQL:  http://localhost:8080/graphiql
```

### Variante B — Apps einzeln (lokal)

```bash
# Terminal 1 — API-Gateway (Maven Wrapper lädt Maven beim ersten Lauf)
cd apps/api-gateway && ./mvnw spring-boot:run

# Terminal 2 — Frontend
cd apps/frontend && npm install && npm run dev
```

### Hooks aktivieren (nach dem Clone)

```bash
git config core.hooksPath .githooks
chmod +x .githooks/*
```

## 🎯 Teststrategie

Strikte Testpyramide — Bulk an Unit-Tests, gezielte Integrationstests, wenige E2E (Roadmap):

```
        ┌────────────────────┐
        │   E2E (Playwright) │   🔜 Roadmap
        ├────────────────────┤
        │  Integrationstests │   GraphQlTester · Testcontainers (ES) · EmbeddedKafka
        ├────────────────────┤
        │     Unit-Tests     │   JUnit 5 · Vitest  (Bulk)
        └────────────────────┘
```

| Schicht           | Tooling                                         | Umfang                       |
|-------------------|-------------------------------------------------|------------------------------|
| Backend           | JUnit 5, Mockito, AssertJ                       | 41 Unit-Tests                |
| Backend           | `GraphQlTester`, Testcontainers (ES), EmbeddedKafka | 11 Integrationstests     |
| Frontend          | Vitest, Vue Test Utils, happy-dom               | 16 Tests                     |
| analytics-service | Vitest (gemockt: kafkajs + pg)                  | 9 Tests                      |

```bash
cd apps/api-gateway          && ./mvnw verify                        # Backend: Tests + JaCoCo-Gate
cd apps/frontend             && npm run test:unit -- --run --coverage
cd services/analytics-service && npm run test:unit -- --run --coverage
```

Coverage-Gate **≥ 80 %** in allen Modulen erzwungen (Frontend ~95 %, analytics-service 100 % der
Logik-Schicht). Flaky-Test-Policy: [FLAKY-TESTS.md](FLAKY-TESTS.md).

## 🔁 CI/CD Pipeline

GitLab CI ([.gitlab-ci.yml](.gitlab-ci.yml)) mit fünf Stages und pfadbasierten `rules`:

```
validate ─→ test ─→ build ─→ security ─→ deploy-staging ─→ deploy-prod (manuell)
 lint        unit/IT   images     trivy      ArgoCD sync       ArgoCD sync
```

- **validate** — ESLint + `type-check` (Frontend), Checkstyle (Backend)
- **test** — Unit + Integration, Coverage als Report/Artifact
- **build** — Container-Images → Registry (nur `main`/`develop`)
- **security** — Trivy-Scan, kein Merge bei HIGH/CRITICAL CVEs
- **deploy** — GitOps via ArgoCD (Staging automatisch, Prod manuelles Gate)

## 📁 Projektstruktur

```
newscore/
├── apps/
│   ├── api-gateway/        # Spring Boot 3 · GraphQL BFF · ES · Kafka (eigene README)
│   └── frontend/           # Nuxt 3 · urql · SSR (eigene README)
├── services/
│   └── analytics-service/  # Node/TS · Kafka → PostgreSQL (eigene README)
├── infrastructure/
│   ├── helm/newscore/      # Umbrella-Chart (Deploy/Svc/HPA/PDB/Ingress/NetworkPolicy/ServiceMonitor)
│   ├── argocd/             # ArgoCD Application-Manifeste (staging/prod)
│   ├── prometheus/         # Scrape-Config + Alert-Rules
│   ├── grafana/            # Provisioning (Datasources) + Dashboards
│   ├── loki/               # Promtail-Config (Log-Aggregation)
│   ├── varnish/            # default.vcl (Edge-Cache, TTLs, PURGE)
│   └── apache/             # Reverse-Proxy (TLS/gzip/Security-Header)
├── docs/
│   ├── adr/                # Architecture Decision Records
│   ├── defects/            # Defect-Template + Bugs
│   └── images/banner.svg
├── .githooks/              # pre-commit · commit-msg · pre-push
├── docker-compose.yml      # lokales Stack-Setup (ES + Kafka + Postgres)
├── .gitlab-ci.yml          # CI-Pipeline
├── CONTRIBUTING.md · FLAKY-TESTS.md · LICENSE
└── README.md
```

## 🐞 Defect Management

Bugs werden in [`docs/defects/`](docs/defects/) nach dem
[Defect-Template](docs/defects/DEFECT-TEMPLATE.md) dokumentiert (Severity, Repro-Schritte,
Root Cause, Regressionstest).

## 📐 Architecture Decision Records

| ADR | Entscheidung |
|-----|--------------|
| [ADR-001](docs/adr/ADR-001-graphql-client-urql.md) | urql als GraphQL-Client im Frontend |
| [ADR-002](docs/adr/ADR-002-graphql-bff-pattern.md) | GraphQL-Gateway nach BFF-Pattern |
| [ADR-003](docs/adr/ADR-003-caffeine-caching.md) | Caffeine als In-Process-Cache |
| [ADR-004](docs/adr/ADR-004-elasticsearch-search.md) | ElasticSearch als Volltextsuche (Profil-gesteuert) |
| [ADR-005](docs/adr/ADR-005-kafka-eventing.md) | Kafka-Eventing mit JSON-Serialisierung + DLQ |
| [ADR-006](docs/adr/ADR-006-helm-argocd-gitops.md) | Helm-Umbrella-Chart + ArgoCD-GitOps |
| [ADR-007](docs/adr/ADR-007-observability.md) | Observability mit Prometheus, Grafana und Loki |
| [ADR-008](docs/adr/ADR-008-varnish-caching.md) | Varnish-Edge-Cache + Apache-Reverse-Proxy |
| [ADR-009](docs/adr/ADR-009-distributed-tracing.md) | Distributed Tracing mit OpenTelemetry + Tempo |
| [ADR-010](docs/adr/ADR-010-sophora-cms-source.md) | Sophora-CMS als austauschbare Datenquelle |
| [ADR-011](docs/adr/ADR-011-avro-schema-registry.md) | Avro + Schema-Registry (ersetzt ADR-005 §2) |

## 🛣 Roadmap

- ✅ **Volltextsuche:** ElasticSearch-`SearchService` (profil-gesteuert, Testcontainers) — erledigt
- ✅ **Eventing (Epic 4):** Kafka-Producer/Consumer, DLQ, aktive Cache-Invalidierung, Analytics → Postgres — erledigt
- ✅ **Plattform (Epic 5):** Helm-Umbrella-Chart, ArgoCD-Apps, HPA/PDB, NetworkPolicies (OpenShift) — erledigt
- ✅ **Observability (Epic 6):** Prometheus-Scrape/Alerts, Grafana-Dashboards, JSON-Logs → Loki — erledigt
- ✅ **Caching (Epic 3):** Varnish-VCL + Apache-Proxy, event-getriebenes `PURGE` — erledigt
- ✅ **Tracing:** OpenTelemetry + Tempo (OTLP), Trace↔Logs-Korrelation — erledigt
- ✅ **Datenanbindung:** Sophora-CMS-HTTP-Adapter (Profil `sophora`), Default In-Memory — erledigt
- ✅ **Analytics-Eventing:** `user.pageview`-Producer im Frontend — erledigt
- ✅ **Eventing-Härtung:** Avro + Confluent Schema-Registry (ersetzt JSON aus ADR-005) — erledigt
- **Frontend:** GraphQL-Codegen, Playwright-E2E, i18n, PWA

## 📄 Lizenz

[MIT](LICENSE)
