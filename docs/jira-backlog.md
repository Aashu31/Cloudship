# CloudShip — Jira-Ready Product Backlog (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  
**Primary Cloud:** Microsoft Azure  
**Future Cloud:** AWS Multi-Cloud (Epic 14)  

---

## Backlog Structure Overview
Structure: **EPIC → STORY → TASK**.  
Estimation Scale: Story Points (`1`, `2`, `3`, `5`, `8`, `13`).  
Priorities: `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`.

---

## EPIC 1: Project Foundation & Architecture (Phase 0)

### STORY CS-101: System Specifications & Azure Architecture Blueprint
- **Description**: Author comprehensive specifications, architecture diagrams, Azure-first cloud strategy, security baseline, and development setup documentation for CloudShip.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: None
- **Acceptance Criteria**:
  - `docs/requirements.md` defines functional, non-functional, Azure-first MVP boundaries.
  - `docs/cloud-strategy.md` documents Azure Student subscription FinOps and future AWS integration.
  - `docs/architecture.md` and `docs/architecture-diagram.md` detail all component layers with Mermaid flowcharts.
- **Tasks**:
  - Task CS-101.1: Author requirements and MVP scope separation matrix.
  - Task CS-101.2: Author cloud strategy for Azure-First and student credit conservation.
  - Task CS-101.3: Design system architecture diagrams and Mermaid workflows.

### STORY CS-102: Repository Scaffold & Git Baseline
- **Description**: Establish base repository layout, `.gitignore`, `.env.example`, and connect to remote GitHub repository `Aashu31/Cloudship`.
- **Priority**: `BLOCKER`
- **Estimate**: 2 pts
- **Dependencies**: CS-101
- **Acceptance Criteria**:
  - Remote GitHub repository `Aashu31/Cloudship` linked on `main` branch.
  - Comprehensive `.gitignore` protecting build artifacts and secrets.
  - Initial scaffolding for `backend/`, `frontend/`, and `database/` ready for Phase 1.
- **Tasks**:
  - Task CS-102.1: Clone and configure GitHub remote on `main`.
  - Task CS-102.2: Create `.gitignore` and `.env.example`.
  - Task CS-102.3: Add Phase 1 directory placeholders with clear README instructions.

---

## EPIC 2: Application Development (Phase 1)

### STORY CS-201: Spring Boot Application Core & Health Endpoints
- **Description**: Scaffold Java Spring Boot service with Spring Web, Actuator, and separate Liveness/Readiness probes.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: CS-102
- **Acceptance Criteria**:
  - Application boots on port `8080`.
  - `/actuator/health/liveness` returns `200 OK` (`UP`).
  - `/actuator/health/readiness` returns `200 OK` (`UP`).
  - Unit test suite passes with `mvn test` or `./gradlew test`.
- **Tasks**:
  - Task CS-201.1: Initialize Spring Boot project skeleton.
  - Task CS-201.2: Configure Spring Actuator health groups (`liveness`, `readiness`).
  - Task CS-201.3: Add unit tests verifying probe status.

### STORY CS-202: PostgreSQL Persistence & Flyway Migrations
- **Description**: Configure PostgreSQL data source with HikariCP connection pool and initialize Flyway migrations for deployment and incident tables.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-201
- **Acceptance Criteria**:
  - Flyway migration script `V1__init_schema.sql` creates `deployments` and `incidents` tables.
  - HikariCP pool establishes TLS connection with bounded timeout.
  - JPA entity classes and Spring Data repositories created.
- **Tasks**:
  - Task CS-202.1: Create Flyway migration script with proper indexes.
  - Task CS-202.2: Map `Deployment` and `Incident` JPA entities.
  - Task CS-202.3: Write repository integration tests.

### STORY CS-203: Deployment Management REST API
- **Description**: Implement REST endpoints to list, trigger, and inspect deployment runs.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-202
- **Acceptance Criteria**:
  - `POST /api/deployments` initiates a new deployment record.
  - `GET /api/deployments` returns paginated deployment history.
  - `GET /api/deployments/{id}` returns detailed deployment state.
