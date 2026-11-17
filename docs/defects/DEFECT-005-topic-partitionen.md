# DEFECT-005: Events bleiben nach dem Start minutenlang unverarbeitet

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-005                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P2 |
| **Komponente**  | analytics-service, infra (Compose) |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Je nach Startreihenfolge erscheinen Such- und Pageview-Events erst Minuten später in PostgreSQL.

## Schritte zur Reproduktion

1. `docker compose up --build` auf leeren Volumes.
2. Eine Suche ausführen, in `search_analytics` nachsehen.
3. `kafka-consumer-groups.sh --describe --group analytics-service`

## Erwartetes Verhalten

Die Zeile ist nach wenigen Millisekunden da.

## Tatsächliches Verhalten

Keine Zeile. Die Consumer-Gruppe ist nur Partition 0 zugewiesen, die Events liegen auf Partition 1 und 2.

## Umgebung

- lokal (Docker Desktop 29.2, macOS arm64); Stand `main` = `1d8ef9a`

## Root Cause

Der Analytics-Service startete vor dem Gateway. Sein `subscribe` legte die Topics über die automatische Topic-Erstellung an, mit dem Broker-Default von einer Partition. Das Gateway erhöhte danach auf drei. Die Zuweisung des Consumers blieb bis zur nächsten Metadaten-Aktualisierung (Standard: fünf Minuten) bei Partition 0.

## Fix

- Der Consumer legt keine Topics mehr an (`allowAutoTopicCreation: false`), der Broker im Compose-Stack ebenfalls nicht.
- Der Analytics-Service startet erst, wenn das Gateway gesund ist (das die Topics deklariert).

## Regressionstest

`e2e/tests/events.spec.ts`: Beide Tests warten höchstens 20 Sekunden auf genau ihre Zeile und sind in dieser Lage fehlgeschlagen.
