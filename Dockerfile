# syntax=docker/dockerfile:1
# One Dockerfile for every service. Pick the module with --build-arg SERVICE=booking-service

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
# The cache mount keeps downloaded dependencies between builds, so rebuilds take seconds
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -DskipTests -pl ${SERVICE} -am package

# ---- Runtime stage: small JRE image, non-root user ----
FROM eclipse-temurin:17-jre
ARG SERVICE
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}.jar app.jar
USER app
# Respect container memory limits (important on Kubernetes)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
