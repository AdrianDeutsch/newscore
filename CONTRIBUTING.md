# Contributing to NewsCore

Danke für deinen Beitrag! Dieses Dokument fasst die wichtigsten Konventionen zusammen.

## Voraussetzungen

- **Java 21** (für `apps/api-gateway`)
- **Node.js 22+** (für `apps/frontend`)
- **Docker** (für das lokale Stack-Setup via `docker compose`)

## Einmaliges Setup nach dem Clone

```bash
git config core.hooksPath .githooks
chmod +x .githooks/*
```

Damit sind die lokalen Quality Gates aktiv:

- `pre-commit` — Lint + Format-Check (nur für geänderte Apps)
- `commit-msg` — Conventional-Commit-Validierung
- `pre-push` — Unit-Tests (Bypass mit `SKIP_TESTS=1 git push`)

## Branch-Strategie (GitFlow light)

| Branch       | Zweck                                            |
|--------------|--------------------------------------------------|
| `main`       | Production-ready, protected, nur via MR          |
| `develop`    | Integrationsbranch                               |
| `feature/*`  | Neue Features (`feature/NC-42-article-search`)   |
| `fix/*`      | Bugfixes (`fix/NC-99-varnish-ttl`)               |
| `hotfix/*`   | Kritische Prod-Fixes                             |
| `release/*`  | Release-Vorbereitung                             |

## Commit-Konventionen (Conventional Commits)

Format: `type(scope): beschreibung (TICKET-NR)`

```text
feat(frontend): add article search with debounce (NC-42)
fix(api): resolve N+1 query in author resolver (NC-55)
test(api): add integration tests for search endpoint
docs(adr): add ADR-001 GraphQL client decision
```

Erlaubte Typen: `feat | fix | perf | test | ci | docs | refactor | chore | style | build | revert`

## Tests

Vor jedem Push laufen Unit-Tests automatisch. Manuell:

```bash
# Backend
cd apps/api-gateway && ./mvnw verify          # Unit + Integration + Coverage

# Frontend
cd apps/frontend && npm run test:unit -- --run
```

Unit-Test-Coverage-Ziel: **≥ 80 %** (in CI erzwungen).

## Architecture Decision Records

Jede größere Technologie- oder Pattern-Entscheidung bekommt ein ADR in [`docs/adr/`](docs/adr/).
Schreibe das ADR **bevor** du die Entscheidung umsetzt.

## Defect Management

Bugs werden über das [`docs/defects/`](docs/defects/)-Verzeichnis nach dem
[Defect-Template](docs/defects/DEFECT-TEMPLATE.md) dokumentiert.
