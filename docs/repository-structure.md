# CloudShip — Repository Structure Design & Evolution (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Structural Overview

The CloudShip repository is organized as a clean, cohesive monorepo engineered for progressive evolution. In accordance with the project's engineering principles, **directories are introduced strictly as their corresponding phases arrive** rather than pre-populating empty placeholders prematurely.

```text
CloudShip/
├── .github/                 # GitHub workflows, issue/PR templates (Phase 1-2)
├── backend/                 # Java / Spring Boot application source (Phase 1)
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml / build.gradle
│   └── README.md
├── frontend/                # Web Dashboard UI (HTML5, CSS3, Vanilla JS) (Phase 1)
│   ├── public/
│   ├── src/
│   └── README.md
├── database/                # Database migrations (Flyway SQL scripts) (Phase 1)
│   ├── migrations/
│   └── README.md
├── docker/                  # Dockerfiles, multi-stage definitions & compose (Phase 2)
│   ├── Dockerfile
│   ├── Dockerfile.dev
│   └── docker-compose.yml
├── jenkins/                 # Jenkins declarative pipeline definitions (Phase 3)
│   ├── Jenkinsfile
│   └── scripts/
├── azure/                   # Azure Bicep / Terraform / CLI deployment scripts (Phase 4)
│   ├── main.bicep
│   └── scripts/
├── k8s/                     # Kubernetes manifests & Kustomize overlays (Phase 6)
│   ├── base/
│   └── overlays/
├── scripts/                 # Operational automation & validation scripts (Phase 2+)
│   ├── dev-setup.sh
│   ├── health-check.sh
│   └── simulate-failure.sh
├── docs/                    # Complete architectural and operational specifications (Phase 0)
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
├── .env.example             # Template for all environment variables (Phase 0)
├── .gitignore               # Strict version control exclusion rules (Phase 0)
├── LICENSE                  # MIT License (Phase 0)
└── README.md                # System overview and entry point (Phase 0)
```

---

## 2. Directory Rationale & Phase Mapping

| Directory | Introduction Phase | Purpose & Content |
|---|---|---|
| `docs/` | **Phase 0 (Active)** | Authoritative system documentation, requirements, Azure cloud strategy, architecture, security, guidelines, and roadmaps. |
| `backend/` | **Phase 1** | Spring Boot REST API application, business logic, health probes, deployment supervisors, and unit tests. |
| `frontend/` | **Phase 1** | Lightweight client dashboard for tracking deployments, inspecting logs, and triggering actions (optionally previewed on Vercel). |
| `database/` | **Phase 1** | Schema DDL and versioned Flyway SQL migration scripts managing tables, indexes, and constraints. |
| `docker/` | **Phase 2** | Multi-stage Dockerfile configurations, `.dockerignore`, and local development compose files. |
| `jenkins/` | **Phase 3** | Declarative `Jenkinsfile` definitions orchestrating checkout, test, build, scan, and deploy stages. |
| `azure/` | **Phase 4** | Azure infrastructure definitions (Resource Groups, VNets, NSGs, ACR setup scripts, Bicep templates). |
| `k8s/` | **Phase 6** | Declarative Kubernetes manifests (`Deployment`, `Service`, `ConfigMap`, `Secret`, `Ingress`). |
| `scripts/` | **Phase 2–4** | Deterministic shell scripts for health checking, Azure provisioning, and failure simulation drills. |
| `cloud/aws/` | **Phase 13 (Future)** | AWS infrastructure definitions introduced strictly during the multi-cloud expansion phase. |

---

## 3. Directory Growth Rules

1. **No Phantom Artifacts**: A directory must not be committed without concrete contents or a clearly defined placeholder with a descriptive `README.md`.
2. **Separation of Infrastructure**: Cloud-specific code (`azure/`, future `cloud/aws/`) is separated cleanly from application business code (`backend/`, `frontend/`).
3. **Zero AWS Code Before Phase 13**: No AWS directories or configuration files are created until Phase 13.
