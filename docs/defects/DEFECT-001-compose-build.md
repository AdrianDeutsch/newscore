# DEFECT-001: docker compose up --build schlägt fehl

| Feld            | Wert                                            |
|-----------------|-------------------------------------------------|
| **ID**          | DEFECT-001                                       |
| **Status**      | behoben                                          |
| **Severity**    | blocker |
| **Priorität**   | P0 |
| **Komponente**  | infra (Dockerfiles) |
| **Gefunden in** | `1d8ef9a` (`main`)                               |
| **Gefunden bei**| Messung des Ist-Zustands, 16.–17.11.2026         |
| **Datum**       | 2026-11-17                                        |

## Beschreibung

Der im README genannte Startbefehl baut die Images nicht mehr. Am Repository hatte sich seit Juni nichts geändert; geändert hatten sich die Basis-Images hinter den unveränderten Tags.

## Schritte zur Reproduktion

1. Frischer Clone, Docker läuft.
2. `docker compose up --build`

## Erwartetes Verhalten

Alle Images bauen, der Stack startet.

## Tatsächliches Verhalten

Der Build bricht ab, nacheinander an drei Stellen:

- `api-gateway`, Runtime-Stage: `useradd: UID 1000 is not unique` (Exit-Code 4).
- `api-gateway`, Build-Stage (nach Behebung des ersten Fehlers): `gzip: stdin has more than one entry`, `tar: Child returned status 2` beim ersten Aufruf von `./mvnw`.
- `frontend`: `npm ci` meldet `Missing: commander@13.1.0 from lock file`. Der Build lief wegen `npm ci || npm install` trotzdem weiter und installierte dann ohne Lockfile-Bindung.

## Umgebung

- lokal (Docker Desktop 29.2, macOS arm64); Stand `main` = `1d8ef9a`

## Root Cause

- `eclipse-temurin:21-jre` basiert inzwischen auf einem Ubuntu, das selbst einen Benutzer mit UID 1000 mitbringt.
- `eclipse-temurin:21-jdk` enthält kein `unzip` mehr; der Maven-Wrapper versucht das ZIP dann mit `tar` zu entpacken.
- Die Lockfiles waren mit npm 11 erzeugt, das Image `node:22-alpine` bringt npm 10 mit. Der Fallback `|| npm install` hat den Fehler verdeckt.
- Die CI hat die Images nie gebaut, deshalb fiel nichts davon auf.

## Fix

- Gateway: Build-Stage auf `maven:3.9-eclipse-temurin-21-noble` (bringt Maven mit), Runtime auf `eclipse-temurin:21-jre-noble`, Ausführung unter der numerischen UID 10001 ohne `useradd`.
- Node-Images auf `node:24-alpine` (npm 11), `npm ci` ohne Fallback.
- Die CI baut und startet den Stack bei jedem Push und zusätzlich einmal pro Woche ohne Commit.

## Regressionstest

CI-Job `e2e` in `.github/workflows/ci.yml`: `docker compose --env-file stack-full.env --profile edge --profile observability up --build --wait --wait-timeout 600`, danach die Suite in `e2e/`.
