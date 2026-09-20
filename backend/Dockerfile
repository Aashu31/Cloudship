# ==============================================================================
# CloudShip Backend — Multi-Stage Production Dockerfile
# ==============================================================================
# Stage 1: Build application using Eclipse Temurin Java 17
# ==============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Layer caching: Copy Maven wrapper and POM first
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production artifact
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal, Hardened Runtime Image
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create unprivileged system group and user
RUN addgroup -S cloudship && adduser -S cloudship -G cloudship

# Copy built artifact from builder stage
COPY --from=builder /workspace/target/cloudship-backend-*.jar /app/app.jar
RUN chown -R cloudship:cloudship /app

# Switch to non-root user
USER cloudship:cloudship

# Expose backend port
EXPOSE 8088

# Application environment configuration
ENV SERVER_PORT=8088 \
    SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8088/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
