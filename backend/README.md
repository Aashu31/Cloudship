# CloudShip — Backend Service

## Overview
This directory houses the core Java Spring Boot service responsible for CloudShip's REST API, deployment orchestration, health probes, and automated recovery supervisory loop.

## Target Technology Stack (Phase 1)
- **Language**: Java 17 or 21 (OpenJDK / Eclipse Temurin)
- **Framework**: Spring Boot 3.x
- **Modules**:
  - `spring-boot-starter-web` (REST endpoints)
  - `spring-boot-starter-actuator` (Liveness & Readiness health probes)
  - `spring-boot-starter-data-jpa` (Persistence)
  - `flyway-core` (Database migrations)
  - `postgresql` (PostgreSQL JDBC driver)
- **Build System**: Maven (`pom.xml`) or Gradle (`build.gradle`)

## Target Structure (Phase 1 Implementation)
```text
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/cloudship/app/
│   │   │   ├── CloudShipApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/cloudship/app/
```

## Phase Status
- **Current State**: Phase 0 Scaffold.
- **Next Action**: Initialize Spring Boot application skeleton in **Phase 1**.
