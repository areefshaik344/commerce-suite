# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY backend/gradlew backend/gradlew
COPY backend/gradle backend/gradle
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/
COPY backend/src backend/src
WORKDIR /workspace/backend
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app \
 && apk add --no-cache curl tini
WORKDIR /app
COPY --from=builder /workspace/backend/build/libs/*.jar /app/app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["/sbin/tini","--","sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
