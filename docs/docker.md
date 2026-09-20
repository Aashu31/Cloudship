# Docker Container Foundation & Architecture

## 1. Overview

CloudShip Phase 2 establishes the containerization foundation. The platform packages services into production-ready OCI containers with multi-stage builds, non-root execution, automated healthchecks, and local orchestration via Docker Compose.

---

## 2. Architecture & Components

```text
┌────────────────────────────────────────────────────────┐
│                   Docker Compose Stack                 │
│                                                        │
│   ┌────────────────┐           ┌──────────────────┐    │
│   │    frontend    │  /api/*   │     backend      │    │
│   │  (Nginx 1.25)  ├──────────►│  (Temurin 17)    │    │
│   │   Port: 80     │           │   Port: 8088     │    │
│   └───────┬────────┘           └────────┬─────────┘    │
│           │                             │              │
│           │                             │ JDBC         │
│           ▼                             ▼              │
│   ┌───────────────────────────────────────────────┐    │
│   │                   database                    │    │
│   │                (PostgreSQL 18)                │    │
│   │                  Port: 5432                   │    │
│   └───────────────────────────────────────────────┘    │
│                                                        │
│           cloudship-network (Internal Bridge)          │
└────────────────────────────────────────────────────────┘
```

---

## 3. Multi-Stage Backend Dockerfile (`backend/Dockerfile`)

The backend uses a two-stage build to ensure minimal final image footprint and security:

### Stage 1: Build & Package (`eclipse-temurin:17-jdk-alpine`)
- Caches Maven wrapper dependencies using `.mvn/` and `pom.xml`.
- Compiles source code with `mvn clean package -DskipTests -B`.
- Produces a self-contained Spring Boot executable JAR in `/workspace/target/`.

### Stage 2: Hardened Runtime (`eclipse-temurin:17-jre-alpine`)
- Uses lightweight Alpine JRE for minimal attack surface.
- Creates a dedicated unprivileged system group and user:
  ```dockerfile
  RUN addgroup -S cloudship && adduser -S cloudship -G cloudship
  ```
- Copies only the compiled artifact (`app.jar`).
- Enforces non-root execution: `USER cloudship:cloudship`.
- Exposes port `8088`.
- Defines an automated Docker healthcheck:
  ```dockerfile
  HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
      CMD wget --no-verbose --tries=1 --spider http://localhost:8088/api/health || exit 1
  ```

---

## 4. Frontend Dockerfile & Nginx (`frontend/Dockerfile`)

The frontend is served via an Alpine-based Nginx container:

- Base: `nginx:alpine`
- Configuration (`frontend/nginx.conf`):
  - Serves static assets (`index.html`, CSS, JS, images) from `/usr/share/nginx/html`.
  - Configures reverse proxy for `/api/` routing traffic directly to `http://backend:8088/api/`.
  - Sets security headers: `X-Frame-Options DENY`, `X-Content-Type-Options nosniff`, `X-XSS-Protection "1; mode=block"`.
  - Gzip compression enabled for HTML, CSS, JS, JSON, and SVG.
- Exposes port `80`.
- Healthcheck: `wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1`.

---

## 5. Docker Compose Local Stack (`docker-compose.yml`)

The multi-container stack orchestrates all three core tiers:

| Service | Image / Build Target | Ports (Host:Container) | Depends On | Healthcheck |
|---|---|---|---|---|
| **`database`** | `postgres:18-alpine` | `5433:5432` | None | `pg_isready -U cloudship` |
| **`backend`** | `./backend/Dockerfile` | `8088:8088` | `database` (healthy) | `wget -q /api/health` |
| **`frontend`** | `./frontend/Dockerfile` | `80:80` | `backend` (healthy) | `wget -q http://localhost:80/` |

### Environment Configuration

Variables are passed via the root `.env` file (templated in `.env.example`):
- `POSTGRES_DB=cloudship_dev`
- `POSTGRES_USER=cloudship`
- `POSTGRES_PASSWORD=cloudship_secret`
- `SERVER_PORT=8088`
- `SPRING_PROFILES_ACTIVE=dev`

---

## 6. Build Information API (`GET /api/build-info`)

CloudShip provides a live build and container metadata endpoint:

- **Endpoint**: `GET /api/build-info`
- **Response**:
  ```json
  {
    "application": "cloudship-backend",
    "version": "1.0.0",
    "environment": "Local Dev",
    "dockerImage": "cloudship/backend:1.0.0",
    "javaVersion": "17.0.20.1",
    "containerStatus": "READY",
    "timestamp": "2026-09-21T04:44:10.779Z"
  }
  ```

---

## 7. Command Reference

### Build & Run Locally with Docker

1. **Build Backend Container**:
   ```bash
   docker build -t cloudship-backend:latest ./backend
   ```

2. **Run Backend Container**:
   ```bash
   docker run -d --name cloudship-backend -p 8088:8088 \
     -e DB_HOST=host.docker.internal \
     -e DB_PORT=5432 \
     -e DB_NAME=cloudship_dev \
     -e DB_USER=cloudship \
     -e DB_PASSWORD=cloudship_secret \
     cloudship-backend:latest
   ```

3. **Build Frontend Container**:
   ```bash
   docker build -t cloudship-frontend:latest ./frontend
   ```

4. **Launch Entire Stack with Docker Compose**:
   ```bash
   docker compose up -d
   ```

5. **Inspect Running Containers & Health**:
   ```bash
   docker compose ps
   ```

6. **View Real-Time Logs**:
   ```bash
   docker compose logs -f backend
   ```

7. **Tear Down Stack**:
   ```bash
   docker compose down -v
   ```
