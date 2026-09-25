# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root system user for security
RUN addgroup -S cloudship && adduser -S cloudship -G cloudship
USER cloudship

# Copy built jar from build stage (version-agnostic wildcard)
COPY --from=build --chown=cloudship:cloudship /app/target/*.jar app.jar

ENV PORT=8088
EXPOSE 8088

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
