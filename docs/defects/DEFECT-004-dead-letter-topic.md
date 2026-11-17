# DEFECT-004: Poison-Records landen nach Update im falschen Dead-Letter-Topic

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-004                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P2 |
| **Komponente**  | api-gateway |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Mit Spring Boot 3.5 (Spring Kafka 3.3) erreichen nicht dekodierbare Nachrichten das deklarierte Topic `newscore.article.events.DLT` nicht mehr.

## Schritte zur Reproduktion

1. `spring-boot-starter-parent` auf 3.5.16 anheben.
2. `./mvnw verify`

## Erwartetes Verhalten

`KafkaEventFlowIntegrationTest.poisonRecord_goesToDeadLetterTopic` ist grün.

## Tatsächliches Verhalten

Der Test läuft in den Timeout (`No records found for topic`). Im Log: `Destination resolver returned non-existent partition newscore.article.events-dlt-1`.

## Umgebung

- `./mvnw verify` lokal und GitHub Actions (PR #50); Stand `main` = `1d8ef9a`

## Root Cause

Der `DeadLetterPublishingRecoverer` wurde ohne Ziel-Resolver erzeugt und hing damit am Bibliotheks-Default. Der hat sich von `<topic>.DLT` auf `<topic>-dlt` geändert. Derselbe Fehler ließ den Dependabot-PR #50 für dieses Update scheitern; die zweite Ursache dort war die Klassen-Freigabe von Avro 1.12 (`AvroTrustConfig`).

## Fix

Der Resolver ist ausgeschrieben: `<topic>` + `KafkaTopics.DLT_SUFFIX`, gleiche Partition.

## Regressionstest

`apps/api-gateway/src/test/java/de/newscore/integration/kafka/KafkaEventFlowIntegrationTest.java` (bestehender Test, der das Problem angezeigt hat).
