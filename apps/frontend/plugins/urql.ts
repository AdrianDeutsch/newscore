import { Client, cacheExchange, fetchExchange, ssrExchange } from '@urql/core'

/**
 * Provides a configured urql client as {@code $urql}.
 *
 * <p>SSR data is transferred to the client through Nuxt's payload via {@code ssrExchange}, so the
 * browser hydrates without an immediate refetch (see ADR-001). The endpoint differs between server
 * (internal DNS) and client (host-published port), driven by runtime config.</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig()
  const url = (import.meta.server
    ? config.graphqlEndpoint
    : config.public.graphqlEndpoint) as string

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
