# ADR-006: Helm-Umbrella-Chart + ArgoCD-GitOps für das Deployment

- **Status:** Akzeptiert
- **Datum:** 2026-06-20
- **Entscheider:** Plattform-Team
- **Kontext-Epic:** Epic 5 (Kubernetes/OpenShift + GitOps)

## Kontext

Die Anwendung (api-gateway + frontend) soll auf OpenShift/Kubernetes hochverfügbar, mit
Zero-Downtime-Deployments und nach dem GitOps-Prinzip (Git als Single Source of Truth) betrieben
werden. Benötigt werden: parametrisierbare Manifeste pro Umgebung, automatisiertes Staging-Deployment
und ein manuelles Prod-Gate.

## Entscheidungen

### 1. Ein Umbrella-Chart statt Chart pro App
Ein Helm-Chart `infrastructure/helm/newscore` deployt beide Komponenten. Die Ressourcen werden über
eine `range`-Schleife über `.Values.components` erzeugt (DRY): jede Komponente bekommt
`Deployment`, `Service` und – optional – `ConfigMap`, `HorizontalPodAutoscaler`,
`PodDisruptionBudget`. Eine Änderung am Rendering trifft alle Komponenten gleich → minimale
Merge-Konflikte.

**Trade-off:** Beide Apps teilen sich Release-Lifecycle und Werte-Datei. Akzeptabel, da sie ein
zusammengehöriges Produkt sind; ein App-of-Apps-Split bleibt später möglich.

### 2. Werte-Schichtung pro Umgebung
`values.yaml` (Defaults) + `values-staging.yaml` / `values-prod.yaml` (Overrides: Image-Tag, Replicas,
Ressourcen, HPA-Grenzen, PDB, Ingress-Host). Helm-Deep-Merge hält die Overrides minimal.

### 3. OpenShift-konforme Sicherheit
Pod-/Container-`securityContext` sind **restricted-v2-kompatibel**: `runAsNonRoot`,
`seccompProfile: RuntimeDefault`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`.
**Kein** hartkodiertes `runAsUser` — OpenShift weist die UID aus dem Namespace-Range zu (die Images
laufen bereits als Nicht-Root). NetworkPolicies setzen Default-Deny-Ingress und erlauben nur
Frontend→Gateway und Ingress→Frontend.

### 4. Verfügbarkeit
- `HorizontalPodAutoscaler` (CPU-basiert) statt fixer Replicas.
- `PodDisruptionBudget` (`minAvailable`) für Zero-Downtime bei Node-Drains/Rollouts.
- `readinessProbe`/`livenessProbe` für beide Apps (Gateway: Actuator-Health-Groups
  `/actuator/health/readiness|liveness`; Frontend: `/`).

### 5. GitOps via ArgoCD
`infrastructure/argocd/`: zwei `Application`-Manifeste.
- **Staging** folgt `develop`, `syncPolicy.automated` (prune + selfHeal) → Auto-Deploy.
- **Prod** folgt `main`, **ohne** `automated`-Block → manuelles Sync-Gate (durch den
  `deploy:prod`-CI-Job / einen Operator). Konsistent mit `.gitlab-ci.yml`.

## Konsequenzen

**Positiv**
- Deklaratives, versioniertes Deployment; Git ist Single Source of Truth.
- Zero-Downtime durch HPA + PDB + Rolling Updates.
- Sicherheits-Defaults erfüllen OpenShift restricted-v2 ohne Cluster-Admin-Eingriff.

**Negativ / Trade-offs**
- Secrets sind im Chart nur Platzhalter; produktiv via Vault/sealed-secrets (nicht committet).
- ES/Kafka/Postgres werden hier als bestehende In-Cluster-Services vorausgesetzt (eigene Charts/
  Operatoren), nicht vom App-Chart verwaltet.

## Verifikation

- `helm lint infrastructure/helm/newscore`
- `helm template ... -f values-staging.yaml` und `-f values-prod.yaml` rendern fehlerfrei.
- Gerenderte Manifeste sind valides YAML (geprüft) und decken alle geforderten Ressourcentypen ab.

## Verweise

- Chart: `infrastructure/helm/newscore/`
- ArgoCD: `infrastructure/argocd/`
- CI-Deploy-Stages: `.gitlab-ci.yml`
