# CloudShip — Intelligent DevOps Deployment & Recovery Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Primary Cloud: Azure](https://img.shields.io/badge/Primary%20Cloud-Microsoft%20Azure-0078D4.svg)](docs/cloud-strategy.md)
[![Future Cloud: AWS](https://img.shields.io/badge/Future%20Cloud-AWS%20(Phase%2013)-FF9900.svg)](docs/cloud-strategy.md)
[![Phase](https://img.shields.io/badge/Phase-2%20(GitHub%20+%20Docker)-success.svg)](docs/roadmap.md)
[![Status](https://img.shields.io/badge/Status-Phase%202%20Complete-brightgreen.svg)](docs/roadmap.md)

**CloudShip** is an intelligent DevOps deployment and recovery platform designed to demonstrate real-world continuous delivery, container orchestration, real-time health observability, and automated site reliability engineering (SRE) recovery.

The end-to-end operational pipeline progresses through:
```text
Git → GitHub → Jenkins → Build/Test → Docker → Azure Container Registry → Kubernetes → Azure → Monitoring → Health Checks → Rollback → Failure Simulation → Automated Recovery → Future AWS
```

---

## 1. The Problem

In modern cloud environments:
- **Silent Deployment Failures Cause Extended Downtime**: Releases often fail silently due to bad configurations, memory exhaustion, or database connection leaks. Operators often discover outages only after end-users report them.
- **Manual Rollbacks Under Pressure are Error-Prone**: Manually issuing rollback commands during production outages increases Mean Time to Recovery (MTTR) and introduces human error.
- **Fragmented Tooling**: CI/CD pipelines, container registries, Kubernetes clusters, and cloud monitoring operate in silos without a unified control plane.
- **Lack of Realistic Failure Drills**: Engineering teams rarely practice recovery procedures prior to actual outages, leaving systems vulnerable when unexpected disruptions strike.

---

## 2. The Solution

CloudShip provides a unified deployment and self-healing platform that:
1. **Automates Delivery**: Pulls code from GitHub, validates with automated unit tests in Jenkins, packages hardened multi-stage Docker images, and pushes to **Azure Container Registry (ACR)**.
2. **Orchestrates Kubernetes Deployments**: Deploys multi-replica workloads to Kubernetes using zero-downtime rolling updates.
3. **Actively Monitors Health**: Continually polls Spring Boot Actuator Liveness (`/actuator/health/liveness`) and Readiness (`/actuator/health/readiness`) probes.
4. **Automates Rollbacks**: Automatically detects unrecoverable deployment regressions or repeated probe failures, instantly issuing `kubectl rollout undo` to restore the last-known stable revision.
5. **Simulates Real-World Outages**: Provides secured diagnostic endpoints to inject controlled faults (probe degradation, CPU spikes, database disconnects) to validate recovery automation.
6. **Maintains Audit Records**: Persists every deployment attempt, health status transition, and automated recovery incident in PostgreSQL with calculated MTTR metrics.

---

## 3. Cloud Strategy: Azure-First & Cloud-Agnostic Design

> [!IMPORTANT]
> **Primary Cloud: Microsoft Azure**  
> CloudShip is engineered primarily for **Microsoft Azure** utilizing an Azure for Students subscription.  
> **AWS Multi-Cloud: Future Scope (Phase 13)**  
> AWS access is not currently available and is **NOT required for the MVP**. The system utilizes a provider-agnostic deployment interface (`CloudDeploymentProvider`) so that AWS can be introduced in Phase 13 without rewriting core application logic.
> **Frontend Hosting**: Vercel may be used optionally for static UI edge previewing; it does **not** replace Azure cloud infrastructure.

For detailed cloud strategy, see [docs/cloud-strategy.md](docs/cloud-strategy.md).

---

## 4. High-Level Architecture

```text
Developer
   │
   ▼
GitHub (Source Repository: Aashu31/Cloudship)
   │
   ▼
Jenkins (Declarative CI/CD) ──► Maven Build & Unit Tests ──► Multi-stage Docker Packaging
   │
   ▼
Azure Container Registry (ACR: cloudshipcr.azurecr.io)
   │
   ▼
Kubernetes (Local Minikube/kind or Azure AKS) ────► Rolling Update Deployment (2 Replicas)
   │                                                         │
   ▼                                                         ▼
Spring Boot Application                                 Kubelet Probes (/actuator/health/*)
   │                                                         │
   ▼                                                         ▼
PostgreSQL (Local Container / Azure PG)                 CloudShip SRE Supervisor
                                                             │
                                                    [Degraded Health?]
                                                             │
                             YES ────────────────────────────┴──────────────────────────── NO
                              │                                                             │
                              ▼                                                             ▼
                     Autonomous Rollback                                           Deployment Succeeded
                   (kubectl rollout undo)                                          (Audit Recorded)
```

For complete technical specifications and Mermaid diagrams, see:
- [docs/architecture.md](docs/architecture.md)
- [docs/architecture-diagram.md](docs/architecture-diagram.md)
- [docs/github-integration.md](docs/github-integration.md)
- [docs/docker.md](docs/docker.md)

---

## 5. Primary Technology Stack

| Layer | Primary Technology | Selection Rationale |
|---|---|---|
| **Application Core** | Java 17/21 + Spring Boot 3.x | Enterprise reliability, native Actuator health probes, strong typing |
| **Frontend UI** | HTML5, CSS3, Vanilla JavaScript | Zero-dependency, responsive operational dashboard (optional Vercel preview) |
| **Persistence** | PostgreSQL 15+ & Flyway | ACID-compliant relational store for deployment records, incidents, and audit trails |
| **Version Control** | Git & GitHub | Distributed version control, webhook integration, branch protection |
| **CI Automation** | Jenkins LTS | Declarative pipeline automation, disposable containerized build agents |
| **Containerization** | Docker | Multi-stage builds, non-root execution (`USER 10001`), lightweight Alpine runtime |
| **Container Registry** | Azure Container Registry (ACR) | Private OCI registry on Azure under low-cost Basic SKU |
| **Orchestration** | Kubernetes (Minikube / kind / AKS) | Declarative rolling updates, self-healing pod management, Liveness/Readiness probes |
| **Primary Cloud** | Microsoft Azure | Azure Resource Groups, VNets, NSGs, B-series VMs, Managed Identities |
| **Observability** | Azure Monitor, App Insights, Actuator | Real-time health signals, structured JSON logs, and live telemetry |
| **Future Multi-Cloud** | AWS (ECR, EKS, VPC) | Phased multi-cloud expansion target (**Phase 13 only**) |

---

## 6. Current Project Status & Phased Roadmap

| Phase | Description | Status | Verification / Key Deliverable |
|---|---|---|---|
| **Phase 0** | **Foundation & Architecture** | **`[x] Completed`** | Comprehensive `docs/`, `.env.example`, `.gitignore`, GitHub origin linked |
| **Phase 1** | **Application Foundation** | **`[x] Completed`** | Spring Boot 3 API, PostgreSQL, Flyway, Health Probes, UI |
| **Phase 2** | **GitHub + Docker Foundation** | **`[x] Completed`** | Multi-stage Dockerfile, docker-compose stack, GitHub repository APIs, Build Info |
| **Phase 3** | **Jenkins CI** | `[ ] Planned` | Declarative `Jenkinsfile`, automated test stages |
| **Phase 4** | **Azure Infrastructure** | `[ ] Planned` | Azure Resource Group, VNet, Subnets, NSGs, B-series VMs |
| **Phase 5** | **Azure Container Registry** | `[ ] Planned` | Private ACR provisioning, CI image push via Service Principal |
| **Phase 6** | **Kubernetes** | `[ ] Planned` | Declarative K8s manifests, probes, rolling update config |
| **Phase 7** | **Complete CI/CD** | `[ ] Planned` | End-to-end Git push to Kubernetes rollout automation |
| **Phase 8** | **CloudShip Dashboard** | `[ ] Planned` | Responsive HTML5/CSS/JS operations web dashboard |
| **Phase 9** | **Monitoring** | `[ ] Planned` | Azure Monitor Log Analytics & Application Insights |
| **Phase 10** | **Rollback** | `[ ] Planned` | Programmatic and manual one-click rollback engine |
| **Phase 11** | **Failure Simulation** | `[ ] Planned` | Diagnostic endpoints simulating faults and crashes |
| **Phase 12** | **Automated Recovery** | `[ ] Planned` | Autonomous SRE loop detecting faults and auto-rolling back |
| **Phase 13** | **AWS Multi-Cloud [FUTURE]** | `[ ] Planned` | Secondary cloud deployment targeting AWS ECR & EKS |
| **Phase 14** | **Security Hardening** | `[ ] Planned` | Vulnerability scanning (Trivy/OWASP), Azure Key Vault, PSS |
| **Phase 15** | **Testing, Runbooks & Release** | `[ ] Planned` | Automated E2E test verification, final documentation, `v1.0.0` |

See [docs/roadmap.md](docs/roadmap.md) for full phase objectives, cost considerations, and Definitions of Done.

---

## 7. Repository Layout

```text
CloudShip/
├── docs/                    # Authoritative engineering specifications
│   ├── requirements.md
│   ├── cloud-strategy.md
│   ├── architecture.md
│   ├── architecture-diagram.md
│   ├── repository-structure.md
│   ├── development-setup.md
│   ├── environment-strategy.md
│   ├── security.md
│   ├── git-workflow.md
│   ├── jira-backlog.md
│   ├── roadmap.md
│   ├── coding-standards.md
│   ├── observability.md
│   ├── failure-recovery.md
│   └── cloud-cost-control.md
├── backend/                 # Java / Spring Boot application service (Phase 1)
├── frontend/                # Operations dashboard user interface (Phase 1)
├── database/                # PostgreSQL schema & Flyway migrations (Phase 1)
├── azure/                   # Azure Bicep / CLI infrastructure templates (Phase 4)
├── .env.example             # Template for all environment variables
├── .gitignore               # Comprehensive exclusion rules
├── LICENSE                  # MIT License
└── README.md                # Project entry point documentation
```

---

## 8. Local Development Setup (Phase 0 / Phase 1)

### Prerequisites for Local Development
- **JDK 17 or 21**: Verify via `java -version`
- **Maven 3.9+**: Verify via `mvn -version`
- **PostgreSQL 15+**: Local daemon or container
- **Git**: Verify via `git --version`
- **Docker & Docker Compose**: Optional for containerized orchestration

### Option A: Local Bare-Metal / CLI
1. **Clone the repository**:
   ```bash
   git clone https://github.com/Aashu31/Cloudship.git
   cd Cloudship
   ```
2. **Configure environment settings**:
   ```bash
   cp .env.example .env
   # Edit .env with your local credentials (default DB_PORT=5433, SERVER_PORT=8088)
   ```
3. **Start local PostgreSQL**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File ./database/start-local-db.ps1
   ```
4. **Run Spring Boot backend**:
   ```powershell
   cd backend
   .\mvnw spring-boot:run
   ```
5. **Open Frontend Dashboard**:
   Open `frontend/index.html` in your web browser.

### Option B: Containerized with Docker Compose (Phase 2)
1. **Launch the entire stack**:
   ```bash
   docker compose up -d
   ```
2. **Inspect running services & health**:
   ```bash
   docker compose ps
   ```
3. **Access endpoints**:
   - Web Dashboard: `http://localhost:80`
   - Backend API: `http://localhost:8088/api/health`
   - Build Information: `http://localhost:8088/api/build-info`

For complete details, refer to:
- [docs/local-development.md](docs/local-development.md) — Local development workflow
- [docs/github-integration.md](docs/github-integration.md) — GitHub connection guide & REST APIs
- [docs/docker.md](docs/docker.md) — Docker container foundation & Compose architecture
- [docs/api.md](docs/api.md) — Complete REST API contract & endpoints
- [docs/database.md](docs/database.md) — Schema design & Flyway migrations
- [docs/development-setup.md](docs/development-setup.md) — Progressive tooling model

---

## 9. Azure Deployment & FinOps Guidelines

- **Student Credit Protection**: Always deallocate VMs when not in use (`az vm deallocate`).
- **Local-First Verification**: Test workloads in local Docker / Minikube before deploying to Azure.
- **Resource Sizing**: Use `Standard_B1s` / `Standard_B2s` burstable VM instances and ACR Basic SKU.
- See [docs/cloud-cost-control.md](docs/cloud-cost-control.md) for complete FinOps policies.

---

## 10. Security Baseline

- **Zero Secret Storage**: No passwords, tokens, or cloud credentials may ever be committed to Git.
- **Azure RBAC Least Privilege**: Service Principals hold only `AcrPush` / `AcrPull` permissions.
- **Non-Root Execution**: Containerized workloads execute under unprivileged user `UID 10001`.
- For complete security specifications, see [docs/security.md](docs/security.md).

---

## 11. Contribution & Git Workflow

- Branches follow the format `feature/<ticket>-<description>` or `bugfix/<ticket>-<description>`.
- Commits must adhere to [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `ci:`, `infra:`).
- For branching model details, see [docs/git-workflow.md](docs/git-workflow.md).

---

## 12. License

This project is licensed under the terms of the [MIT License](LICENSE).
