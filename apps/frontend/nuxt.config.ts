// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-06-01',
  devtools: { enabled: true },

  css: ['~/assets/css/main.css'],

  // GraphQL endpoint. The private value is used during SSR (server -> api-gateway via internal DNS),
  // the public value during client-side navigation (browser -> host-published port). Override with
  // NUXT_GRAPHQL_ENDPOINT and NUXT_PUBLIC_GRAPHQL_ENDPOINT (see docker-compose.yml).
  runtimeConfig: {
    graphqlEndpoint: 'http://localhost:8080/graphql',
    public: {
      graphqlEndpoint: 'http://localhost:8080/graphql',
    },
  },

  app: {
    head: {
      htmlAttrs: { lang: 'de' },
      title: 'NewsCore',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'NewsCore — cloud-natives, hochverfügbares Nachrichtenportal.' },
      ],
    },
  },
})
