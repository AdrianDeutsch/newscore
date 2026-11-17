# NewsCore Infrastruktur

Konfiguration für den Edge (Apache, Varnish), für Observability (Prometheus, Grafana, Loki, Tempo)
und für Kubernetes (Helm-Chart, ArgoCD-Applications).

| Teil | Wo es läuft | Wie es geprüft wird |
|---|---|---|
| Varnish, Apache | Compose-Profil `edge` | `varnish/tests/*.vtc` (varnishtest), `e2e/tests/edge.spec.ts` gegen den Stack (CI) |
| Prometheus, Grafana, Loki, Tempo | Compose-Profil `observability` | `prometheus/rules.test.yml` (promtool), `e2e/tests/observability.spec.ts` gegen den Stack (CI) |
| Helm-Chart | nirgends installiert | `helm lint` und `helm template` (CI) |
| ArgoCD-Applications | nirgends installiert | nicht geprüft; `repoURL` ist ein Platzhalter (`gitlab.example.com`) |

## Edge: Apache → Varnish → Frontend

```
infrastructure/
├── apache/        Reverse Proxy: gzip, Security-Header, nur GET/HEAD/POST
└── varnish/
    ├── default.vcl    Backend (frontend:3000), bindet newscore.vcl ein
    ├── newscore.vcl   Regeln: TTLs, Invalidierung, Fehlerantworten, X-Cache
    └── tests/         varnishtest gegen einen Mock-Backend
```

- **TTLs:** statische Dateien 24 h, Artikelseiten 5 min, Startseite 1 min, sonst 30 s; 1 h Grace.
  5xx wird nicht gespeichert, 404 nur 30 s.
- **Invalidierung:** Das Gateway schickt bei einem Artikel-Ereignis `PURGE /article/{id}` und `PURGE /`.
  Varnish setzt daraufhin einen Ban auf den gespeicherten Pfad, unabhängig vom Host-Header und
  einschließlich der Query-Varianten wie `/?category=politik`. Suchseiten werden nicht invalidiert
  (TTL 30 s).
- **Schutz:** Apache leitet nur `GET`, `HEAD` und `POST` weiter. Ein `PURGE` von außen endet dort mit
  403. Die IP-Liste in Varnish allein reicht nicht, weil hinter Apache jede Anfrage dessen Adresse
  trägt ([DEFECT-002](../docs/defects/DEFECT-002-cache-invalidierung.md)).
- **CSP** setzt das Frontend, nicht Apache ([ADR-012](../docs/adr/ADR-012-same-origin-und-csp.md)).
- TLS: Apache aktiviert einen 443-VirtualHost, sobald unter `conf/server.crt|key` ein Zertifikat
  liegt. Im Compose-Stack gibt es keins; dieser Teil ist nicht geprüft.

```bash
# im Repository-Root
docker compose --env-file stack-full.env --profile edge --profile observability up --build --wait
curl -sI http://localhost:8088/article/1 | grep -i x-cache    # erster Aufruf MISS, danach HIT
curl -XPOST http://localhost:8080/internal/cms/articles/1/update
curl -sI http://localhost:8088/article/1 | grep -i x-cache    # MISS
```

VCL-Tests allein:

```bash
docker run --rm -v "$PWD/infrastructure/varnish:/etc/varnish:ro" --entrypoint varnishtest \
  varnish:7.7 /etc/varnish/tests/caching.vtc /etc/varnish/tests/invalidation.vtc
```

## Observability

```
infrastructure/
├── prometheus/   prometheus.yml (Scrape-Config), rules.test.yml (promtool-Tests)
├── grafana/      Provisioning: Datenquellen Prometheus, Loki, Tempo; Dashboard „NewsCore API Overview“
├── loki/         promtail-config.yml: Logs aller Container über den Docker-Socket
└── tempo/        tempo.yaml: OTLP-Empfänger, lokaler Speicher
```

Die Alarmregeln liegen in `helm/newscore/files/prometheus-rules.yml`. Der lokale Prometheus bindet
dieselbe Datei ein wie die `PrometheusRule` des Charts.

Mit `stack-full.env` schreibt das Gateway JSON-Logs und exportiert jeden Trace. Ein Artikel-Ereignis
ergibt dann einen Trace aus HTTP-Anfrage, Kafka-Send, Kafka-Receive und den PURGE-Aufrufen; die
Logzeilen des Consumers tragen dieselbe `traceId`. Die End-to-End-Suite prüft das in Tempo und Loki.
Spans für Aufrufe an ElasticSearch gibt es nicht.

| Dienst | Adresse |
|---|---|
| Grafana (anonym lesend, Admin `admin`/`admin`) | <http://localhost:3001> |
| Prometheus | <http://localhost:9090> |
| Loki | <http://localhost:3100> |
| Tempo | <http://localhost:3200> |

```bash
docker run --rm -v "$PWD/infrastructure:/infra:ro" -w /infra/prometheus --entrypoint sh \
  prom/prometheus:v2.54.1 -c \
  'promtool check rules ../helm/newscore/files/prometheus-rules.yml && promtool test rules rules.test.yml'
```

## Helm und ArgoCD

```
infrastructure/
├── helm/newscore/
│   ├── values.yaml, values-staging.yaml, values-prod.yaml
│   ├── files/prometheus-rules.yml
│   └── templates/   Deployment, Service, HPA, PDB, ConfigMap, Secret, Ingress,
│                    NetworkPolicy, ServiceAccount, ServiceMonitor, PrometheusRule
└── argocd/          Applications für staging (develop) und prod (main)
```

Pro Komponente (`api-gateway`, `frontend`) erzeugt das Chart Deployment, Service, ConfigMap, HPA und
PDB. Dazu Ingress (nur Frontend), Secret, ServiceAccount, drei NetworkPolicies (Default-Deny,
Ingress → Frontend, Frontend → Gateway) und, mit `monitoring.enabled=true`, ServiceMonitor und
PrometheusRule. Die Pods laufen ohne Root, ohne Capabilities und mit Seccomp `RuntimeDefault`.

```bash
helm lint infrastructure/helm/newscore
helm template newscore infrastructure/helm/newscore -f infrastructure/helm/newscore/values-staging.yaml
```

Was das Chart **nicht** enthält und voraussetzt: ElasticSearch, Kafka, Schema Registry, PostgreSQL und
den analytics-service. Das Secret ist ein Platzhalter (`changeme`). `NUXT_GATEWAY_URL` geht vom
Release-Namen `newscore` aus. Das Chart wurde nie gegen einen Cluster installiert.
