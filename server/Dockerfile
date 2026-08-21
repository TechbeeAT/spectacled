# syntax=docker/dockerfile:1

# Build the Ktor proxy as a self-contained fat JAR.
# Build context is the repository ROOT (the server module depends on :shared), e.g.:
#   docker build -f server/Dockerfile -t spectacled-proxy .
# Pin the build stage to the *builder's* native architecture. The produced fat JAR is
# architecture-independent Java bytecode, so we build it once (natively, no emulation) and
# only the small runtime stage below is materialised per target architecture.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Warm the Gradle wrapper cache separately from sources for better layer caching.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Copy the rest of the project and build only the server fat JAR.
COPY . .
RUN ./gradlew --no-daemon :server:buildFatJar

# Minimal JRE runtime image.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 10001 --create-home appuser
USER appuser

COPY --from=build /app/server/build/libs/server-all.jar /app/server.jar

# PaaS platforms (Fly.io, Render, …) inject PORT; the app honours it, defaulting to 8088.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
