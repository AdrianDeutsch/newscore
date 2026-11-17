# DEFECT-010: Analytics verwirft nach einem Neustart neue Ereignisse als Duplikate

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-010                                       |
| **Status**      | behoben                                          |
| **Severity**    | critical |
| **Priorität**   | P1 |
| **Komponente**  | analytics-service |
| **Gefunden in** | Branch `feat/verified-stack` (eigener Fix zu DEFECT-003), nie auf `main` |
| **Gefunden bei**| Unabhängige Prüfung des Pull Requests, 17.11.2026 |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Der Fix zu [DEFECT-003](DEFECT-003-analytics-eventverlust.md) machte das Speichern idempotent über den Schlüssel *(Topic, Partition, Offset)*. Nach einem Neustart des Compose-Stacks ohne `-v` beginnen die Kafka-Offsets wieder bei 0, PostgreSQL behält aber seine Zeilen. Neue Ereignisse treffen dann auf die Koordinaten alter Zeilen und werden still als Duplikat verworfen.

## Schritte zur Reproduktion

1. Stack starten, einige Suchen ausführen (Zeilen mit Offsets 0…n auf einer Partition).
2. `docker compose down` (ohne `-v`), dann wieder `up --wait`.
3. Eine Suche ausführen, die auf derselben Partition landet.

## Erwartetes Verhalten

Eine neue Zeile.

## Tatsächliches Verhalten

Keine Zeile. Die Consumer-Gruppe zeigt die Nachricht als verarbeitet (Lag 0); `ON CONFLICT DO NOTHING` hat sie wegen der alten Zeile mit denselben Koordinaten verworfen. `e2e/tests/events.spec.ts` schlug fehl.

## Umgebung

- Compose-Stack: Kafka ohne Volume (Daten im Container), PostgreSQL mit Volume `pgdata`

## Root Cause

Die Koordinaten einer Kafka-Nachricht sind nur eindeutig, solange das Topic lebt. Wird es neu angelegt (Broker ohne persistenten Speicher, gelöschtes Topic), beginnen die Offsets von vorn. Der Schlüssel muss zum Ereignis gehören, nicht zu seiner Position im Log.

## Fix

- `SearchExecutedEvent` und `PageViewEvent` tragen eine `eventId` (UUID), die das Gateway beim Erzeugen vergibt. Das Avro-Feld hat den Default `""`, damit das Schema rückwärtskompatibel bleibt.
- Der eindeutige Schlüssel in PostgreSQL ist `event_id`. Topic, Partition und Offset bleiben als Herkunftsangabe gespeichert, ohne Eindeutigkeit.
- Nachrichten ohne `eventId` (vor der Schemaänderung geschrieben) bekommen einen aus den Koordinaten abgeleiteten Schlüssel.

## Regressionstest

- `services/analytics-service/tests/integration/analyticsRepository.int.test.ts` („stores a new event that arrives under the coordinates of an old one“) gegen PostgreSQL. Mit einem Unique-Constraint auf den Koordinaten schlägt er fehl.
- `apps/api-gateway/src/test/java/de/newscore/unit/kafka/EventIdTest.java` (jede Instanz eine eigene UUID) und die erweiterten Assertions in `KafkaEventPublisherTest` und `KafkaEventFlowIntegrationTest` (die `eventId` kommt im Avro-Datensatz an).
- Manuell nachgestellt am 17.11.2026: Stack ohne `-v` neu gestartet, danach `e2e/tests/events.spec.ts` grün.
