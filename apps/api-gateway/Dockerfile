# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
# The Maven image brings Maven itself, so the build does not rely on tools the JDK base image may
# or may not ship (the wrapper needs `unzip`, which newer eclipse-temurin images dropped — see
# docs/defects/DEFECT-001).
FROM maven:3.9-eclipse-temurin-26-noble AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src/ src/
# Tests run in CI, not in the image build. The cache mount keeps the local repository between builds.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q clean package -DskipTests

# ---- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /app

COPY --from=build /workspace/target/api-gateway.jar app.jar

# Run as a non-root user. A numeric UID needs no passwd entry, so the image does not depend on
# which users the base image ships (the earlier `useradd --uid 1000` broke once the base image
# gained its own UID 1000), and it matches OpenShift's arbitrary UIDs.
USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
