# NewsCore Analytics-Service

Node.js/TypeScript **Kafka-Consumer**, der `search.executed`-Events des api-gateway in **PostgreSQL**
schreibt (Epic 4). Polyglotter Consumer am selben JSON-Event-Bus — siehe
[ADR-005](../../docs/adr/ADR-005-kafka-eventing.md).

## 🔁 Datenfluss

```
GraphQL search()  ──▶  newscore.search.events (Kafka)  ──▶  analytics-service  ──▶  PostgreSQL
                          (JSON SearchExecutedEvent)         (kafkajs + pg)        search_analytics
```

## 🗂 Struktur

```
src/
├── config.ts               # Env-Konfiguration (Kafka + Postgres)
├── types.ts                # SearchExecutedEvent (Spiegel des Gateway-Events)
├── analyticsRepository.ts  # pg-Insert hinter Interface (testbar)
├── searchEventHandler.ts   # reine Parse-/Validierungslogik (Unit-getestet)
├── searchEventConsumer.ts  # kafkajs-Verdrahtung
└── index.ts                # Entry-Point (Pool + Consumer + Graceful Shutdown)
sql/init.sql                # Tabelle search_analytics
```

## 🚀 Nutzung

```bash
npm install
npm run build && npm start     # erwartet Kafka + Postgres (siehe docker-compose)
```

Konfiguration über Umgebungsvariablen (Defaults für lokal):

| Variable        | Default                    |
|-----------------|----------------------------|
| `KAFKA_BROKERS` | `localhost:9092`           |
| `KAFKA_TOPIC`   | `newscore.search.events`   |
| `KAFKA_GROUP_ID`| `analytics-service`        |
| `PGHOST`        | `localhost`                |
| `PGPORT`        | `5432`                     |
| `PGUSER` / `PGPASSWORD` / `PGDATABASE` | `newscore` |

## 🎯 Tests

```bash
npm run test:unit -- --run --coverage   # Vitest, >=80% auf der Logik-Schicht
npm run lint
```

Getestet werden die reinen Einheiten (Handler, Config, Repository) mit gemocktem `pg`-Pool —
kein laufender Broker / keine DB nötig. Die kafkajs-Verdrahtung wird über das Compose-Setup
integrativ abgedeckt.

## 🐳 Docker

Teil des Compose-Stacks (`docker compose up`); konsumiert automatisch, sobald das Gateway
`search`-Queries verarbeitet.
