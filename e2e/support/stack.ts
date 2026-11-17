import { Client } from 'pg'

/**
 * Addresses of the compose stack as published to the host (docker-compose.yml). The suite never
 * starts or stops containers; it expects, from the repository root,
 * `docker compose --env-file stack-full.env --profile edge --profile observability up --build --wait`.
 */
export const stack = {
  /** Nuxt SSR frontend, reached directly. */
  frontend: process.env.E2E_FRONTEND_URL ?? 'http://localhost:3000',
  /** Apache -> Varnish -> frontend. */
  edge: process.env.E2E_EDGE_URL ?? 'http://localhost:8088',
  /** api-gateway (GraphQL, CMS simulator). */
  gateway: process.env.E2E_GATEWAY_URL ?? 'http://localhost:8080',
  elasticsearch: process.env.E2E_ELASTICSEARCH_URL ?? 'http://localhost:9200',
  postgres: process.env.E2E_POSTGRES_URL ?? 'postgres://newscore:newscore@localhost:5432/newscore',
  prometheus: process.env.E2E_PROMETHEUS_URL ?? 'http://localhost:9090',
  grafana: process.env.E2E_GRAFANA_URL ?? 'http://localhost:3001',
  loki: process.env.E2E_LOKI_URL ?? 'http://localhost:3100',
  tempo: process.env.E2E_TEMPO_URL ?? 'http://localhost:3200',
}

/** Runs one query against the analytics database and returns the rows. */
export async function queryAnalytics<T extends Record<string, unknown>>(
  sql: string,
  params: unknown[] = [],
): Promise<T[]> {
  const client = new Client({ connectionString: stack.postgres })
  await client.connect()
  try {
    const result = await client.query(sql, params)
    return result.rows as T[]
  } finally {
    await client.end()
  }
}

/** Sends a GraphQL query to the gateway and returns `data`; fails on GraphQL errors. */
export async function graphql<T>(query: string, variables: Record<string, unknown> = {}): Promise<T> {
  const response = await fetch(`${stack.gateway}/graphql`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ query, variables }),
  })
  const body = (await response.json()) as { data?: T, errors?: unknown }
  if (!response.ok || body.errors || !body.data) {
    throw new Error(`GraphQL request failed (${response.status}): ${JSON.stringify(body.errors ?? body)}`)
  }
  return body.data
}

/** Emits an article event through the CMS simulator, as the CMS webhook would. */
export async function emitArticleEvent(articleId: string, action: 'publish' | 'update' | 'delete'): Promise<void> {
  const response = await fetch(`${stack.gateway}/internal/cms/articles/${articleId}/${action}`, { method: 'POST' })
  if (response.status !== 202) {
    throw new Error(`CMS simulator answered ${response.status}`)
  }
}

/** `X-Cache` of a GET through the edge: "HIT" or "MISS". */
export async function cacheStatus(path: string): Promise<string | null> {
  const response = await fetch(`${stack.edge}${path}`)
  await response.arrayBuffer()
  return response.headers.get('x-cache')
}