- **Tasks**:
  - Task CS-203.1: Implement `DeploymentController` with DTO validation.
  - Task CS-203.2: Implement `DeploymentService` state machine.
  - Task CS-203.3: Add MockMvc controller tests.

---

## EPIC 3: Dockerization (Phase 2)

### STORY CS-301: Multi-Stage Dockerfile & Container Optimization
- **Description**: Author a production-grade multi-stage Dockerfile producing a secure, lightweight OCI image.
- **Priority**: `CRITICAL`
- **Estimate**: 3 pts
- **Dependencies**: CS-201
- **Acceptance Criteria**:
  - Multi-stage build compiles JAR in builder stage and runs in minimal Alpine JRE.
  - Container executes under non-root user `USER 10001`.
  - Final image size remains under 250MB.
  - `.dockerignore` excludes git history, secrets, and local build outputs.
- **Tasks**:
  - Task CS-301.1: Write multi-stage `Dockerfile`.
  - Task CS-301.2: Create `.dockerignore`.
  - Task CS-301.3: Verify non-root UID inside running container.

### STORY CS-302: Local Multi-Container Development Environment
- **Description**: Configure `docker-compose.yml` orchestrating CloudShip application and PostgreSQL with volume persistence.
- **Priority**: `MAJOR`
- **Estimate**: 3 pts
- **Dependencies**: CS-301, CS-202
- **Acceptance Criteria**:
  - `docker compose up` brings up PostgreSQL and application in an isolated network.
  - Application auto-runs Flyway migrations against the PostgreSQL container.
- **Tasks**:
  - Task CS-302.1: Create `docker-compose.yml` with health checks on DB.
  - Task CS-302.2: Verify container networking and environment injection.

---

## EPIC 4: Jenkins CI Pipeline (Phase 3)

### STORY CS-401: Declarative Jenkins Pipeline Definition
- **Description**: Author a robust `Jenkinsfile` executing checkout, Maven compilation, unit testing, and Docker packaging.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: CS-301
- **Acceptance Criteria**:
  - `Jenkinsfile` with distinct stages: `Checkout`, `Build & Test`, `Docker Build`, `Security Scan`.
  - Pipeline fails fast on test failures with explicit stage reporting.
  - Artifacts and test reports archived upon completion.
- **Tasks**:
  - Task CS-401.1: Write declarative `Jenkinsfile`.
  - Task CS-401.2: Configure pipeline parameterization for branch and commit SHA.
  - Task CS-401.3: Validate pipeline execution on local Jenkins instance.

---

## EPIC 5: Azure Cloud Infrastructure (Phase 4)

### STORY CS-501: Azure Foundation Network & Resource Group Setup
- **Description**: Provision isolated Azure Resource Group, Virtual Network (VNet), Subnets, and Network Security Groups (NSGs).
- **Priority**: `CRITICAL`
- **Estimate**: 8 pts
- **Dependencies**: CS-101
- **Acceptance Criteria**:
  - Resource Group `rg-cloudship-dev` created in `eastus`.
  - VNet `vnet-cloudship` created with compute and database subnets.
  - NSG rules enforce default-deny ingress posture.
  - FinOps cost-control rules adhered to (no expensive unneeded resources).
- **Tasks**:
  - Task CS-501.1: Author Azure CLI / Bicep template for Resource Group & VNet.
  - Task CS-501.2: Configure Subnets and Network Security Groups.

---

## EPIC 6: Azure Container Registry (Phase 5)

### STORY CS-601: ACR Provisioning & CI Publishing Integration
- **Description**: Provision private Azure Container Registry (`cloudshipcr`) and configure Jenkins pipeline with Azure Service Principal (`AcrPush` role) to push tagged images.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: CS-401, CS-501
- **Acceptance Criteria**:
  - Private ACR created under Basic SKU to minimize student credit burn.
  - Service principal authenticates and pushes image without enabling legacy admin account.
  - Images tagged with Git commit SHA and semantic version.
