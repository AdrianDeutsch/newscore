# ADR-008: Varnish-Edge-Cache + Apache-Reverse-Proxy

- **Status:** Akzeptiert
- **Datum:** 2026-06-20
- **Entscheider:** Plattform-Team
- **Kontext-Epic:** Epic 3 (Caching)

## Kontext

Die Akzeptanzkriterien fordern TTFB < 200 ms und eine Varnish-Hit-Rate > 85 % unter Last. Vor dem
SSR-Frontend braucht es einen HTTP-Edge-Cache und einen Reverse-Proxy für TLS-Terminierung,
Kompression, Security-Header und Rate-Limiting.

## Entscheidung

Request-Pfad: **Client → Apache → Varnish → Nuxt-Frontend**.

### 1. Varnish als Edge-Cache (statt nginx-Cache)
`infrastructure/varnish/default.vcl` (VCL 4.1):
- **TTLs:** statische Assets (`/_nuxt/`, `/images/`, `*.css|js|…`) 24 h, Artikelseiten (`/article/`)
  5 min, Startseite (`/`) 1 min, Rest 30 s; `grace 1h` für Stale-While-Revalidate.
- **Gezielte Invalidierung:** `PURGE`-Methode mit ACL (nur das Gateway aus dem internen Netz darf
  purgen). Cookies werden für cachebare Inhalte entfernt, damit sie den Cache nicht aushebeln.
- **Observability:** `X-Cache: HIT|MISS` + `X-Cache-Hits` im Response (für die Hit-Rate).

Varnish (statt nginx) wegen der ausgereiften VCL, Stale-While-Revalidate-Semantik und granularer
PURGE/BAN-Steuerung.

### 2. Apache als Reverse-Proxy
`infrastructure/apache/` (eigenes Image, das die nötigen Module aktiviert):
TLS-Terminierung (mod_ssl, aktiv sobald ein Zertifikat hinterlegt ist), gzip (mod_deflate),
Security-Header (HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy) und
Bandbreiten-Rate-Limiting (mod_ratelimit; request-rate via mod_qos/Ingress in Prod).

### 3. Event-getriebene Invalidierung
Der Gateway-`ArticleEventConsumer` (Epic 4) ruft bei `article.*`-Events einen `CachePurger` auf:
- `NoOpCachePurger` (Default) — ohne Edge-Cache,
- `HttpVarnishPurger` (`newscore.varnish.purge.enabled=true`) — sendet HTTP `PURGE` an Varnish für
  `/article/{id}` und `/`.

So wird ein veröffentlichter Artikel sofort invalidiert, statt auf den TTL-Ablauf zu warten.
Purge-Fehler werden geschluckt (Fallback = TTL) und dürfen die Event-Verarbeitung nie brechen.

## Konsequenzen

**Positiv**
- Hohe Hit-Rate / niedrige TTFB durch Edge-Caching; Ursprung (SSR) entlastet.
- Sofortige, gezielte Invalidierung bei Redaktions-Events; Stale-While-Revalidate dämpft Lastspitzen.
- Sicherheits-Header und Kompression zentral am Edge.

**Negativ / Trade-offs**
- Zusätzliche zwei Hops (Apache, Varnish) im Pfad — durch Caching mehr als kompensiert.
- TTL-basierte Konsistenz: ohne Purge sind Inhalte bis zu ihrer TTL veraltet.
- Edge ist optional (`docker compose --profile edge`), um den lokalen Default-Stack schlank zu halten.

## Verifikation

- `varnishd -C -f default.vcl` kompiliert die VCL.
- Der Apache-Image-Build führt `httpd -t` aus (Configtest).
- Gateway-Unit-Tests für `HttpVarnishPurger` (PURGE via `MockRestServiceServer`) und die
  Consumer-Integration.
- Lokal: `docker compose --profile edge up` → `curl -I http://localhost:8088/` zeigt `X-Cache`
  (MISS→HIT) und die Security-Header.

## Verweise

- `infrastructure/varnish/default.vcl`, `infrastructure/apache/`
- `apps/api-gateway/src/main/java/de/newscore/cache/`
- Verwandt: [ADR-003](ADR-003-caffeine-caching.md) (App-Cache), [ADR-005](ADR-005-kafka-eventing.md) (Events)

## Nachtrag 2026-11-17

Die Verifikation oben prüfte, dass das Gateway ein `PURGE` absendet, nicht, dass Varnish danach neu
lädt. Am laufenden Stack gemessen, tat es das nicht; zugleich konnte jeder Client von außen purgen
([DEFECT-002](../defects/DEFECT-002-cache-invalidierung.md)). Geändert:

- **`ban` statt `return (purge)`.** `purge` trifft nur das Objekt unter dem Host-Header der Anfrage;
  Seiten liegen unter dem öffentlichen Hostnamen, das Gateway ruft Varnish als `varnish` auf. Der Ban
  gilt für den gespeicherten Pfad (`obj.http.x-url`), unabhängig vom Host und einschließlich der
  Query-Varianten (`/?category=…`).
- **Apache leitet nur `GET`, `HEAD`, `POST` weiter.** Die ACL in Varnish reicht hinter einem Proxy
  nicht, weil jede Anfrage dessen Adresse trägt.
- **Fehlerantworten:** 5xx wird nicht gespeichert, 404 nur 30 s
  ([DEFECT-007](../defects/DEFECT-007-artikel-404.md)).
- **CSP** setzt nicht mehr Apache, sondern das Frontend ([ADR-012](ADR-012-same-origin-und-csp.md)).
- Die Regeln liegen in `infrastructure/varnish/newscore.vcl`, `default.vcl` benennt nur das Backend.

Nicht gemessen sind die im Kontext genannten Zielwerte (TTFB < 200 ms, Hit-Rate > 85 % unter Last);
einen Lasttest gibt es nicht. Suchseiten werden nicht aktiv invalidiert (TTL 30 s).

Verifikation jetzt: `infrastructure/varnish/tests/*.vtc` (varnishtest) und `e2e/tests/edge.spec.ts`
gegen den laufenden Stack.
