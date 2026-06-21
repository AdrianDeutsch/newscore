# ADR-005: Kafka-Eventing mit JSON-Serialisierung und Dead-Letter-Queue

- **Status:** Akzeptiert
- **Datum:** 2026-06-19
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 4 (Kafka Event-Streaming)

## Kontext

Abgeleitete Zustände (Suchindex, Caches, Analytics) müssen entkoppelt und ereignisgetrieben
aktuell gehalten werden, statt synchron im Request-Pfad. Benötigt werden:

- ein Event-Bus zwischen Gateway, Suchindex und Analytics,
- ein Serialisierungsformat,
- eine Fehlerstrategie für nicht verarbeitbare Nachrichten.

## Entscheidungen

### 1. Apache Kafka als Event-Bus (Stack-Vorgabe)
Topics (Key-Strategie in Klammern):

| Topic                       | Producer            | Consumer                          |
|-----------------------------|---------------------|-----------------------------------|
| `newscore.article.events`   | CMS-Simulator (key = articleId) | Gateway → ES-Reindex + Cache-Evict |
| `newscore.search.events`    | Gateway (GraphQL)   | analytics-service → PostgreSQL    |
| `newscore.article.events.DLT` | (Recoverer)       | Ops / manuelle Sichtung           |

`article.events` wird nach `articleId` partitioniert → Ordnung pro Artikel.

### 2. JSON statt Avro (für diese Iteration) — ⚠️ abgelöst durch [ADR-011](ADR-011-avro-schema-registry.md)
**JSON** über `JsonSerializer`/`JsonDeserializer` (Spring Kafka), `trusted.packages = de.newscore.kafka`.

Begründung: kein Schema-Registry-Betrieb nötig, sprachneutral lesbar (der Node-`analytics-service`
liest denselben JSON-Body), minimale Reibung. **Trade-off:** kein erzwungenes Schema / keine
Kompatibilitätsprüfung. Für Produktion ist **Avro + Schema-Registry** vorgesehen — austauschbar,
da Producer/Consumer hinter `EventPublisher` bzw. typisierten Listenern gekapselt sind.

### 3. Dead-Letter-Queue statt Partition-Blockade
`ErrorHandlingDeserializer` + `DefaultErrorHandler` mit `DeadLetterPublishingRecoverer`: nach
endlichen Retries (oder bei Deserialisierungsfehlern sofort) landen Records auf `<topic>.DLT`,
statt die Partition zu blockieren.

### 4. Profil-gesteuerte Aktivierung
Kafka-Beans (`@Profile("kafka")`); Default ist ein `NoOpEventPublisher`, damit App und Unit-Tests
ohne Broker laufen (analog ADR-004). Voller Loop: `SPRING_PROFILES_ACTIVE=kafka,elasticsearch`.

## Konsequenzen

**Positiv**
- Entkopplung: Indexierung/Cache-Invalidierung/Analytics laufen asynchron, nicht im Request-Pfad.
- Aktive Cache-Invalidierung ergänzt die TTL aus ADR-003.
- Robust gegen Poison-Messages (DLQ).
- Sprachneutrale Events → polyglotter Consumer (Node).

**Negativ / Trade-offs**
- Kein erzwungenes Schema (bis Avro/Registry folgt).
- Zusätzliche Infrastruktur (Kafka im Compose-Stack).
- Eventual Consistency zwischen Quelle und abgeleiteten Zuständen.

## Tests & Betrieb

- **EmbeddedKafka**-Integrationstest (`KafkaEventFlowIntegrationTest`) — läuft **in-JVM ohne Docker**,
  deckt Producer, Cache-Eviction und DLQ ab; daher Teil von `./mvnw verify`.
- Unit-Tests für Producer/Consumer/Strategie.
- Lokal: `docker compose up` startet Kafka; CMS-Webhook simulieren via
  `POST /internal/cms/articles/{id}/publish`.

## Verweise

- `apps/api-gateway/src/main/java/de/newscore/kafka/`
- `apps/api-gateway/src/main/resources/application-kafka.yml`
- `services/analytics-service/` (Consumer von `newscore.search.events`)
- Verwandt: [ADR-003](ADR-003-caffeine-caching.md), [ADR-004](ADR-004-elasticsearch-search.md)
