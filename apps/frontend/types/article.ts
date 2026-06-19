/**
 * Frontend-facing types mirroring the GraphQL schema of the api-gateway.
 */

export interface Author {
  id: string
  name: string
  email?: string | null
  bio?: string | null
}

export interface Category {
  id: string
  name: string
  slug: string
}

export interface Article {
  id: string
  title: string
  teaser?: string | null
  body: string
  author: Author
  category: Category
  /** ISO-8601 date-time string (GraphQL DateTime scalar). */
  publishedAt: string
  tags: string[]
  imageUrl?: string | null
}

export interface ArticleConnection {
  nodes: Article[]
  totalCount: number
  hasNextPage: boolean
}

export interface SearchResult {
  query: string
  totalCount: number
  results: Article[]
}

export interface ArticlesParams {
  category?: string
  limit?: number
  offset?: number
}
