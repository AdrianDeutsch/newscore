import { Client, cacheExchange, fetchExchange, ssrExchange } from '@urql/core'

/**
 * Provides a configured urql client as {@code $urql}.
 *
 * <p>SSR data is transferred to the client through Nuxt's payload via {@code ssrExchange}, so the
 * browser hydrates without an immediate refetch (see ADR-001). On the server the client calls the
 * gateway directly; in the browser it calls this site's own {@code /graphql}, which
 * {@code server/routes/graphql.post.ts} forwards.</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  const url = import.meta.server
    ? `${useRuntimeConfig().gatewayUrl}/graphql`
    : '/graphql'

  const ssr = ssrExchange({ isClient: import.meta.client })

  if (import.meta.server) {
    nuxtApp.hook('app:rendered', () => {
      nuxtApp.payload.data.urql = ssr.extractData()
    })
  }
  else if (nuxtApp.payload?.data?.urql) {
    ssr.restoreData(nuxtApp.payload.data.urql)
  }

  const client = new Client({
    url,
    exchanges: [cacheExchange, ssr, fetchExchange],
  })

  return {
    provide: { urql: client },
  }
})
