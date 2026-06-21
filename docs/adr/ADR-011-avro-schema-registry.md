# ADR-011: Avro + Confluent Schema Registry für Kafka-Events

- **Status:** Akzeptiert — **ersetzt** die JSON-Entscheidung (§2) aus [ADR-005](ADR-005-kafka-eventing.md)
- **Datum:** 2026-06-21
- **Entscheider:** Architektur-Team
- **Kontext-Epic:** Epic 4 (Kafka Event-Streaming) — Härtung

## Kontext

ADR-005 wählte bewusst **JSON** als Serialisierung für die erste Iteration und nannte **Avro +
Schema-Registry** als sauber zu migrierenden Produktionsschritt. JSON erzwingt kein Schema: Producer
und Consumer (Java-Gateway, Node-Analytics) können unbemerkt auseinanderlaufen, und es gibt keine
Kompatibilitätsprüfung bei Schema-Änderungen. Diese Härtung wird hier umgesetzt.

## Entscheidung

**Apache Avro** als Serialisierung, **Confluent Schema Registry** als zentrales Schema-Verzeichnis.

- **Schemas** liegen versioniert als `.avsc` im Gateway (`apps/api-gateway/src/main/avro/`):
  `ArticleEvent`, `SearchExecutedEvent`, `PageViewEvent` (Namespace `de.newscore.kafka.avro`).
  Das `avro-maven-plugin` generiert daraus die Java-Klassen.
- **Gateway (Producer/Consumer):** `KafkaAvroSerializer`/`KafkaAvroDeserializer` (Confluent) gegen
  die Registry. Avro ist auf die Kafka-Grenze begrenzt — ein `AvroEventMapper` übersetzt zwischen den
  Domänen-Records und den Avro-Klassen, sodass Controller und das `EventPublisher`-Interface
  unverändert bleiben.
- **analytics-service (Node):** `@kafkajs/confluent-schema-registry` dekodiert die Avro-Nachrichten
  über dieselbe Registry → sprachneutraler, schema-gestützter Konsum.
- **DLQ** bleibt erhalten: `ErrorHandlingDeserializer` um den Avro-Deserializer → Poison-Records auf
  `<topic>.DLT` statt Partition-Blockade.

## Konsequenzen

**Positiv**
- **Erzwungenes Schema** + Kompatibilitätsprüfung (Registry) — Producer/Consumer können nicht mehr
  stillschweigend divergieren; sichere Schema-Evolution.
- Kompaktere, schnellere Binär-Serialisierung als JSON.
- Sprachneutral: Java-Producer ↔ Node-Consumer teilen sich dieselben Schemas.

**Negativ / Trade-offs**
- Zusätzliche Infrastruktur (Schema-Registry) — im Compose-Stack enthalten.
- Build-Schritt (Codegen) + Confluent-Maven-Repo.
- Weniger ad-hoc-lesbar als JSON (Tooling/Registry nötig zum Inspizieren).

## Tests & Betrieb

- **EmbeddedKafka + Mock-Registry** (`mock://`): `KafkaEventFlowIntegrationTest` läuft **in-JVM ohne
  Docker/Registry** und deckt Avro-Producer, Cache-Eviction und DLQ ab → Teil von `./mvnw verify`.
- Unit-Tests für `AvroEventMapper`/Producer/Consumer und die Node-Handler.
- Lokal: `docker compose up` startet `schema-registry` (Port 8081); Gateway und analytics-service
  sind via `SCHEMA_REGISTRY_URL` verdrahtet.

## Verweise

- `apps/api-gateway/src/main/avro/`, `de.newscore.kafka.AvroEventMapper`, `application-kafka.yml`
- `services/analytics-service/src/eventConsumer.ts`
- Ersetzt §2 von [ADR-005](ADR-005-kafka-eventing.md); verwandt: [ADR-002](ADR-002-graphql-bff-pattern.md)
