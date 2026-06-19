import { computed } from 'vue'
import { createArticleService } from '~/services/article.service'
import type { SearchResult } from '~/types/article'

const EMPTY: SearchResult = { query: '', totalCount: 0, results: [] }

/**
 * Reactive full-text search. Re-runs whenever the query getter changes; a blank query short-circuits
 * to an empty result without hitting the gateway.
 *
 * @param getQuery getter returning the current search term (e.g. from the route query)
 */
export function useSearch(getQuery: () => string) {
  const { $urql } = useNuxtApp()
  const service = createArticleService($urql)
  const query = computed(getQuery)

  return useAsyncData<SearchResult>(
    'search',
    () => {
      const term = query.value.trim()
      return term ? service.search(term) : Promise.resolve({ ...EMPTY, query: term })
    },
    { watch: [query] },
  )
}
