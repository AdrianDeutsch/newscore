import { createArticleService } from '~/services/article.service'

/**
 * Loads a single article by id, SSR-ready via {@link useAsyncData}.
 *
 * @param id the article id
 */
export function useArticle(id: string) {
  const { $urql } = useNuxtApp()
  const service = createArticleService($urql)
  return useAsyncData(`article:${id}`, () => service.getArticle(id))
}
