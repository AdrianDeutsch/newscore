# ADR-012: Browser spricht nur mit der eigenen Origin; CSP per Hash

- **Status:** Akzeptiert
- **Datum:** 2026-11-17
- **Auslöser:** [DEFECT-006](../defects/DEFECT-006-csp-edge.md)

## Kontext

Das Frontend rief GraphQL im Browser direkt am Gateway auf (`http://localhost:8080/graphql`,
öffentliche Laufzeit-Konfiguration). Dafür brauchte das Gateway eine CORS-Freigabe (`*`), musste von
außen erreichbar sein, und die Content-Security-Policy am Proxy hätte die Gateway-Adresse kennen
müssen. Die feste Richtlinie dort (`script-src 'self'`) blockierte zudem das Inline-Skript, mit dem
Nuxt seine Laufzeit-Konfiguration übergibt: Hinter dem Proxy startete die App im Browser nicht.

Im Helm-Chart zeigte die öffentliche GraphQL-Adresse auf den Host des Ingress, der aber nur das
Frontend routet. Der Pfad `/graphql` hätte dort ins Leere geführt.

## Entscheidung

1. **Same-Origin.** Der Browser kennt nur die Adresse der Seite. `POST /graphql` und
   `POST /events/pageview` sind Server-Routen des Frontends (`apps/frontend/server/routes/`), die an
   das Gateway weiterreichen. Wo das Gateway steht, ist serverseitige Konfiguration
   (`NUXT_GATEWAY_URL`). Beim SSR ruft der Server das Gateway direkt auf.
2. **CSP aus der Anwendung.** Ein Nitro-Plugin berechnet für jede gerenderte Seite die
   SHA-256-Hashes ihrer ausführbaren Inline-Skripte und sendet die Richtlinie als Header
   (`apps/frontend/server/utils/csp.ts`). Apache setzt die übrigen Security-Header, keine CSP mehr.

**Hash statt Nonce:** Seiten liegen im Varnish. Eine Nonce wäre für die Dauer der TTL für alle Leser
gleich und damit wertlos; ein Hash gehört zum Inhalt und bleibt gültig. **Hash statt
`'unsafe-inline'`:** Damit wäre die Richtlinie für Skripte wirkungslos.

## Konsequenzen

**Positiv**
- Keine CORS-Konfiguration; das Gateway muss nicht öffentlich erreichbar sein. Die NetworkPolicy
  „nur Frontend → Gateway“ im Chart beschreibt damit den tatsächlichen Datenfluss.
- Eine Konfiguration statt drei (`NUXT_GRAPHQL_ENDPOINT`, `NUXT_PUBLIC_GRAPHQL_ENDPOINT`,
  `NUXT_PUBLIC_PAGEVIEW_ENDPOINT`).
- `connect-src 'self'` genügt.

**Negativ / Trade-offs**
- GraphQL-Anfragen des Browsers laufen über einen zusätzlichen Hop (Frontend-Server).
- `style-src` erlaubt weiterhin `'unsafe-inline'` (Vue setzt Inline-Styles).
- Im Entwicklungsmodus (`nuxt dev`) ist die CSP aus, weil Vite dort Inline-Skripte und `eval` nutzt.

## Verifikation

- `apps/frontend/tests/unit/server/csp.test.ts` (welche Skripte gehasht werden, kein `unsafe-inline`).
- `e2e/tests/ui.spec.ts` im Projekt `ui-edge`: alle Lesewege im Browser über Apache → Varnish; ein
  Konsolenfehler (also auch eine CSP-Verletzung) lässt den Test scheitern.
