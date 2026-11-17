# DEFECT-009: Health-Endpoint meldet DOWN, obwohl das Gateway funktioniert

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-009                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P2 |
| **Komponente**  | api-gateway |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Prüfen der README-Befehle, 17.11.2026            |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Im Standardprofil, das ohne Infrastruktur laufen soll, antwortet `GET /actuator/health` mit 503 und `DOWN`, obwohl GraphQL normal antwortet. Ein Load-Balancer oder Monitoring auf diesem Endpoint nähme das Gateway aus dem Verkehr.

## Schritte zur Reproduktion

1. `cd apps/api-gateway && ./mvnw spring-boot:run` (oder das Image ohne Umgebungsvariablen starten).
2. `curl -i http://localhost:8080/actuator/health`

## Erwartetes Verhalten

`200`, `{"status":"UP",…}`.

## Tatsächliches Verhalten

`503`. Mit `show-details=always`: Komponente `elasticsearch` `DOWN`, `Connection refused`.

## Umgebung

- lokal, `./mvnw spring-boot:run` und `newscore/api-gateway` (Default-Profil)

## Root Cause

`spring-boot-starter-data-elasticsearch` liegt immer im Classpath. Spring Boot registriert den ElasticSearch-Health-Indicator deshalb in jedem Profil, auch wenn das Profil `elasticsearch` nicht aktiv ist und die Suche im Speicher läuft. Liveness und Readiness (`/actuator/health/liveness|readiness`, im Helm-Chart verwendet) waren nicht betroffen.

Dieselbe Prüfung deckte einen zweiten Fehler auf, den erst das Update auf Spring Boot 3.5 in diesem Branch eingeführt hatte: Der Java-Client 8.18 kann die `cluster.health`-Antwort des Servers 8.13.4 nicht dekodieren (`Failed to decode response`). Suchen funktionierten weiter, der Health-Check nicht. Er kam nie auf `main`.

## Fix

- `management.health.elasticsearch.enabled` ist im Standard `false` und im Profil `elasticsearch` `true`.
- ElasticSearch-Server in Compose und Testcontainers auf 8.18.8, die Version des von Spring Boot verwalteten Clients.
- Der Compose-Healthcheck des Gateways fragt `/actuator/health` ab statt nur den Port. `docker compose up --wait` scheitert damit, wenn das Gateway nicht gesund ist.

## Regressionstest

- `apps/api-gateway/src/test/java/de/newscore/integration/HealthIntegrationTest.java` (Standardprofil: UP, ohne ElasticSearch-Komponente).
- `ElasticsearchSearchIntegrationTest.health_includesElasticsearch` (Profil `elasticsearch`: Komponente UP gegen einen echten Server).
- `e2e/tests/observability.spec.ts` („the gateway reports itself UP“): Gesamtstatus im Compose-Stack. Er sieht einen ElasticSearch-Check, der DOWN meldet, nicht aber einen abgeschalteten; das deckt `ElasticsearchSearchIntegrationTest` ab.
- Beide Java-Tests schalten `show-components` ein: Ohne das antwortet `/actuator/health/<komponente>` immer mit 404, und eine Prüfung auf „nicht vorhanden“ könnte nie fehlschlagen.
