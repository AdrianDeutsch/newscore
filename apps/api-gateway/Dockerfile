# syntax=docker/dockerfile:1

# ---- Build stage ---------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Resolve dependencies first for better layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Build the application (tests run in CI, not in the image build)
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as a non-root user (OpenShift-friendly)
RUN useradd --uid 1000 --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /workspace/target/api-gateway.jar app.jar
USER 1000

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
