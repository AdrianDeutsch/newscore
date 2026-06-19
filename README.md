# NewsCore

![NewsCore Banner](docs/images/banner.svg)

[![Pipeline](https://img.shields.io/badge/pipeline-passing-brightgreen)](.gitlab-ci.yml)
[![Backend Coverage](https://img.shields.io/badge/backend%20coverage-98%25-brightgreen)](apps/api-gateway)
[![Frontend Coverage](https://img.shields.io/badge/frontend%20coverage-96%25-brightgreen)](apps/frontend)
[![Nuxt 3](https://img.shields.io/badge/Nuxt-3-00DC82?logo=nuxt.js&logoColor=white)](https://nuxt.com)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org)
[![GraphQL](https://img.shields.io/badge/GraphQL-BFF-E10098?logo=graphql&logoColor=white)](apps/api-gateway)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **NewsCore** ist ein cloud-natives, hochverfügbares Nachrichtenportal: redaktionelle Inhalte aus
> einem CMS, ausgeliefert über eine GraphQL-API (Backend-for-Frontend) an ein server-seitig
> gerendertes Nuxt-3-Frontend — ausgelegt für High-Traffic und GitOps-Betrieb auf OpenShift/Kubernetes.

## 📑 Inhaltsverzeichnis

- [Highlights](#-highlights)
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
- **Austauschbare Datenquelle** — Resolver kennen nur Service-Interfaces; Mock heute, Sophora/ElasticSearch morgen, ohne Resolver-Änderung.
- **Hohe Testabdeckung, in CI erzwungen** — Backend **98 %**, Frontend **96 %** (Gate ≥ 80 %).
- **Dokumentierte Entscheidungen** — jede Technologiewahl als [ADR](docs/adr/).

## ✨ Features

| Feature                         | Status | Technologie                              | Anmerkung |
|---------------------------------|:------:|------------------------------------------|-----------|
| GraphQL-BFF-Gateway             |   ✅   | Spring Boot 3, `spring-graphql`, Java 21 | Schema-first |
| N+1-Vermeidung                  |   ✅   | DataLoader via `@BatchMapping`           | author + category |
| Application-Cache               |   ✅   | Caffeine (`@Cacheable`)                  | [ADR-003](docs/adr/ADR-003-caffeine-caching.md) |
| SSR-Frontend                    |   ✅   | Nuxt 3, Vue 3, TypeScript                | `useAsyncData` |
| GraphQL-Client                  |   ✅   | urql (`ssrExchange`)                     | [ADR-001](docs/adr/ADR-001-graphql-client-urql.md) |
| Volltextsuche                   |   ✅   | In-Memory (ES-ready Interface)           | Mock-Stufe |
| Observability-Endpoint          |   ✅   | Actuator + `/actuator/prometheus`        | Grafana folgt |
| Lokales Stack-Setup             |   ✅   | docker-compose                           | `--profile data` für PG/ES |
| CI-Pipeline                     |   ✅   | GitLab CI (5 Stages)                     | Coverage-Gate |
| Kafka / OpenShift / Monitoring  |   🔜   | —                                        | Roadmap |

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
    class Nuxt,GW done;
    class Apache,Varnish,CMS,ES,PG,Kafka,Analytics,Prom soon;
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
        │  Integrationstests │   GraphQlTester (Backend)
        ├────────────────────┤
        │     Unit-Tests     │   JUnit 5 · Vitest  (Bulk)
        └────────────────────┘
```

| Schicht     | Tooling                          | Umfang                         |
|-------------|----------------------------------|--------------------------------|
| Backend     | JUnit 5, Mockito, AssertJ        | 21 Unit-Tests                  |
| Backend     | `GraphQlTester` (`@SpringBootTest`) | 4 Integrationstests         |
| Frontend    | Vitest, Vue Test Utils, happy-dom | 16 Tests (Komponenten + Service) |

```bash
cd apps/api-gateway && ./mvnw verify              # Backend: Tests + JaCoCo-Gate (>=80%)
cd apps/frontend    && npm run test:unit -- --run --coverage
```

Coverage aktuell: **Backend 98 %**, **Frontend 96 %**. Flaky-Test-Policy: [FLAKY-TESTS.md](FLAKY-TESTS.md).

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
│   ├── api-gateway/        # Spring Boot 3 · GraphQL BFF (eigene README)
│   └── frontend/           # Nuxt 3 · urql · SSR (eigene README)
├── docs/
│   ├── adr/                # Architecture Decision Records
│   ├── defects/            # Defect-Template + Bugs
│   └── images/banner.svg
├── .githooks/              # pre-commit · commit-msg · pre-push
├── docker-compose.yml      # lokales Stack-Setup
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

## 🛣 Roadmap

- **Epic 3 — Caching:** Varnish-VCL + Apache-Proxy, gezielte `PURGE`-Invalidierung
- **Epic 4 — Eventing:** Kafka-Topics, Schema-Registry, DLQ, EmbeddedKafka-Tests
- **Epic 5 — Plattform:** Helm-Chart, ArgoCD-Apps, HPA/PDB, NetworkPolicies (OpenShift)
- **Epic 6 — Observability:** Grafana-Dashboards, Prometheus-Alerts, Loki, OpenTelemetry
- **Datenanbindung:** Sophora-CMS-Resolver, ElasticSearch-`SearchService`
- **Frontend:** GraphQL-Codegen, Playwright-E2E, i18n, PWA

## 📄 Lizenz

[MIT](LICENSE)
