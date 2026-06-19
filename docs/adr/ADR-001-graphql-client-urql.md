# ADR-001: urql als GraphQL-Client im Nuxt-Frontend

- **Status:** Akzeptiert
- **Datum:** 2026-06-19
- **Entscheider:** Frontend-Team
- **Kontext-Epic:** Epic 1 (Nuxt 3 Frontend)

## Kontext

Das Nuxt-3-Frontend rendert serverseitig (SSR) und konsumiert ausschließlich die
GraphQL-API des BFF-Gateways. Der GraphQL-Client muss:

- SSR-tauglich sein (State-Transfer Server → Client ohne Doppel-Fetch),
- eine kleine Bundle-Size haben (Akzeptanzkriterium LCP < 2,5 s),
- gut mit Nuxts `useAsyncData`/Composables harmonieren,
- für ein **primär lesendes** News-Portal keinen schweren normalisierten Cache erzwingen.

Bewertete Optionen: **Apollo Client**, **urql**, **nuxt-graphql-client**.

## Entscheidung

Wir verwenden **urql**.

| Kriterium              | urql            | Apollo Client      | nuxt-graphql-client |
|------------------------|-----------------|--------------------|---------------------|
| Bundle-Size            | ~ klein         | groß               | klein (Codegen)     |
| SSR-Integration        | `ssrExchange`   | gut, schwerer      | sehr eng an Nuxt    |
| Cache-Modell           | Document-Cache  | normalisiert       | Document-Cache      |
| Kontrolle / Transparenz| hoch (Exchanges)| mittel             | gering (Magie)      |
| Lernkurve              | niedrig         | mittel-hoch        | niedrig             |

Für ein lesedominiertes Portal ist der normalisierte Apollo-Cache Overhead; die enge
Kopplung von `nuxt-graphql-client` reduziert die Kontrolle über das Cache-Verhalten.
urqls Exchange-Architektur (`ssrExchange`, `cacheExchange`, `fetchExchange`) gibt uns
explizite Kontrolle bei minimalem Footprint.

## Konsequenzen

**Positiv**
- Kleines Bundle → bessere LCP/TTFB-Werte.
- `ssrExchange` transferiert den Server-State sauber in die Client-Hydration.
- Exchanges sind explizit und testbar.

**Negativ / Trade-offs**
- Kein automatischer normalisierter Cache; für komplexe Mutations-Szenarien müsste
  später `@urql/exchange-graphcache` ergänzt werden (für reine Reads nicht nötig).
- Kein integriertes Codegen out-of-the-box → GraphQL-Operationen werden manuell typisiert
  (alternativ später `@graphql-codegen` ergänzbar, siehe Roadmap).

## Verweise

- Frontend-Plugin: `apps/frontend/plugins/urql.ts`
- Verwandte Entscheidung: [ADR-002](ADR-002-graphql-bff-pattern.md) (BFF-Pattern)
