import type { Client } from '@urql/core'
import { ARTICLES, ARTICLE_BY_ID, SEARCH } from '~/graphql/queries'
import type { Article, ArticleConnection, ArticlesParams, SearchResult } from '~/types/article'

const EMPTY_CONNECTION: ArticleConnection = { nodes: [], totalCount: 0, hasNextPage: false }

/**
 * Read-only access to editorial content via the GraphQL gateway.
 *
 * <p>The service is a thin, framework-agnostic wrapper around a urql {@link Client}. Decoupling it
 * from Nuxt makes it trivially unit-testable with a mocked client (see ADR-001).</p>
 */
export interface ArticleService {
  getArticle(id: string): Promise<Article | null>
  getArticles(params?: ArticlesParams): Promise<ArticleConnection>
  search(query: string, limit?: number): Promise<SearchResult>
}

/**
 * Creates an {@link ArticleService} backed by the given urql client.
 *
 * @param client a configured urql client
 */
export function createArticleService(client: Client): ArticleService {
  return {
    async getArticle(id: string): Promise<Article | null> {
      const result = await client.query<{ article: Article | null }>(ARTICLE_BY_ID, { id }).toPromise()
      if (result.error) {
        throw result.error
      }
      return result.data?.article ?? null
    },

    async getArticles(params: ArticlesParams = {}): Promise<ArticleConnection> {
      const result = await client
        .query<{ articles: ArticleConnection }>(ARTICLES, {
          category: params.category ?? null,
          limit: params.limit ?? 20,
          offset: params.offset ?? 0,
        })
        .toPromise()
      if (result.error) {
        throw result.error
      }
      return result.data?.articles ?? EMPTY_CONNECTION
    },

    async search(query: string, limit = 10): Promise<SearchResult> {
      const result = await client.query<{ search: SearchResult }>(SEARCH, { query, limit }).toPromise()
      if (result.error) {
        throw result.error
      }
      return result.data?.search ?? { query, totalCount: 0, results: [] }
    },
  }
}
