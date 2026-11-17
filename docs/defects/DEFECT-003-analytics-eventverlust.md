# DEFECT-003: Analytics-Service verwirft Events bei Ausfällen

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-003                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P1 |
| **Komponente**  | analytics-service |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Fällt PostgreSQL oder die Schema-Registry aus, gehen alle Events dieser Zeit verloren, ohne dass der Dienst stoppt oder das Offset stehen bleibt.

## Schritte zur Reproduktion

1. Code-Review von `src/eventConsumer.ts`: `eachMessage` umschließt Dekodieren und Speichern mit `try/catch` und loggt nur.
2. Nachgestellt im Unit-Test: Repository wirft `connection refused`.

## Erwartetes Verhalten

Ein vorübergehender Fehler führt zu erneuter Zustellung; nur Nachrichten, die nie verarbeitbar sind, werden übersprungen.

## Tatsächliches Verhalten

Der Fehler wird geloggt, kafkajs committet das Offset, die Nachricht ist verloren.

## Umgebung

- Unit-Test und Testcontainers (PostgreSQL 16); Stand `main` = `1d8ef9a`

## Root Cause

„Eine kaputte Nachricht darf den Consumer nicht blockieren“ war als `catch` um alles umgesetzt. Damit wurden dauerhaft unverarbeitbare Nachrichten und vorübergehende Ausfälle gleich behandelt.

## Fix

- `src/messageProcessor.ts` trennt die Fälle: ohne Avro-Rahmen oder mit falscher Form → überspringen; alles andere wird geworfen, kafkajs stellt erneut zu.
- Weil damit mindestens einmal zugestellt wird, ist das Einfügen idempotent. Der Schlüssel ist die `eventId` des Ereignisses; die erste Fassung nahm dafür Topic, Partition und Offset, was nach einem Neustart Ereignisse verwarf ([DEFECT-010](DEFECT-010-idempotenzschluessel.md)).
- Offen: Eine Nachricht mit gültigem Rahmen, aber unbekannter Schema-ID würde die Partition blockieren. Eine Dead-Letter-Queue auf der Node-Seite gibt es nicht.

## Regressionstest

`services/analytics-service/tests/messageProcessor.test.ts` („throws when PostgreSQL fails …“, „throws when the Schema Registry fails …“) und `tests/integration/analyticsRepository.int.test.ts` („stores a redelivered event only once“) gegen echtes PostgreSQL.
