# Multi-stage Dockerfile for Test Automation Service
# Optimized for ECS deployment

# Build stage
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy Gradle files first (for layer caching)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached if build.gradle hasn't changed)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy Karate features and configuration
COPY --from=build /app/src/test/java/features /app/features
COPY --from=build /app/src/test/java/karate-config.js /app/karate-config.js

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port (matches application.yml server.port)
EXPOSE 8080

# Health check for ECS
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/test-automation/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Spring Boot options
ENV SPRING_OPTS="--spring.profiles.active=prod"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $SPRING_OPTS"]

# Labels for container metadata
LABEL maintainer="test-automation-team@example.com" \
      version="1.0.0" \
      description="Karate-based test automation service" \
      service="test-automation-service"
