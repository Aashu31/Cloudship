# CloudShip — Local Development Guide

**Document Version:** 1.0.0  
**Phase:** Phase 1 (Application Foundation)  
**Status:** Active  

---

## 1. Architecture & Local Execution Overview

CloudShip Phase 1 runs **100% locally** and does **not** require any cloud subscriptions (Azure or AWS), Kubernetes, or Jenkins.

The local development stack consists of:
1. **PostgreSQL 15+** database (running on port `5433` or `5432`)
2. **Java 17+ / Spring Boot 3.3.3** backend REST API (running on port `8088`)
3. **HTML5 / CSS3 / Vanilla JavaScript** operational dashboard frontend

```text
  Browser Dashboard ──────── HTTP / Fetch ────────► Spring Boot API (:8088)
(frontend/index.html)                                      │
                                                    HikariCP / JPA
                                                           ▼
                                                 PostgreSQL Database (:5433)
```

---

## 2. Prerequisites

| Component | Minimum Version | Verified With |
|---|---|---|
| **Java JDK** | OpenJDK 17+ | Eclipse Temurin 17.0.20.1 |
| **Build Tool** | Apache Maven 3.9+ | Maven Wrapper (`./mvnw` / `mvnw.cmd`) |
| **Database** | PostgreSQL 15+ | PostgreSQL 18.6 |
| **Web Browser** | Chrome, Edge, Firefox, Safari | Any modern browser |

---

## 3. Step-by-Step Local Setup

### Step 1: Clone and Enter Repository
```bash
git clone https://github.com/Aashu31/Cloudship.git
cd Cloudship
```

### Step 2: Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Ensure the database and port settings match your local environment:
```env
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8088
DB_HOST=localhost
DB_PORT=5433
DB_NAME=cloudship_dev
DB_USERNAME=cloudship
DB_PASSWORD=cloudship_dev_password
```

### Step 3: Start Local PostgreSQL Database
You can use the provided automated startup script:
```powershell
# PowerShell:
powershell -ExecutionPolicy Bypass -File ./database/start-local-db.ps1
```
Or use Docker:
```bash
docker run -d --name cloudship-postgres \
  -e POSTGRES_DB=cloudship_dev \
  -e POSTGRES_USER=cloudship \
  -e POSTGRES_PASSWORD=cloudship_dev_password \
  -p 5433:5432 \
  postgres:15-alpine
```

### Step 4: Build and Run Spring Boot Backend
Navigate to the `backend/` directory and start the application:
```powershell
cd backend
.\mvnw spring-boot:run
```
Upon startup:
- Flyway automatically applies all versioned migrations (`V1__init_schema.sql`).
- The application binds to port `8088`.
- The health probe is accessible at `http://localhost:8088/api/health`.

### Step 5: Launch the Frontend Dashboard
Open `frontend/index.html` directly in any web browser, or serve it using any lightweight static server:
```bash
# Option A: Open directly
start frontend/index.html

# Option B: Using python http.server
python -m http.server 3000 --directory frontend
```
The dashboard will connect to `http://localhost:8088`, display real-time **Backend: CONNECTED** and **Database: CONNECTED** status badges, and allow you to manage CloudShip projects.

---

## 4. Running Automated Tests

Run the full test suite (including context boot, controllers, service validation, and MockMvc tests):
```powershell
cd backend
.\mvnw test
```
All unit and integration tests use an in-memory test profile with H2 in PostgreSQL compatibility mode and do not affect the development database.

---

## 5. Troubleshooting

- **Port 8080 Conflict**: If port 8080 is occupied by an existing web server (e.g. Apache/IIS), CloudShip defaults to port `8088` (configurable via `SERVER_PORT` in `.env`).
- **PostgreSQL Connection Refused**: Verify that PostgreSQL is running on the port configured in `.env` (`DB_PORT=5433`).
