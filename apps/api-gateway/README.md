# NewsCore API Gateway

GraphQL-Backend-for-Frontend für NewsCore: Spring Boot 3.5, Java 21, `spring-graphql`. Liefert
Artikel, Autoren, Ressorts und die Suche an das Nuxt-Frontend, veröffentlicht Ereignisse nach Kafka
und hält Suchindex und Caches aktuell, wenn Artikel-Ereignisse eintreffen.

Architektur: [ADR-002](../../docs/adr/ADR-002-graphql-bff-pattern.md) ·
Caching: [ADR-003](../../docs/adr/ADR-003-caffeine-caching.md)

## Was drin ist

| Thema | Umsetzung |
|---|---|
| GraphQL-Schema | `Article`, `Author`, `Category`, `ArticleConnection`, `SearchResult` (`src/main/resources/graphql/schema.graphqls`) |
| N+1-Vermeidung | `@BatchMapping` für `Article.author` und `Article.category`: eine Liste von Artikeln kostet je einen Aufruf, nicht einen pro Artikel (`BatchLoadingIntegrationTest`) |
| Datenquelle | In-Memory (Standard, 8 Demo-Artikel in 4 Ressorts) oder HTTP-Adapter für ein CMS (Profil `sophora`, [ADR-010](../../docs/adr/ADR-010-sophora-cms-source.md)) |
| Cache | Caffeine (`@Cacheable`) an beiden Datenquellen, 5 min TTL, Hit-/Miss-Zähler auf `/actuator/prometheus` |
| Suche | In-Memory (Standard) oder ElasticSearch (Profil `elasticsearch`, [ADR-004](../../docs/adr/ADR-004-elasticsearch-search.md)) |
| Ereignisse | Profil `kafka`: Avro über die Confluent Schema Registry, Dead-Letter-Topic für nicht lesbare Nachrichten ([ADR-011](../../docs/adr/ADR-011-avro-schema-registry.md)) |
| Aktualisierung | Ein Artikel-Ereignis leert die Caffeine-Caches, indiziert den Artikel neu bzw. löscht ihn aus dem Index und schickt `PURGE` an Varnish ([ADR-008](../../docs/adr/ADR-008-varnish-caching.md)) |
| CMS-Simulator | `POST /internal/cms/articles/{id}/{publish\|update\|delete}` erzeugt ein Artikel-Ereignis, wie es ein CMS-Webhook täte |
| Pageviews | `POST /events/pageview` → Kafka. Angenommen werden nur seitenrelative Pfade aus URL-Zeichen (wie der Router sie schickt) bis 2048 Zeichen |
| Ereignis-IDs | Such- und Seitenaufruf-Ereignisse bekommen beim Erzeugen eine UUID; der analytics-service speichert damit idempotent ([ADR-013](../../docs/adr/ADR-013-analytics-zustellung.md)) |
| Observability | Actuator, Prometheus-Endpoint mit Histogrammen, JSON-Logs im Profil `json`, Traces per OTLP ([ADR-007](../../docs/adr/ADR-007-observability.md), [ADR-009](../../docs/adr/ADR-009-distributed-tracing.md)) |

### Profile

| Profil | Wirkung |
|---|---|
| (keins) | Läuft ohne Infrastruktur: In-Memory-Daten und -Suche, Ereignisse werden verworfen |
| `elasticsearch` | Suche über ElasticSearch; Index wird beim Start und bei Artikel-Ereignissen aufgebaut |
| `kafka` | Ereignisse über Kafka; Consumer für Artikel-Ereignisse |
| `sophora` | Artikel über den CMS-HTTP-Adapter statt In-Memory |
| `json` | Eine JSON-Zeile pro Log-Eintrag, mit `traceId`/`spanId` |

Der Compose-Stack startet das Gateway mit `elasticsearch,kafka`, mit `stack-full.env` zusätzlich mit
`json` und Tracing.

## Starten

```bash
./mvnw spring-boot:run
```

- GraphiQL: <http://localhost:8080/graphiql>
- Prometheus-Metriken: <http://localhost:8080/actuator/prometheus>

Der Maven-Wrapper lädt Maven beim ersten Lauf; Java 21 muss installiert sein.

Beispiel-Query:

```graphql
query {
  articles(limit: 3) {
    totalCount
    hasNextPage
    nodes { id title author { name } category { slug } publishedAt }
  }
}
```

## Tests

| Ebene | Anzahl | Werkzeuge | Ort |
|---|---|---|---|
| Unit | 76 | JUnit 5, Mockito, AssertJ | `src/test/java/de/newscore/unit/` |
| Integration | 16 | `GraphQlTester`, Testcontainers (ElasticSearch 8.18), EmbeddedKafka mit Mock-Schema-Registry, `MockRestServiceServer` | `src/test/java/de/newscore/integration/` |

```bash
./mvnw verify                                   # alle Tests, JaCoCo-Bericht und 80-%-Grenze (Zeilen)
./mvnw test -DexcludedGroups=integration        # nur Unit
./mvnw test -Dgroups=integration                # nur Integration
./mvnw checkstyle:check
```

Bericht: `target/site/jacoco/index.html`. Die CI schreibt die gemessenen Zahlen in die
Zusammenfassung jedes Laufs.

Der ElasticSearch-Test braucht Docker (`@Testcontainers(disabledWithoutDocker = true)`); ohne Docker
wird er übersprungen. Mit Docker läuft er lokal und in der CI.

## Struktur

```
src/main/java/de/newscore/
├── config/      CachingConfig (Caffeine), GraphQlConfig (DateTime-Scalar)
├── domain/      Article, Author, Category, ArticleConnection, SearchResult (Records)
├── service/     *Service-Interfaces + In-Memory-Implementierungen
├── cms/         SophoraArticleService (HTTP-Adapter, Profil "sophora") + DTOs
├── search/      ArticleDocument, Repository, ElasticsearchSearchService, ArticleIndexer
├── kafka/       Ereignisse, EventPublisher (NoOp/Kafka), Consumer, AvroEventMapper,
│                Dead-Letter-Konfiguration, AvroTrustConfig
├── cache/       CachePurger (NoOp / HTTP-PURGE an Varnish)
├── web/         CmsSimulatorController, PageViewController (REST)
└── resolver/    ArticleController (Query + BatchMapping), SearchController
src/main/avro/   Avro-Schemas → Java-Klassen per avro-maven-plugin
src/main/resources/
├── application.yml                 Standardprofil
├── application-{elasticsearch,kafka,sophora}.yml
├── logback-spring.xml              Profil "json"
└── graphql/schema.graphqls
```

## Docker

Das Image baut der Compose-Stack (`docker compose up --build` im Repository-Root). Einzeln:

```bash
docker build -t newscore/api-gateway .
docker run -p 8080:8080 newscore/api-gateway
```

Ohne Umgebungsvariablen läuft es im Standardprofil.

## Nicht umgesetzt

- Der CMS-Adapter hat keine eigenen Timeouts und kein festgelegtes Verhalten bei 5xx des CMS;
  `findAll` lädt höchstens 1000 Artikel.
- Keine Authentifizierung. `/internal/cms/**` ist im Compose-Stack von außen erreichbar.
- Statische Analyse über Checkstyle hinaus (etwa SpotBugs).
