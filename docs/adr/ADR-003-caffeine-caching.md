# ADR-003: Caffeine als In-Process-Cache im API-Gateway

- **Status:** Akzeptiert
- **Datum:** 2026-06-19
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 2 / Epic 3 (Caching)

## Kontext

Artikel- und Listen-Queries sind lesedominiert und ändern sich nur bei Redaktions-Events.
Mehrstufiges Caching ist vorgesehen: **Varnish** als HTTP-Edge-Cache (Epic 3) und ein
zusätzlicher **Application-Level-Cache** im Gateway, um wiederholte Resolver-Berechnungen
und Backend-Calls bei Cache-Miss auf Edge-Ebene zu reduzieren.

Bewertete Optionen für den Application-Cache: **Caffeine** (in-process) vs. **Redis**
(distributed).

## Entscheidung

Für diese Iteration verwenden wir **Caffeine** via Spring Cache Abstraction
(`@Cacheable` auf `article(id)` und `articles(category, limit, offset)`).

Begründung:

- Der Edge-Cache (Varnish) trägt die Hauptlast (Ziel > 85 % Hit-Rate); der App-Cache ist
  nur eine zweite, kurzlebige Stufe → ein verteilter Cache ist hier Over-Engineering.
- Caffeine ist in-process, ohne zusätzliche Infrastruktur, mit exzellenter Performance
  (W-TinyLFU-Eviction).
- Die Spring Cache Abstraction (`@Cacheable`) hält den Code provider-agnostisch — ein
  späterer Wechsel auf Redis ist eine reine Konfigurationsänderung.

## Konsequenzen

**Positiv**
- Keine zusätzliche Infrastruktur, minimale Latenz.
- Provider-agnostischer Code dank Spring Cache Abstraction.

**Negativ / Trade-offs**
- Cache ist pro Pod-Instanz lokal → bei mehreren Replicas leicht abweichende Cache-Stände.
  Akzeptabel wegen kurzer TTL und vorgelagertem Varnish; bei Bedarf später Redis
  (reine Konfigänderung).
- Aktive Invalidierung (Kafka `article.published` → Cache evict) ist für eine spätere
  Iteration vorgesehen (Epic 4). Aktuell greift TTL-basierte Expiry.

## Konfiguration

- TTL / Max-Size konfiguriert in `apps/api-gateway/src/main/java/de/newscore/config/CachingConfig.java`
- Cache-Namen: `articles`, `article`

## Verweise

- Verwandte Entscheidung: [ADR-002](ADR-002-graphql-bff-pattern.md) (BFF-Pattern)
