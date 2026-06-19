# NewsCore Frontend

Server-seitig gerendertes Medienportal — **Nuxt 3**, **Vue 3**, **TypeScript**, GraphQL via **urql**.
Konsumiert ausschließlich die GraphQL-API des [api-gateway](../api-gateway/README.md).

> GraphQL-Client-Entscheidung: [ADR-001](../../docs/adr/ADR-001-graphql-client-urql.md)

## ✨ Was drin ist

| Thema            | Umsetzung                                                                 |
|------------------|---------------------------------------------------------------------------|
| Rendering        | SSR via `useAsyncData`; urql `ssrExchange` → Nuxt-Payload-Hydration       |
| GraphQL-Client   | urql (`@urql/core`), gekapselt in `services/article.service.ts`           |
| Komponenten      | `ArticleCard`, `CategoryNav`, `SearchBar`, `BreadcrumbNav`                |
| Composables      | `useArticle`, `useSearch`                                                  |
| Seiten           | Startseite (Ressort-Filter), Artikeldetail, Suche                         |
| Bilder           | Lazy Loading (`loading="lazy"`), Placeholder-Fallback                      |
| Tests            | Vitest + Vue Test Utils (Komponente + Service-Layer)                       |

## 🚀 Quick Start

```bash
npm install
npm run dev      # http://localhost:3000  (erwartet api-gateway auf :8080)
```

Andere Endpoints konfigurieren:

```bash
# Server-seitig (SSR) und Client-seitig getrennt überschreibbar
NUXT_GRAPHQL_ENDPOINT=http://api-gateway:8080/graphql \
NUXT_PUBLIC_GRAPHQL_ENDPOINT=http://localhost:8080/graphql \
npm run dev
```

## 🎯 Tests

```bash
npm run test:unit -- --run            # einmalig
npm run test:unit -- --run --coverage # mit Coverage (>=80% auf der Unit-Schicht)
npm run lint
npm run type-check
```

Der Coverage-Scope umfasst die unit-testbare Schicht (`components/**`, `services/**`).
Nuxt-Glue (Plugins, Composables, Pages) wird später über E2E-Tests (Playwright) abgedeckt.

## 🗂 Struktur

```
components/   ArticleCard, CategoryNav, SearchBar, BreadcrumbNav
composables/  useArticle, useSearch        (Nuxt-Glue um den Service)
services/     article.service.ts           (testbarer urql-Wrapper)
graphql/      queries.ts                    (Query-Dokumente)
plugins/      urql.ts                       (Client + ssrExchange)
pages/        index, article/[id], search
types/        article.ts                    (Schema-Spiegel)
tests/unit/   components, services, factories
```

## 🐳 Docker

```bash
docker build -t newscore/frontend .
docker run -p 3000:3000 \
  -e NUXT_GRAPHQL_ENDPOINT=http://host.docker.internal:8080/graphql \
  newscore/frontend
```

## 🛣 Roadmap

- `@graphql-codegen` für typsichere Operationen (siehe ADR-001)
- Playwright-E2E für die wichtigsten User Journeys
- `@nuxt/image` + CDN, i18n (`@nuxtjs/i18n`), PWA-Support
