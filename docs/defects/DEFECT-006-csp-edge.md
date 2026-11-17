# DEFECT-006: Seite hinter dem Edge-Proxy ist nicht bedienbar

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-006                                       |
| **Status**      | behoben                                          |
| **Severity**    | critical |
| **Priorität**   | P1 |
| **Komponente**  | infra (Apache), frontend |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Über Apache/Varnish (`:8088`) wird die Seite ausgeliefert und sieht richtig aus, reagiert aber nicht: Die Suche und die Ressort-Filter funktionieren nicht.

## Schritte zur Reproduktion

1. `docker compose --profile edge up --build`
2. `http://localhost:8088/` im Browser öffnen, Konsole ansehen.
3. „klima“ suchen.

## Erwartetes Verhalten

Die Suche zeigt zwei Treffer.

## Tatsächliches Verhalten

Konsole: `Executing inline script violates the following Content Security Policy directive 'script-src 'self''`, danach `Cannot read properties of undefined (reading 'app')`. Das Suchformular wird als normales `GET` abgeschickt und lädt die Startseite neu (`/?q=klima`). Mit `curl` fällt nichts auf, weil das HTML vom Server kommt.

Zusätzlich hätte die Richtlinie die GraphQL-Aufrufe des Browsers an `http://localhost:8080` blockiert (`default-src 'self'`).

## Umgebung

- Chromium (Playwright); alte Richtlinie am 17.11.2026 gegen das laufende Frontend nachgestellt

## Root Cause

Apache setzte eine feste CSP mit `script-src 'self'`. Nuxt schreibt seine Laufzeit-Konfiguration als Inline-Skript in die Seite; ohne das Skript startet die App nicht. Geprüft war der Edge nur mit `curl -I`.

## Fix

- Der Browser spricht nur noch mit der eigenen Origin: `/graphql` und `/events/pageview` sind Server-Routen des Frontends, die an das Gateway weiterreichen. Die CORS-Freigabe (`*`) am Gateway entfällt.
- Die CSP kommt vom Frontend und erlaubt genau die Inline-Skripte der Seite per SHA-256-Hash (`server/utils/csp.ts`). Ein Hash bleibt gültig, wenn die Seite aus dem Cache kommt.

## Regressionstest

`e2e/tests/ui.spec.ts` läuft im Projekt `ui-edge` vollständig über den Proxy; jeder Konsolenfehler lässt einen Test scheitern (`e2e/support/fixtures.ts`). Dazu `apps/frontend/tests/unit/server/csp.test.ts`.
