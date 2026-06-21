# ADR-010: Sophora-CMS als austauschbare Artikel-Datenquelle

- **Status:** Akzeptiert
- **Datum:** 2026-06-21
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 2 (GraphQL Gateway) — Datenanbindung

## Kontext

Die bisherigen Iterationen liefern Artikel aus einer In-Memory-Mock-Quelle. Produktiv kommen die
redaktionellen Inhalte aus dem **Sophora-CMS**. [ADR-002](ADR-002-graphql-bff-pattern.md) versprach
eine austauschbare Datenquelle hinter dem `ArticleService`-Interface — das wird hier konkret
eingelöst, ohne Resolver, Schema oder übrige Anwendung zu ändern.

## Entscheidung

Eine `SophoraArticleService`-Implementierung von `ArticleService`, **HTTP-Client gegen die Sophora-API**
(Spring `RestClient`), aktiviert über das Spring-Profil **`sophora`**:

- `findById` → `GET {base}/articles/{id}` (404 → leer),
- `findArticles` → `GET {base}/articles?category=&limit=&offset=` (Filter/Paging im CMS),
- `findAll` → voller Korpus über `findArticles`.

Eine DTO-Schicht (`SophoraArticleDto`/`SophoraPageDto`) entkoppelt den CMS-JSON-Vertrag vom
Domänenmodell. Default bleibt `InMemoryArticleService` (`@Profile("!sophora")`).

**HTTP-Adapter statt JPA/Postgres:** Sophora ist ein *entferntes* CMS — ein HTTP-Adapter ist
architektonisch treffender als Artikel in eine SQL-Tabelle zu spiegeln (Postgres dient hier der
Analytik, [ADR-005](ADR-005-kafka-eventing.md)). Zudem bleibt der Default dependency-frei: keine
DataSource-Autokonfiguration, die den lauffähigen Zero-Dependency-Stand gefährden würde.

## Konsequenzen

**Positiv**
- Resolver/Schema unverändert — nur eine neue `ArticleService`-Implementierung (gelebtes ADR-002).
- Default-Stack und Tests bleiben ohne Infrastruktur lauffähig; CMS nur unter Profil aktiv.
- Robust testbar (HTTP via `MockRestServiceServer`).

**Negativ / Trade-offs**
- Latenz/Verfügbarkeit hängen am CMS — abgefedert durch Caffeine (ADR-003) + Varnish (ADR-008) und
  die event-getriebene Invalidierung (ADR-005).
- Der konkrete Sophora-Endpunkt-Vertrag ist hier angenommen; an die reale Sophora-API anzupassen ist
  auf die DTO-Schicht begrenzt.

## Verifikation

- Unit-Tests (`SophoraArticleServiceTest`, `MockRestServiceServer`) decken Mapping, 404 und Paging ab.
- Aktivierung: `SPRING_PROFILES_ACTIVE=sophora` + `NEWSCORE_SOPHORA_BASE_URL=...`.

## Verweise

- `apps/api-gateway/src/main/java/de/newscore/cms/`, `application-sophora.yml`
- Verwandt: [ADR-002](ADR-002-graphql-bff-pattern.md), [ADR-004](ADR-004-elasticsearch-search.md)
