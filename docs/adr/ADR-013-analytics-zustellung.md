# ADR-013: Analytics-Events mindestens einmal zustellen, idempotent speichern

- **Status:** Akzeptiert
- **Datum:** 2026-11-17
- **Auslöser:** [DEFECT-003](../defects/DEFECT-003-analytics-eventverlust.md),
  [DEFECT-005](../defects/DEFECT-005-topic-partitionen.md)

## Kontext

Der analytics-service fing jeden Fehler beim Verarbeiten einer Nachricht ab und machte weiter. Das
schützt vor einer einzelnen kaputten Nachricht, verwirft aber bei einem Ausfall von PostgreSQL oder
der Schema-Registry jede Nachricht, bis der Ausfall vorbei ist.

## Entscheidung

1. **Zwei Fehlerklassen.** Eine Nachricht, die nie verarbeitbar sein wird (kein Inhalt, kein
   Confluent-Rahmen, falsche Form), wird übersprungen und geloggt. Jeder andere Fehler wird
   geworfen: kafkajs committet das Offset nicht und stellt erneut zu
   (`services/analytics-service/src/messageProcessor.ts`).
2. **Idempotentes Einfügen.** Erneute Zustellung heißt, dass dieselbe Nachricht zweimal ankommen
   kann. Jede Zeile speichert `source_topic`, `source_partition`, `source_offset` unter einem
   eindeutigen Schlüssel; eingefügt wird mit `ON CONFLICT … DO NOTHING`.
3. **Topics werden deklariert, nicht nebenbei angelegt.** Das Gateway legt sie mit ihrer
   Partitionszahl an (`KafkaTopicsConfig`). Consumer und Broker (im Compose-Stack) erzeugen keine
   Topics automatisch.

## Konsequenzen

**Positiv**
- Ein Ausfall verzögert Analytics-Zeilen, statt sie zu verlieren.
- Doppelte Zustellung erzeugt keine doppelte Zeile, ohne Transaktionen über Kafka und PostgreSQL.

**Negativ / Trade-offs**
- Eine Nachricht mit gültigem Rahmen, die sich dauerhaft nicht dekodieren lässt (unbekannte
  Schema-ID), blockiert ihre Partition. Eine Dead-Letter-Queue wie im Gateway gibt es auf der
  Node-Seite nicht.
- Drei zusätzliche Spalten und ein eindeutiger Index je Tabelle.
- Der Dienst startet nicht, solange die Topics fehlen (im Compose-Stack: bis das Gateway gesund ist).

## Verifikation

- `tests/messageProcessor.test.ts`: überspringen bzw. werfen je Fehlerklasse.
- `tests/integration/analyticsRepository.int.test.ts`: dieselbe Nachricht zweimal ergibt eine Zeile
  (PostgreSQL 16 per Testcontainers, Schema aus `sql/init.sql`).
- `e2e/tests/events.spec.ts`: Suche und Seitenaufruf bis zur Zeile in PostgreSQL.
