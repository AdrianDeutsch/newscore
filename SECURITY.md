# Security Policy

## Unterstützte Versionen

Aktiv gepflegt wird der `main`-Branch. Sicherheitsrelevante Fixes fließen dort zuerst ein.

| Branch    | Unterstützt |
|-----------|:-----------:|
| `main`    | ✅          |
| `develop` | ✅          |
| sonstige  | ❌          |

## Eine Schwachstelle melden

Bitte melde Sicherheitslücken **nicht** über öffentliche Issues, sondern vertraulich über
[GitHub Security Advisories](https://github.com/AdrianDeutsch/newscore/security/advisories/new).

Bitte gib nach Möglichkeit an:

- betroffene Komponente (`api-gateway`, `frontend`, `analytics-service`, Infrastruktur),
- Beschreibung und möglichen Impact,
- Schritte zur Reproduktion (PoC),
- betroffene Version / Commit.

Du erhältst zeitnah eine Rückmeldung. Verantwortungsvolle Offenlegung wird ausdrücklich begrüßt.

## Maßnahmen im Projekt

- Container-Image-Scan (Trivy) in der Pipeline — kein Merge bei HIGH/CRITICAL CVEs.
- Automatische Dependency-Updates via Dependabot (Maven, npm, GitHub Actions, Docker).
- Secrets ausschließlich über Vault/Sealed-Secrets (siehe [ADR-006](docs/adr/ADR-006-helm-argocd-gitops.md)),
  niemals im Repository.
