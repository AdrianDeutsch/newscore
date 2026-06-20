# NewsCore API Gateway

GraphQL **Backend-for-Frontend (BFF)** für NewsCore — Spring Boot 3, Java 21, `spring-graphql`.
Aggregiert redaktionelle Inhalte und liefert sie typsicher an das Nuxt-Frontend.

> Architektur-Begründung: [ADR-002](../../docs/adr/ADR-002-graphql-bff-pattern.md) ·
> Caching: [ADR-003](../../docs/adr/ADR-003-caffeine-caching.md)

## ✨ Was drin ist

| Thema                | Umsetzung                                                              |
|----------------------|-----------------------------------------------------------------------|
| GraphQL-Schema       | `Article`, `Author`, `Category`, `ArticleConnection`, `SearchResult`  |
| N+1-Vermeidung       | `@BatchMapping` (DataLoader) für `Article.author` / `Article.category` |
| Caching              | Caffeine via `@Cacheable` (`article`, `articles`)                     |
| Volltextsuche        | ElasticSearch (Profil `elasticsearch`), In-Memory als Default ([ADR-004](../../docs/adr/ADR-004-elasticsearch-search.md)) |
| Event-Streaming      | Kafka (Profil `kafka`): `search.executed`-Producer, `article.*`-Consumer, DLQ ([ADR-005](../../docs/adr/ADR-005-kafka-eventing.md)) |
| Aktive Invalidierung | `article.events` → ES-Reindex + Cache-Evict; CMS-Simulator `POST /internal/cms/articles/{id}/publish` |
| Datenquelle          | austauschbar — `*Service`-Interfaces; Mock heute, Sophora später      |
| Observability        | Actuator + `/actuator/prometheus` (Histogram-Buckets); JSON-Logs im `json`-Profil ([ADR-007](../../docs/adr/ADR-007-observability.md)) |
| Custom Scalar        | `DateTime` (→ `java.time.OffsetDateTime`)                             |

## 🚀 Quick Start

```bash
# Build + alle Tests + Coverage-Gate (>=80%)
./mvnw verify

# Lokal starten
./mvnw spring-boot:run
```

- GraphiQL: <http://localhost:8080/graphiql>
- Prometheus-Metriken: <http://localhost:8080/actuator/prometheus>

> Maven ist nicht erforderlich — der **Maven Wrapper** (`./mvnw`) lädt Maven beim ersten Lauf.

### Beispiel-Query

```graphql
query {
  articles(limit: 3) {
    totalCount
    hasNextPage
    nodes {
      id
      title
      author { name }
      category { slug }
      publishedAt
      tags
    }
  }
}
```

## 🎯 Teststrategie

| Ebene             | Werkzeuge                       | Ort                                  |
|-------------------|---------------------------------|--------------------------------------|
| Unit              | JUnit 5, Mockito, AssertJ       | `src/test/java/de/newscore/unit/`    |
| Integration       | `GraphQlTester`, **Testcontainers** (echtes ES) | `src/test/java/de/newscore/integration/` |

```bash
./mvnw test                              # alle Tests
./mvnw test -DexcludedGroups=integration # nur Unit
./mvnw test -Dgroups=integration         # nur Integration
./mvnw verify                            # + JaCoCo-Report & 80%-Gate
```

Coverage-Report: `target/site/jacoco/index.html`

> **Hinweis Testcontainers:** Der ElasticSearch-Integrationstest ist mit
> `@Testcontainers(disabledWithoutDocker = true)` markiert und wird **ohne laufenden Docker
> übersprungen** (die Logik ist zusätzlich durch Docker-freie Unit-Tests abgedeckt, daher bleibt das
> Coverage-Gate erfüllt). In CI mit Docker läuft er gegen echtes ElasticSearch.

## 🗂 Struktur

```
src/main/java/de/newscore/
├── NewsCoreApplication.java
├── config/         # CachingConfig (Caffeine), GraphQlConfig (DateTime-Scalar)
├── domain/         # Article, Author, Category, ArticleConnection, SearchResult (records)
├── service/        # *Service-Interfaces + InMemory*Service-Impls (Mock-Daten)
├── search/         # ArticleDocument, Repository, ElasticsearchSearchService, ArticleIndexer
├── kafka/          # Events, EventPublisher (NoOp/Kafka), Consumer, Topics, DLQ-Config
├── web/            # CmsSimulatorController (REST → article.events)
└── resolver/       # ArticleController (Query + BatchMapping), SearchController
src/main/resources/
├── application.yml             # Default-Profil (In-Memory-Suche, kein Broker)
├── application-elasticsearch.yml  # Profil "elasticsearch"
├── application-kafka.yml          # Profil "kafka"
└── graphql/schema.graphqls
```

## 🐳 Docker

```bash
docker build -t newscore/api-gateway .
docker run -p 8080:8080 newscore/api-gateway
```

## 🛣 Roadmap (Folge-Iterationen)

- ✅ ElasticSearch-`SearchService` (Volltextsuche) — erledigt
- ✅ Kafka-Eventing: `search.executed`-Producer, `article.*`-Consumer (ES-Reindex + Cache-Evict), DLQ — erledigt
- Sophora-/Repository-Implementierung der `*Service`-Interfaces
- Avro + Schema-Registry statt JSON (siehe ADR-005)
- Spotbugs im Lint-Stage, strukturierte JSON-Logs für Loki (Epic 6)
