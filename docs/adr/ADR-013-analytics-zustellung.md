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
2. **Idempotentes Einfügen.** Erneute Zustellung heißt, dass dasselbe Ereignis zweimal ankommen
   kann. Das Gateway gibt jedem Such- und Seitenaufruf-Ereignis beim Erzeugen eine `eventId` (UUID);
   sie ist der eindeutige Schlüssel in PostgreSQL, eingefügt wird mit `ON CONFLICT (event_id) DO
   NOTHING`. `source_topic`, `source_partition` und `source_offset` werden als Herkunft gespeichert,
   aber nicht als Schlüssel: Offsets beginnen von vorn, wenn ein Topic neu angelegt wird. Die erste
   Fassung dieses ADR nahm genau diese Koordinaten als Schlüssel und verlor dadurch nach einem
   Neustart Ereignisse ([DEFECT-010](../defects/DEFECT-010-idempotenzschluessel.md)).
3. **Topics werden deklariert, nicht nebenbei angelegt.** Das Gateway legt sie mit ihrer
   Partitionszahl an (`KafkaTopicsConfig`). Consumer und Broker (im Compose-Stack) erzeugen keine
   Topics automatisch.

## Konsequenzen

**Positiv**
- Ein Ausfall verzögert Analytics-Zeilen, statt sie zu verlieren.
- Doppelte Zustellung erzeugt keine doppelte Zeile, ohne Transaktionen über Kafka und PostgreSQL.
- Der Schlüssel hängt nicht an der Lebensdauer des Topics.

**Negativ / Trade-offs**
- Eine Nachricht mit gültigem Rahmen, die sich dauerhaft nicht dekodieren lässt (unbekannte
  Schema-ID), blockiert ihre Partition. Eine Dead-Letter-Queue wie im Gateway gibt es auf der
  Node-Seite nicht.
- Vier zusätzliche Spalten und ein eindeutiger Index je Tabelle; ein neues Feld im Avro-Schema
  (mit Default `""`, Nachrichten ohne ID bekommen einen Schlüssel aus den Koordinaten).
- Kein Migrationswerkzeug: Eine Schemaänderung braucht ein frisches Volume (`docker compose down -v`).
- Der Dienst startet nicht, solange die Topics fehlen (im Compose-Stack: bis das Gateway gesund ist).

## Verifikation

- `tests/messageProcessor.test.ts`: überspringen bzw. werfen je Fehlerklasse.
- `tests/integration/analyticsRepository.int.test.ts` (PostgreSQL 16 per Testcontainers, Schema aus
  `sql/init.sql`): dasselbe Ereignis zweimal ergibt eine Zeile; ein neues Ereignis unter den
  Koordinaten eines alten ergibt eine zweite.
- `e2e/tests/events.spec.ts`: Suche und Seitenaufruf bis zur Zeile in PostgreSQL.
