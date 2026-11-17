# DEFECT-007: Nicht vorhandener Artikel antwortet mit 200

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-007                                       |
| **Status**      | behoben                                          |
| **Severity**    | major |
| **Priorität**   | P2 |
| **Komponente**  | frontend, infra (Varnish) |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Eine Artikel-URL ohne Artikel liefert HTTP 200 mit dem Text „Artikel nicht gefunden“. Varnish speichert diese Antwort wie eine Artikelseite.

## Schritte zur Reproduktion

1. `curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/article/gibt-es-nicht`

## Erwartetes Verhalten

`404`.

## Tatsächliches Verhalten

`200`. Über den Edge fünf Minuten lang aus dem Cache. Ein Fehler des Gateways zeigte denselben Text; die VCL setzte außerdem für jede Antwort eine TTL, also auch für 5xx.

## Umgebung

- lokal (Docker Desktop 29.2, macOS arm64); Stand `main` = `1d8ef9a`

## Root Cause

Die Seite hat den leeren Zustand im Template dargestellt, statt den Status der Antwort zu setzen. Die VCL überschrieb das Standardverhalten von Varnish für Fehlerantworten.

## Fix

- `pages/article/[id].vue` wirft `createError` mit 404 (kein Artikel) bzw. 502 (Gateway-Fehler); `error.vue` stellt die Fehlerseite dar.
- VCL: 5xx wird nicht gespeichert, 404 nur 30 Sekunden.

## Regressionstest

`e2e/tests/ui.spec.ts` („answers with HTTP 404 …“), `e2e/tests/edge.spec.ts` („a missing article is answered with 404 through the cache as well“), `infrastructure/varnish/tests/caching.vtc` (5xx wird nicht gespeichert).
