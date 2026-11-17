# Contributing to NewsCore

Die wichtigsten Konventionen in Kürze.

## Voraussetzungen

- **Java 21** für `apps/api-gateway`
- **Node.js 24** für `apps/frontend`, `services/analytics-service` und `e2e`
- **Docker** mit Compose für den Stack, die Testcontainers-Tests und die End-to-End-Suite

## Nach dem Clone

```bash
git config core.hooksPath .githooks
```

Damit laufen lokal:

- `pre-commit`: ESLint bzw. Checkstyle für die Apps, deren Dateien im Commit sind
- `commit-msg`: Prüfung auf Conventional Commits
- `pre-push`: Unit-Tests von Gateway, Frontend und Analytics-Service (überspringen mit
  `SKIP_TESTS=1 git push`)

## Branches

| Branch | Zweck |
|---|---|
| `main` | stabiler Stand; Änderungen über Pull Requests |
| `develop` | Integrationsbranch |
| `feature/*`, `feat/*` | neue Funktionen |
| `fix/*` | Fehlerbehebungen |

## Commit-Format (Conventional Commits)

Format: `type(scope): beschreibung`

```text
feat(frontend): add article search with debounce
fix(edge): make cache invalidation work and keep outsiders from purging
test(e2e): end-to-end suite against the compose stack
docs(adr): add ADR-012
```

Erlaubte Typen: `feat | fix | perf | test | ci | docs | refactor | chore | style | build | revert`

## Tests

```bash
cd apps/api-gateway && ./mvnw verify
cd apps/frontend && npm run test:unit -- --run
cd services/analytics-service && npm run test:unit -- --run && npm run test:integration
```

End-to-End gegen den vollständigen Stack: siehe [README](README.md#tests).

Abdeckungsgrenze in allen drei Diensten: 80 % Zeilen, in der CI erzwungen. Jede fachliche Regel
bekommt einen Test, der fehlschlagen kann; bei Fehlerbehebungen einen, der vor dem Fix fehlschlägt.

## Architecture Decision Records

Größere Technologie- oder Musterentscheidungen bekommen ein ADR in [`docs/adr/`](docs/adr/).

## Fehlerberichte

Fehler werden in [`docs/defects/`](docs/defects/) nach dem
[Template](docs/defects/DEFECT-TEMPLATE.md) dokumentiert, mit Ursache und Regressionstest.
