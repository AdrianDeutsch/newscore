# DEFECT-008: Uhrzeit eines Artikels hängt vom Ort des Renderns ab

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-008                                       |
| **Status**      | behoben                                          |
| **Severity**    | minor |
| **Priorität**   | P3 |
| **Komponente**  | frontend |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Die Veröffentlichungszeit wird auf dem Server in dessen Zeitzone (im Container UTC) und im Browser in der des Lesers formatiert.

## Schritte zur Reproduktion

1. Artikelseite direkt aufrufen (`/article/2`) mit einem Browser, dessen Zeitzone nicht UTC ist.
2. Konsole ansehen.

## Erwartetes Verhalten

Dieselbe Uhrzeit auf Server und Client, keine Hydration-Warnung.

## Tatsächliches Verhalten

`Hydration completed but contains mismatches.`; die Zeit springt nach dem Laden um.

## Umgebung

- Chromium (Playwright), Zeitzone Europe/Vienna, Frontend-Container in UTC

## Root Cause

`Intl.DateTimeFormat` ohne `timeZone` in `ArticleCard.vue` und `pages/article/[id].vue`.

## Fix

`utils/formatDate.ts` formatiert fest in `Europe/Berlin`.

## Regressionstest

`apps/frontend/tests/unit/utils/formatDate.test.ts` (die Unit-Tests laufen mit `TZ=America/New_York`) und `e2e/tests/ui.spec.ts` („an article opened by its address hydrates cleanly …“; der Browser der Suite läuft ebenfalls in einer fremden Zeitzone).