- **Tasks**:
  - Task CS-601.1: Provision ACR via Azure CLI with retention policy.
  - Task CS-601.2: Add `Push to ACR` stage in `Jenkinsfile`.

---

## EPIC 7: Kubernetes Deployment Architecture (Phase 6)

### STORY CS-701: Declarative Kubernetes Manifests & Probing
- **Description**: Author Kubernetes manifests (`Deployment`, `Service`, `ConfigMap`, `Secret`, `Ingress`) with rolling update and health probes.
- **Priority**: `CRITICAL`
- **Estimate**: 8 pts
- **Dependencies**: CS-601
- **Acceptance Criteria**:
  - Deployment specifies 2 replicas with `rollingUpdate` (`maxSurge: 1`, `maxUnavailable: 0`).
  - Liveness and Readiness probes configured pointing to `/actuator/health/*`.
  - Resource requests (`100m CPU`, `256Mi RAM`) and limits (`500m CPU`, `512Mi RAM`) defined.
  - Pod Security Context enforces non-root UID 10001.
- **Tasks**:
  - Task CS-701.1: Write `deployment.yaml` and `service.yaml`.
  - Task CS-701.2: Write `configmap.yaml` and `secret.yaml` templates.
  - Task CS-701.3: Validate deployment on local Minikube/kind cluster.

---

## EPIC 8: End-to-End CI/CD Automation (Phase 7)

### STORY CS-801: Automated GitHub Webhook & Continuous Delivery
- **Description**: Connect GitHub webhook to Jenkins to automatically build, push to ACR, and deploy new commits to Kubernetes.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: CS-401, CS-701
- **Acceptance Criteria**:
  - Git push triggers pipeline automatically.
  - Successful image build triggers rollout update.
  - Pipeline waits for rollout completion (`kubectl rollout status`).
- **Tasks**:
  - Task CS-801.1: Configure GitHub webhook payload handler.
  - Task CS-801.2: Implement `Deploy to Kubernetes` stage in Jenkins.

---

## EPIC 9: CloudShip Operations Dashboard (Phase 8)

### STORY CS-901: Web Dashboard UI Implementation
- **Description**: Build a clean, responsive frontend dashboard to visualize deployment status, pod health, and operational controls (with optional Vercel static preview).
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-203
- **Acceptance Criteria**:
  - Dashboard displays current active deployment version and commit SHA.
  - Visual health status pill (GREEN = Healthy, YELLOW = Rolling out, RED = Degraded).
  - List of past deployments with status badges and duration.
- **Tasks**:
  - Task CS-901.1: Build semantic HTML5 layout with CSS grid/flexbox styling.
  - Task CS-901.2: Implement Vanilla JavaScript client querying `/api/deployments`.
  - Task CS-901.3: Add real-time auto-refresh mechanism.

---

## EPIC 10: Monitoring & Telemetry Integration (Phase 9)

### STORY CS-1001: Azure Monitor & Application Insights Pipeline
- **Description**: Expose Spring Boot Actuator metrics and stream structured container logs into Azure Monitor Log Analytics.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-701
- **Acceptance Criteria**:
  - Application Insights traces HTTP requests and response latencies.
  - Container logs stream into Azure Log Analytics workspace.
- **Tasks**:
  - Task CS-1001.1: Configure Azure Application Insights Java agent.
  - Task CS-1001.2: Configure Log Analytics workspace and ingestion rules.

---

## EPIC 11: Deployment Rollback Engine (Phase 10)

### STORY CS-1101: Manual & Programmatic Deployment Rollback
- **Description**: Implement API and UI controls to trigger instantaneous rollback of a running deployment to the previous stable revision.
- **Priority**: `CRITICAL`
- **Estimate**: 5 pts
- **Dependencies**: CS-203, CS-701
- **Acceptance Criteria**:
  - `POST /api/deployments/{id}/rollback` executes `kubectl rollout undo`.
  - Rollout undo returns cluster to previous replica set without downtime.
  - Event recorded in PostgreSQL as `ROLLED_BACK`.
- **Tasks**:
  - Task CS-1101.1: Implement rollback service method wrapping Kubernetes client.
  - Task CS-1101.2: Add Rollback button to web dashboard with confirmation modal.

