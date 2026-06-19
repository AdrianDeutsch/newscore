# ADR-004: ElasticSearch als Volltextsuche, eingebunden über ein Service-Interface

- **Status:** Akzeptiert
- **Datum:** 2026-06-19
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 2 (GraphQL Gateway) — ElasticSearch-Resolver / Volltextsuche

## Kontext

Die erste Iteration lieferte eine In-Memory-Substring-Suche (`InMemorySearchService`) als Platzhalter.
Für relevanzbasierte Volltextsuche über große Artikelmengen (Akzeptanzkriterium: Suche < 300 ms)
ist ein dedizierter Suchindex nötig. Der Tech-Stack sieht **ElasticSearch 8** vor.

Die Herausforderung: ES anbinden, ohne die bestehende, abhängigkeitsfreie lokale Entwicklung und die
schnellen Unit-Tests zu beeinträchtigen.

## Entscheidung

Wir binden **ElasticSearch** über das bestehende `SearchService`-Interface ein (Strategy-Pattern,
gesteuert per Spring-Profil) — konsistent mit dem BFF-Prinzip aus [ADR-002](ADR-002-graphql-bff-pattern.md).

- **Default-Profil:** `InMemorySearchService` (`@Profile("!elasticsearch")`) — keine Infrastruktur
  nötig, schnelle Tests, sofort lauffähig.
- **`elasticsearch`-Profil:** `ElasticsearchSearchService` (`@Profile("elasticsearch")`) via Spring
  Data Elasticsearch (`ElasticsearchOperations`), analysierte Multi-Field-Suche über `title`,
  `teaser`, `body`, `tags`.
- **Indexierung:** `ArticleIndexer` (`@Profile("elasticsearch")`) baut den Index beim Start aus dem
  `ArticleService`-Korpus. In Epic 4 wird dies durch Kafka-`article.published`-Events ersetzt.
- **Repositories** sind per Default deaktiviert (`spring.data.elasticsearch.repositories.enabled=false`)
  und nur unter dem Profil aktiv.

Bewertete Alternativen: ElasticSearch (gewählt, Stack-Vorgabe, relevanzstark), PostgreSQL Full-Text
(weniger leistungsfähig bei Relevanz/Skalierung), OpenSearch (gleichwertig, aber Stack nennt ES).

## Konsequenzen

**Positiv**
- Resolver und übrige Anwendung bleiben unverändert — nur eine neue `SearchService`-Implementierung.
- Lokale Entwicklung und Unit-Tests laufen weiter ohne ES (Default-Profil).
- Echte Integration ist über Testcontainers abgesichert (`@ServiceConnection`).

**Negativ / Trade-offs**
- Zusätzliche Infrastruktur im `elasticsearch`-Profil (in `docker-compose.yml` enthalten).
- Index-Aktualität hängt aktuell am Start-Reindex; aktive Event-getriebene Indexierung folgt in Epic 4.

## Test- und Betriebshinweise

- **Unit-Tests** (Docker-frei): `ElasticsearchSearchServiceTest`, `ArticleIndexerTest`,
  `ArticleDocumentTest` mit gemockten `ElasticsearchOperations` → decken die Logik ab.
- **Integrationstest:** `ElasticsearchSearchIntegrationTest` startet echtes ES via Testcontainers
  (`@Testcontainers(disabledWithoutDocker = true)` → wird ohne Docker übersprungen, läuft in CI).
- **Lokal/Compose:** `docker compose up` startet ES und das Gateway mit `SPRING_PROFILES_ACTIVE=elasticsearch`.

## Verweise

- `apps/api-gateway/src/main/java/de/newscore/search/`
- `apps/api-gateway/src/main/resources/application-elasticsearch.yml`
- Verwandt: [ADR-002](ADR-002-graphql-bff-pattern.md), [ADR-003](ADR-003-caffeine-caching.md)
