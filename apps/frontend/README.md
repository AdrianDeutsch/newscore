# NewsCore Frontend

Server-seitig gerendertes Nachrichtenportal: Nuxt 4, Vue 3, TypeScript, GraphQL über urql. Die Daten
kommen ausschließlich vom [api-gateway](../api-gateway/README.md).

GraphQL-Client: [ADR-001](../../docs/adr/ADR-001-graphql-client-urql.md) ·
Same-Origin und CSP: [ADR-012](../../docs/adr/ADR-012-same-origin-und-csp.md)

## Was drin ist

| Thema | Umsetzung |
|---|---|
| Rendering | SSR mit `useAsyncData`; urql `ssrExchange` überträgt die Daten in den Nuxt-Payload, der Browser lädt beim Hydrieren nicht neu |
| Seiten | Startseite mit Ressort-Filter, Artikel, Suche, Fehlerseite (`error.vue`) |
| Zugriff auf das Gateway | Beim SSR direkt. Im Browser über die eigene Origin: `server/routes/graphql.post.ts` und `server/routes/events/pageview.post.ts` reichen an das Gateway weiter |
| Content-Security-Policy | `server/plugins/csp.ts`: `script-src 'self'` plus SHA-256-Hashes der Inline-Skripte der jeweiligen Seite, kein `'unsafe-inline'` |
| HTTP-Status | Artikel nicht gefunden → 404; Gateway beim Server-Rendering nicht erreichbar → 502 auf Start-, Such- und Artikelseite (`createError`). Schlägt nur das Nachladen im Browser fehl, zeigt die Seite einen Hinweis |
| Datum und Uhrzeit | Immer in `Europe/Berlin` (`utils/formatDate.ts`), damit Server und Browser denselben Text erzeugen |
| Pageviews | `plugins/pageview.client.ts` meldet jede Navigation an `/events/pageview` |

## Starten

```bash
npm install
npm run dev      # http://localhost:3000, erwartet das Gateway auf http://localhost:8080
```

Gateway an anderer Adresse:

```bash
NUXT_GATEWAY_URL=http://localhost:18081 npm run dev
```

Im Entwicklungsmodus sendet die Anwendung keine CSP (Vite braucht dort Inline-Skripte und `eval`).

## Tests

```bash
npm run test:unit -- --run              # 25 Tests
npm run test:unit -- --run --coverage   # mit Abdeckungsgrenze (80 % Zeilen)
npm run lint
npm run type-check
```

Unit-getestet und in der Abdeckung gemessen sind `components/`, `services/`, `utils/` und
`server/utils/`. Seiten, Plugins, Composables und die Server-Routen haben keine Unit-Tests; sie laufen
in der Browser-Suite unter [`e2e/`](../../e2e/) gegen den gestarteten Stack, einmal direkt und einmal
hinter Apache/Varnish. Dort lässt jeder Fehler in der Browser-Konsole einen Test scheitern, also auch
eine CSP-Verletzung oder ein Hydration-Mismatch. Das Verhalten ohne erreichbares Gateway prüft
`e2e/tests/gateway-down.spec.ts`.

Die Unit-Tests laufen mit `TZ=America/New_York` (`vitest.config.ts`), damit ein Datum, das ohne feste
Zeitzone formatiert wird, sofort auffällt.

## Struktur

```
pages/          index, article/[id], search
components/     ArticleCard, CategoryNav, SearchBar, BreadcrumbNav
composables/    useArticle, useSearch
services/       article.service.ts       testbarer urql-Wrapper
graphql/        queries.ts
plugins/        urql.ts (Client + ssrExchange), pageview.client.ts
server/routes/  graphql.post.ts, events/pageview.post.ts   Weiterleitung ans Gateway
server/plugins/ csp.ts
server/utils/   csp.ts                   Hash-Berechnung
utils/          formatDate.ts
error.vue
tests/unit/     components, services, server, utils
```

## Docker

Das Image baut der Compose-Stack. Es braucht zur Laufzeit nur `NUXT_GATEWAY_URL`.

## Nicht umgesetzt

- GraphQL-Codegen (die Typen in `types/article.ts` sind von Hand gepflegt)
- Die Ressort-Navigation ist fest verdrahtet (`components/CategoryNav.vue`), nicht vom Gateway geladen
- i18n, `@nuxt/image`, PWA
