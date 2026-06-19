# ADR-002: GraphQL-API-Gateway als Backend for Frontend (BFF)

- **Status:** Akzeptiert
- **Datum:** 2026-06-19
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 2 (GraphQL API Gateway)

## Kontext

Das Frontend benötigt aggregierte, view-orientierte Daten (Artikel inkl. Author und
Category, Suchergebnisse, paginierte Listen). Die Quellsysteme (Sophora CMS,
ElasticSearch, PostgreSQL) haben heterogene APIs und Datenmodelle. Ein direkter Zugriff
des Frontends auf diese Systeme würde Kopplung, Over-/Under-Fetching und verteilte
Aggregationslogik erzeugen.

## Entscheidung

Wir führen ein **GraphQL-API-Gateway nach dem BFF-Pattern** ein
(Spring Boot 3 + `spring-graphql`, Java 21). Das Gateway ist die **einzige** API-Oberfläche
für das Frontend und aggregiert die Backend-Systeme.

Architekturprinzipien:

- **Schema-first**: `schema.graphqls` ist der Vertrag (`Article`, `Author`, `Category`,
  `SearchResult`, `ArticleConnection`, `Query`).
- **Dependency Inversion / SoC**: Resolver kennen nur **Service-Interfaces**
  (`ArticleService`, `SearchService`). Die Datenquelle ist austauschbar — aktuell eine
  In-Memory-Mock-Implementierung, später eine Sophora-/Repository-Implementierung, ohne
  dass Resolver geändert werden müssen.
- **N+1-Vermeidung**: Nested Felder (`author`, `category`) werden per
  `@BatchMapping` (DataLoader) aufgelöst statt pro Artikel einzeln.
- **Caching**: häufige Queries via Caffeine (`@Cacheable`), siehe
  [ADR-003](ADR-003-caffeine-caching.md).

## Konsequenzen

**Positiv**
- Frontend hat eine stabile, typsichere, aggregierte API.
- Quellsysteme bleiben hinter dem Gateway gekapselt; Migrationen sind transparent.
- N+1-Problematik wird auf Gateway-Ebene zentral gelöst.

**Negativ / Trade-offs**
- Zusätzliche Netzwerk- und Betriebs-Schicht (durch Caching/Varnish abgefedert).
- Das Gateway kann zum „God-Service" werden — dem begegnen wir mit strikter
  Service-Schicht-Trennung und klaren Schema-Grenzen.

## Verweise

- Schema: `apps/api-gateway/src/main/resources/graphql/schema.graphqls`
- Resolver: `apps/api-gateway/src/main/java/de/newscore/resolver/`
- Services: `apps/api-gateway/src/main/java/de/newscore/service/`
