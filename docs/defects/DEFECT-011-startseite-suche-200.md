# DEFECT-011: Start- und Suchseite antworten bei Gateway-Ausfall mit 200

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-011                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P2 |
| **Komponente**  | frontend |
| **Gefunden in** | `1d8ef9a` (`main`); im Branch nur für die Artikelseite behoben (DEFECT-007) |
| **Gefunden bei**| Unabhängige Prüfung des Pull Requests, 17.11.2026 |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Ist das Gateway nicht erreichbar, liefern `/` und `/search` HTTP 200. Die Suchseite behauptet dabei „Keine Treffer.“, was für den Leser falsch ist. Über den Edge speichert Varnish diese Antwort und liefert sie anstelle der letzten guten Kopie aus.

## Schritte zur Reproduktion

1. `docker compose stop api-gateway`
2. `curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/search?q=klima`

## Erwartetes Verhalten

502 und eine Fehlerseite; Varnish speichert die Antwort nicht.

## Tatsächliches Verhalten

200, „Keine Treffer.“; über `:8088` beim zweiten Aufruf `X-Cache: HIT`.

## Umgebung

- Compose-Stack, Frontend und Edge

## Root Cause

Die Startseite zeigte den Fehler als Text im Template, die Suchseite wertete den Fehler gar nicht aus. Der Fix zu DEFECT-007 hatte nur die Artikelseite umgestellt.

## Fix

Beide Seiten werfen beim Server-Rendering `createError` mit 502, wie die Artikelseite. Ein Fehler beim Nachladen im Browser (Filterwechsel) zeigt einen Hinweis; die Suchseite sagt dann „Die Suche ist gerade nicht verfügbar.“ statt „Keine Treffer.“.

## Regressionstest

`e2e/tests/gateway-down.spec.ts`: startet ein zweites Frontend aus dem Image des Stacks gegen eine Adresse, unter der kein Gateway antwortet, und erwartet 502 für `/`, `/?category=politik`, `/search?q=klima` und `/article/1`. Gegen das Image vor dem Fix schlagen die ersten drei fehl. Dass Varnish 5xx nicht speichert, prüft `infrastructure/varnish/tests/caching.vtc`.
