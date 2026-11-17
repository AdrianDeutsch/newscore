import { randomBytes } from 'node:crypto'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { parse } from 'yaml'
import { expect, test } from '../support/fixtures'
import { emitArticleEvent, graphql, stack } from '../support/stack'

const repo = path.resolve(__dirname, '../..')

/** Metric names in PromQL expressions: an identifier directly followed by a label selector. */
function metricNames(expressions: string[]): string[] {
  const names = new Set<string>()
  for (const expression of expressions) {
    for (const match of expression.matchAll(/\b([a-z_][a-z0-9_]*)\{/g)) {
      names.add(match[1] as string)
    }
  }
  // `up` is written by Prometheus itself, not exported by the application.
  names.delete('up')
  return [...names].sort()
}

function alertExpressions(): string[] {
  const rules = parse(readFileSync(path.join(repo, 'infrastructure/helm/newscore/files/prometheus-rules.yml'), 'utf8')) as {
    groups: { rules: { expr: string }[] }[]
  }
  return rules.groups.flatMap((group) => group.rules.map((rule) => rule.expr))
}

function dashboardExpressions(): string[] {
  const dashboard = JSON.parse(
    readFileSync(path.join(repo, 'infrastructure/grafana/dashboards/newscore-api-overview.json'), 'utf8'),
  ) as { panels: { targets?: { expr?: string }[] }[] }
  return dashboard.panels.flatMap((panel) => (panel.targets ?? []).map((target) => target.expr ?? ''))
}

const grafanaAuth = { Authorization: `Basic ${Buffer.from('admin:admin').toString('base64')}` }

/**
 * Metrics, logs and traces as the observability profile promises them. The point is not that the
 * containers start, but that what alerts, dashboards and the trace-to-logs link query actually
 * exists.
 */
test.describe('observability', () => {
  test('the gateway reports itself UP', async () => {
    const response = await fetch(`${stack.gateway}/actuator/health`)

    expect(response.status).toBe(200)
    expect(await response.json()).toEqual({ status: 'UP', groups: ['liveness', 'readiness'] })
  })

  test('every metric an alert rule or the dashboard queries is exported, alert metrics with a numeric value', async () => {
    // Micrometer picks up new Kafka client metrics (the per-partition lag) on a 60-second refresh,
    // so right after the stack started they can take up to a minute to appear.
    test.setTimeout(120_000)
    // Traffic, so the request metrics exist, and an article event, so the consumer has fetched.
    await graphql('{ articles(limit: 1) { totalCount } }')
    await emitArticleEvent('2', 'update')

    const samples = async (name: string): Promise<string[]> => {
      const exported = await (await fetch(`${stack.gateway}/actuator/prometheus`)).text()
      return exported.split('\n').filter((line) => line.startsWith(`${name}{`) || line.startsWith(`${name} `))
        .map((line) => line.slice(line.lastIndexOf(' ') + 1))
    }

    const queried = metricNames([...alertExpressions(), ...dashboardExpressions()])
    expect(queried.length).toBeGreaterThan(3)
    for (const name of queried) {
      await expect.poll(() => samples(name), { message: `${name} is not exported`, timeout: 90_000, intervals: [2_000] }).not.toEqual([])
    }

    // An alert on a metric that is NaN never fires. Every series of an alert metric must be a number.
    for (const name of metricNames(alertExpressions())) {
      const values = await samples(name)
      expect(values.filter((value) => !Number.isFinite(Number(value))), `${name} has non-numeric samples`).toEqual([])
    }
  })

  test('Prometheus scrapes the gateway and has loaded every alert rule', async () => {
    await expect
      .poll(async () => {
        const body = (await (await fetch(`${stack.prometheus}/api/v1/targets`)).json()) as {
          data: { activeTargets: { labels: { job: string }, health: string }[] }
        }
        return body.data.activeTargets.find((target) => target.labels.job === 'newscore-api-gateway')?.health
      }, { timeout: 45_000 })
      .toBe('up')

    const rules = (await (await fetch(`${stack.prometheus}/api/v1/rules`)).json()) as {
      data: { groups: { rules: { name: string, health: string }[] }[] }
    }
    const loaded = rules.data.groups.flatMap((group) => group.rules)
    expect(loaded.map((rule) => rule.name).sort()).toEqual(
      ['ApiGatewayDown', 'ApiHighErrorRate', 'ApiHighLatencyP95', 'JvmHeapPressure', 'KafkaConsumerLagHigh'],
    )
    expect(loaded.every((rule) => rule.health === 'ok')).toBe(true)
  })

  test('Grafana comes up with the three data sources and the dashboard', async () => {
    const sources = (await (await fetch(`${stack.grafana}/api/datasources`, { headers: grafanaAuth })).json()) as {
      name: string
    }[]
    expect(sources.map((source) => source.name).sort()).toEqual(['Loki', 'Prometheus', 'Tempo'])

    const dashboards = (await (
      await fetch(`${stack.grafana}/api/search?type=dash-db`, { headers: grafanaAuth })
    ).json()) as { title: string }[]
    expect(dashboards.map((dashboard) => dashboard.title)).toContain('NewsCore API Overview')
  })

  test('a CMS event is one trace from the HTTP request to the Kafka consumer, and its logs carry the trace id', async () => {
    // Start the trace ourselves (W3C traceparent, sampled) so we know which one to look for.
    const traceId = randomBytes(16).toString('hex')
    const response = await fetch(`${stack.gateway}/internal/cms/articles/3/update`, {
      method: 'POST',
      headers: { traceparent: `00-${traceId}-${randomBytes(8).toString('hex')}-01` },
    })
    expect(response.status).toBe(202)

    // Tempo: the trace contains the HTTP request, the Kafka send and the Kafka receive.
    await expect
      .poll(async () => {
        const trace = await fetch(`${stack.tempo}/api/traces/${traceId}`)
        if (trace.status !== 200) {
          return []
        }
        const body = (await trace.json()) as {
          batches: { scopeSpans: { spans: { kind: string }[] }[] }[]
        }
        return [...new Set(body.batches.flatMap((batch) => batch.scopeSpans.flatMap((scope) => scope.spans.map((span) => span.kind))))].sort()
      }, { timeout: 30_000, intervals: [1_000] })
      // SERVER: the HTTP request; PRODUCER/CONSUMER: the Kafka hop; CLIENT: the PURGE calls to Varnish.
      .toEqual(['SPAN_KIND_CLIENT', 'SPAN_KIND_CONSUMER', 'SPAN_KIND_PRODUCER', 'SPAN_KIND_SERVER'])

    // Loki: the consumer's log line, found by the trace id — the query Grafana's
    // "logs for this trace" link uses (infrastructure/grafana/provisioning/datasources).
    await expect
      .poll(async () => {
        const url = new URL(`${stack.loki}/loki/api/v1/query_range`)
        url.searchParams.set('query', `{service="api-gateway"} |= "${traceId}"`)
        url.searchParams.set('limit', '20')
        const body = (await (await fetch(url)).json()) as { data: { result: { values: [string, string][] }[] } }
        return body.data.result.flatMap((stream) => stream.values.map(([, line]) => JSON.parse(line).message as string))
      }, { timeout: 30_000, intervals: [1_000] })
      .toContain('Handling UPDATED for article 3')
  })
})
