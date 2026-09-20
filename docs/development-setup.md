# CloudShip — Development Environment Setup Guide (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Overview & Progressive Tooling Model

CloudShip strictly follows a **progressive tooling model**. You do **not** need cloud accounts or Kubernetes tools installed on day one. Tool requirements are split into three explicit operational tiers:
1. **Local Development (Required Now / Phase 1)**
2. **Azure Cloud Deployment (Required Later / Phases 2–12)**
3. **Future AWS Multi-Cloud Deployment (Phase 13 Only)**

```text
Local Foundation (Phase 0/1) ────► Container & CI (Phase 2/3) ────► Azure Cloud (Phase 4+)
(Java, Postgres, Git, Docker)     (Docker, Jenkins)                  (Azure CLI, ACR, K8s)
                                                                             │
                                                                             ▼
                                                                Future AWS Multi-Cloud (Phase 13)
                                                                (AWS CLI, ECR, EKS)
```

---

## 2. Tooling Requirements Matrix

### 2.1 Tier 1: Local Development (Required Now — Phase 0 & Phase 1)

These tools are required immediately for local application engineering and database setup:

| Tool | Recommended Version | Verification Command | Purpose in Phase 1 |
|---|---|---|---|
| **Git** | `2.40+` | `git --version` | Version control & GitHub synchronization |
| **GitHub Account** | Active account | Access to `Aashu31/Cloudship` | Remote code hosting and webhook integration |
| **Java Development Kit (JDK)** | `OpenJDK 17` or `21` (Temurin/Corretto) | `java -version` | Core backend compilation and runtime |
| **Build Tool** | `Maven 3.9+` or `Gradle 8+` (or wrapper) | `mvn -version` or `./gradlew -v` | Dependency management & test suite runner |
| **PostgreSQL** | `15+` (local service or dev container) | `psql --version` | Persistence engine for deployments/incidents |
| **Docker Engine** | `24+` | `docker --version` | Local database/services containerization |
| **Bash / Shell** | Standard Linux shell or Git Bash / WSL2 | `bash --version` | Operational automation scripts |
| **Web Browser** | Any modern browser | N/A | Testing frontend UI dashboard |
| **Node.js** | Optional (only if bundlers used) | `node --version` | Optional (vanilla JS used by default) |

### 2.2 Tier 2: Azure Cloud Deployment (Required Later — Phases 2 through 12)

Do **not** configure these until reaching the corresponding phase:

| Tool | Introduction Phase | Verification Command | Purpose |
|---|---|---|---|
| **Jenkins** | **Phase 3** | Browser `http://localhost:8080` | Local/VM CI execution engine |
| **Azure CLI (`az`)** | **Phase 4** | `az --version` | Provisioning Azure resources, ACR login |
| **Azure Subscription** | **Phase 4** | `az account show` | Active Azure for Students subscription |
| **Azure Container Registry** | **Phase 5** | `az acr list` | Private cloud container registry |
| **kubectl** | **Phase 6** | `kubectl version --client` | Kubernetes cluster control |
| **Minikube / kind** | **Phase 6** | `minikube status` / `kind version` | Local Kubernetes sandbox before cloud deployment |
| **Azure Monitor** | **Phase 9** | Azure Portal inspection | Cloud telemetry & log analytics |

### 2.3 Tier 3: Future AWS Multi-Cloud Deployment (Phase 13 Only)

> [!IMPORTANT]
> **AWS CLI & AWS Credentials are NOT required for Phase 1 through 12**:
> - **AWS CLI (`aws`)**: Phase 13 only.
> - **AWS Account**: Phase 13 only.
> - Under no circumstances should AWS setup block MVP development.

---

## 3. Step-by-Step Setup Guide for Phase 1

### Step 1: Verify Git Configuration
Ensure Git user identity is established:
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### Step 2: Verify Java 17/21 Installation
Ensure OpenJDK 17 or 21 is active:
```bash
java -version
```

### Step 3: Local PostgreSQL Configuration
Launch PostgreSQL locally or via a lightweight Docker container:
```bash
docker run -d --name cloudship-postgres \
  -e POSTGRES_DB=cloudship_dev \
  -e POSTGRES_USER=cloudship \
  -e POSTGRES_PASSWORD=cloudship_dev_password \
  -p 5432:5432 \
  postgres:15-alpine
```

### Step 4: Environment Variables Template
Initialize your local `.env` from the provided `.env.example`:
```bash
cp .env.example .env
```
*(Never commit `.env` to version control; it is excluded by `.gitignore`.)*

---

## 4. Phase 1 Verification Checklist

- [x] Workspace connected to `https://github.com/Aashu31/Cloudship` on branch `main`.
- [ ] Java 17+ installed and verified (`java -version`).
- [ ] Maven or Gradle build tool accessible.
- [ ] PostgreSQL reachable on `localhost:5432`.
- [ ] `.env` file generated from `.env.example`.
