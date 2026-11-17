# DEFECT-002: Cache-Invalidierung wirkungslos, PURGE von außen möglich

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-002                                       |
| **Status**      | behoben                                          |
| **Severity**    | critical |
| **Priorität**   | P1 |
| **Komponente**  | infra (Varnish, Apache), api-gateway |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Zwei Fehler am selben Mechanismus: Das PURGE des Gateways entfernt die gecachte Seite nicht, während jeder Client von außen den Cache leeren kann.

## Schritte zur Reproduktion

1. `docker compose --profile edge up --build`
2. `curl -sI http://localhost:8088/article/1` zweimal: `X-Cache: MISS`, dann `HIT`.
3. `curl -XPOST http://localhost:8080/internal/cms/articles/1/update`, danach erneut abrufen.
4. `curl -X PURGE http://localhost:8088/article/1`, danach erneut abrufen.

## Erwartetes Verhalten

Nach dem Artikel-Event ist die Seite ein `MISS`. Ein PURGE von außen wird abgelehnt.

## Tatsächliches Verhalten

- Nach dem Artikel-Event bleibt die Seite ein `HIT`. Im Compose-Stack war das Purgen gar nicht eingeschaltet. Mit einem PURGE, wie ihn das Gateway sendet (`Host: varnish`), antwortet Varnish `200 Purged`, die Seite bleibt trotzdem ein `HIT`; das Gateway hätte also Erfolg geloggt.
- Das PURGE von außen über Apache liefert `200`, die Seite ist danach ein `MISS`.

## Umgebung

- lokal (Docker Desktop 29.2, macOS arm64); Stand `main` = `1d8ef9a`

## Root Cause

- `return (purge)` entfernt nur das Objekt unter dem Hash der Anfrage, und in den Hash geht der Host-Header ein. Seiten werden unter dem öffentlichen Hostnamen gecacht (`ProxyPreserveHost On`), das Gateway spricht Varnish als `varnish` an.
- Varnish autorisiert PURGE über die Client-Adresse. Hinter Apache ist das immer die Adresse des Apache-Containers, und die liegt im erlaubten Docker-Netz.
- Geprüft war nur mit `MockRestServiceServer`: dass das Gateway ein PURGE absendet, nicht, dass Varnish danach neu lädt.

## Fix

- Invalidierung per `ban` auf den gespeicherten Pfad, unabhängig vom Host und einschließlich der Query-Varianten (`/?category=…`). Pfade mit Zeichen, die im regulären Ausdruck als Platzhalter wirken würden, werden mit 400 abgelehnt.
- Apache leitet nur `GET`, `HEAD` und `POST` weiter.
- Compose: Schalter `NEWSCORE_VARNISH_PURGE_ENABLED` für das Gateway.

## Regressionstest

`infrastructure/varnish/tests/invalidation.vtc` (varnishtest gegen einen Mock-Backend) und `e2e/tests/edge.spec.ts` („an article event invalidates …“, „nobody outside can empty the cache“).
