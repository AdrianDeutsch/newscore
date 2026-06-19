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
| Datenquelle          | austauschbar — aktuell `InMemory*Service` (Mock), später Sophora/ES   |
| Observability        | Actuator + `/actuator/prometheus`                                     |
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
| Integration       | `GraphQlTester` (`@SpringBootTest`) | `src/test/java/de/newscore/integration/` |

```bash
./mvnw test                              # alle Tests
./mvnw test -DexcludedGroups=integration # nur Unit
./mvnw test -Dgroups=integration         # nur Integration
./mvnw verify                            # + JaCoCo-Report & 80%-Gate
```

Coverage-Report: `target/site/jacoco/index.html`

## 🗂 Struktur

```
src/main/java/de/newscore/
├── NewsCoreApplication.java
├── config/         # CachingConfig (Caffeine), GraphQlConfig (DateTime-Scalar)
├── domain/         # Article, Author, Category, ArticleConnection, SearchResult (records)
├── service/        # *Service-Interfaces + InMemory*Service-Impls (Mock-Daten)
└── resolver/       # ArticleController (Query + BatchMapping), SearchController
src/main/resources/
├── application.yml
└── graphql/schema.graphqls
```

## 🐳 Docker

```bash
docker build -t newscore/api-gateway .
docker run -p 8080:8080 newscore/api-gateway
```

## 🛣 Roadmap (Folge-Iterationen)

- Sophora-/Repository-Implementierung der `*Service`-Interfaces
- ElasticSearch-`SearchService` (Epic 3)
- Kafka-Consumer für aktive Cache-Invalidierung (Epic 4)
- Spotbugs im Lint-Stage, strukturierte JSON-Logs für Loki (Epic 6)
