/**
 * GraphQL query documents used by the article service. Kept as plain strings — urql accepts string
 * documents and this avoids a codegen step in this iteration (see ADR-001 roadmap note).
 */

/** Reusable article field selection, including the batched author/category fields. */
const ARTICLE_FIELDS = `
  id
  title
  teaser
  body
  publishedAt
  tags
  imageUrl
  author { id name }
  category { id name slug }
`

export const ARTICLE_BY_ID = `
  query ArticleById($id: ID!) {
    article(id: $id) {
      ${ARTICLE_FIELDS}
    }
  }
`

export const ARTICLES = `
  query Articles($category: String, $limit: Int, $offset: Int) {
    articles(category: $category, limit: $limit, offset: $offset) {
      totalCount
      hasNextPage
      nodes {
        ${ARTICLE_FIELDS}
      }
    }
  }
`

export const SEARCH = `
  query Search($query: String!, $limit: Int) {
    search(query: $query, limit: $limit) {
      query
      totalCount
      results {
        ${ARTICLE_FIELDS}
      }
    }
  }
`