---

## EPIC 12: Failure Simulation Engine (Phase 11)

### STORY CS-1201: Controllable Failure Injection Endpoints
- **Description**: Add secured diagnostic endpoints to simulate application crashes, probe failures, and high CPU load.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-201
- **Acceptance Criteria**:
  - `POST /api/simulation/readiness-failure` causes readiness probe to return 503.
  - `POST /api/simulation/cpu-spike` safely burns CPU for 30 seconds before auto-recovering.
  - Simulators include TTL auto-reset to avoid permanent cluster degradation.
- **Tasks**:
  - Task CS-1201.1: Build `SimulationController` with security guards.
  - Task CS-1201.2: Implement custom `HealthIndicator` toggleable via API.

---

## EPIC 13: Automated Self-Healing & Recovery (Phase 12)

### STORY CS-1301: Autonomous Health Monitor & Auto-Rollback Loop
- **Description**: Build an autonomous supervisor loop that detects unrecoverable deployment regressions and triggers automatic rollback.
- **Priority**: `CRITICAL`
- **Estimate**: 8 pts
- **Dependencies**: CS-1101, CS-1201
- **Acceptance Criteria**:
  - Supervisor polls readiness probes during deployment rollout.
  - If 3 consecutive readiness failures occur within 60 seconds, auto-rollback fires.
  - Incident logged to database with root cause and calculated MTTR (< 30s).
- **Tasks**:
  - Task CS-1301.1: Implement `DeploymentSupervisor` scheduled evaluator.
  - Task CS-1301.2: Implement auto-rollback trigger and incident persistence.
  - Task CS-1301.3: Validate end-to-end with simulated failure drill.

---

## EPIC 14: AWS Multi-Cloud Expansion (Phase 13) [FUTURE]

### STORY CS-1401: AWS ECR & EKS Target Support
- **Description**: Implement `AwsDeploymentAdapter` supporting pushing images to AWS ECR and orchestrating deployments on AWS EKS.
- **Priority**: `MINOR`
- **Estimate**: 8 pts
- **Dependencies**: CS-701, CS-801
- **Acceptance Criteria**:
  - Docker images pushable to AWS ECR using AWS credentials.
  - Workloads deployable to AWS EKS using unified manifests.
  - Zero disruption to existing Azure deployment workflow.
- **Tasks**:
  - Task CS-1401.1: Author `AwsDeploymentAdapter` implementing `CloudDeploymentProvider`.
  - Task CS-1401.2: Provision AWS ECR repository and test push pipeline.

---

## EPIC 15: Security Hardening & Vulnerability Remediation (Phase 14)

### STORY CS-1501: Comprehensive Security Audit & Azure Key Vault
- **Description**: Conduct dependency vulnerability audit, container image scanning, Azure Key Vault integration, and network policy verification.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: CS-801
- **Acceptance Criteria**:
  - Zero `CRITICAL` or `HIGH` CVEs in base images and dependencies.
  - Azure Key Vault stores master credentials; injected via Managed Identity.
- **Tasks**:
  - Task CS-1501.1: Run OWASP Dependency-Check / Snyk scan.
  - Task CS-1501.2: Run Trivy container scanner and remediate findings.

---

## EPIC 16: Comprehensive Testing, Documentation & Final Release (Phase 15)

### STORY CS-1601: End-to-End Verification & Demonstration Package
- **Description**: Package full test suite, automated demonstration scripts, and operational runbooks for interview presentation.
- **Priority**: `MAJOR`
- **Estimate**: 5 pts
- **Dependencies**: All preceding Epics
- **Acceptance Criteria**:
  - End-to-end demonstration script executes: Push → Build → Deploy → Fault Inject → Auto-Rollback → Verified.
  - Complete architecture documentation and runbooks finalized.
- **Tasks**:
  - Task CS-1601.1: Write automated demo test script (`scripts/demo-e2e.sh`).
  - Task CS-1601.2: Produce final operational runbook in `docs/runbook.md`.
