# NewsCore Analytics-Service

Kafka-Consumer in Node.js/TypeScript. Schreibt die Such- und Seitenaufruf-Ereignisse des
api-gateway in PostgreSQL. Die Nachrichten sind Avro und werden über die Confluent Schema Registry
dekodiert ([ADR-011](../../docs/adr/ADR-011-avro-schema-registry.md)). Zustellung und Fehlerbehandlung:
[ADR-013](../../docs/adr/ADR-013-analytics-zustellung.md).

```
GraphQL search()   ──▶  newscore.search.events ──▶  analytics-service ──▶  search_analytics
Seitenaufruf       ──▶  newscore.user.events   ──▶  (kafkajs + pg)    ──▶  page_view_analytics
```

## Verhalten

- **Unverarbeitbare Nachrichten** (kein Inhalt, kein Confluent-Rahmen, falsche Form) werden
  übersprungen und geloggt.
- **Alle anderen Fehler** (PostgreSQL oder Schema Registry nicht erreichbar) werden geworfen. kafkajs
  committet das Offset dann nicht und stellt die Nachricht erneut zu.
- **Idempotent:** Jede Zeile speichert Topic, Partition und Offset der Nachricht unter einem
  eindeutigen Schlüssel; eine zweite Zustellung derselben Nachricht schreibt keine zweite Zeile.
- **Keine automatische Topic-Erstellung.** Die Topics legt das Gateway an. Fehlen sie, startet der
  Dienst nicht (im Compose-Stack wartet er deshalb auf das Gateway).

## Struktur

```
src/
├── config.ts                # Konfiguration aus Umgebungsvariablen
├── types.ts                 # Ereignis-Formen, Herkunft einer Nachricht, Ergebnis
├── messageProcessor.ts      # eine Nachricht: Rahmen prüfen, dekodieren, an den Handler, Fehlerklasse
├── searchEventHandler.ts    # Prüfung + Insert Suche
├── pageViewEventHandler.ts  # Prüfung + Insert Seitenaufruf
├── analyticsRepository.ts   # SQL (INSERT … ON CONFLICT DO NOTHING) hinter einem Interface
├── eventConsumer.ts         # kafkajs-Verdrahtung
└── index.ts                 # Einstieg, Graceful Shutdown
sql/init.sql                 # Tabellen; im Compose-Stack beim ersten Start von PostgreSQL ausgeführt
```

## Konfiguration

| Variable | Standard |
|---|---|
| `KAFKA_BROKERS` | `localhost:9092` |
| `KAFKA_SEARCH_TOPIC` | `newscore.search.events` |
| `KAFKA_PAGEVIEW_TOPIC` | `newscore.user.events` |
| `KAFKA_GROUP_ID` | `analytics-service` |
| `SCHEMA_REGISTRY_URL` | `http://localhost:8081` |
| `PGHOST` / `PGPORT` | `localhost` / `5432` |
| `PGUSER` / `PGPASSWORD` / `PGDATABASE` | `newscore` |

## Tests

```bash
npm run test:unit -- --run --coverage   # 23 Tests, Abdeckungsgrenze 80 % Zeilen
npm run test:integration                # 4 Tests gegen PostgreSQL 16 (Testcontainers, braucht Docker)
npm run lint
npm run build
```

- **Unit:** alles in `src/` außer `index.ts` und `eventConsumer.ts`, die nur kafkajs und pg verbinden.
  PostgreSQL und die Schema Registry sind dort gemockt.
- **Integration:** das SQL des Repositorys gegen PostgreSQL, initialisiert mit derselben `sql/init.sql`
  wie im Compose-Stack. Geprüft wird unter anderem, dass dieselbe Nachricht nur einmal gespeichert wird.
- **End-to-End:** [`e2e/tests/events.spec.ts`](../../e2e/tests/events.spec.ts) prüft die ganze Kette
  vom GraphQL-Aufruf bzw. Seitenaufruf bis zur Zeile in PostgreSQL.

`package.json` legt unter `allowScripts` fest, welche Install-Skripte von Abhängigkeiten laufen
dürfen (npm 11). Abgelehnt sind `protobufjs` (Versionsprüfung) sowie `ssh2` und `cpu-features`
(optionale native Builds, über Testcontainers eingezogen).

## Starten

Teil des Compose-Stacks. Einzeln (Kafka, Schema Registry, PostgreSQL und die Topics müssen
existieren):

```bash
npm install
npm run build && npm start
```

## Bekannte Grenzen

- Eine Nachricht mit gültigem Confluent-Rahmen, deren Schema sich dauerhaft nicht laden lässt,
  blockiert ihre Partition. Eine Dead-Letter-Queue wie im Gateway gibt es hier nicht.
- Der Dienst exportiert keine Metriken; der Lag-Alarm in Prometheus deckt nur den Consumer des
  Gateways ab.
- kafkajs hat seit Februar 2023 (2.2.4) kein Release mehr. Unter Node 24 meldet es pro Verbindung
  eine `TimeoutNegativeWarning`; das Image blendet genau diese Warnung aus (`Dockerfile`).
