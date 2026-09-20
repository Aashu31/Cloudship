# CloudShip — Product & System Requirements Specification

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  
**Primary Cloud:** Microsoft Azure (Student Subscription)  
**Future Cloud:** Amazon Web Services (AWS Multi-Cloud Expansion)  

---

## 1. Executive Summary & Purpose

### 1.1 Project Purpose
**CloudShip** is an intelligent DevOps deployment and recovery platform designed to demonstrate modern cloud engineering, declarative continuous delivery, active health observability, and automated failure recovery.

Modern software organizations face significant operational overhead when deployments fail silently, degrade under unexpected traffic, or require manual intervention to restore service availability. CloudShip bridges the gap between raw CI/CD automation (Git → Jenkins → Docker → Azure Container Registry → Kubernetes) and intelligent site reliability engineering (SRE) by providing a unified control plane that triggers deployments, validates runtime health, detects regressions, executes automated rollbacks, and logs actionable incident records.

### 1.2 Target Users & Personas
1. **Application Developers**: Require rapid, predictable deployment cycles, immediate build/test feedback, and zero-downtime releases without needing deep Kubernetes or cloud topology expertise.
2. **DevOps / Platform Engineers**: Require auditable, declarative pipeline specifications, standardized container builds, idempotent Kubernetes deployments, and automated canary/rollback safety nets.
3. **Site Reliability Engineers (SREs)**: Require strict adherence to Service Level Objectives (SLOs), automated detection of crash-looping workloads, sub-minute Mean Time to Recovery (MTTR), and persistent incident audit trails.
4. **Engineering Leadership**: Requires visibility into deployment frequency, lead time for changes, change failure rate, and platform operational costs under cloud budget constraints.

### 1.3 Core Problems Solved
- **High Deployment Anxiety**: Manual release verifications and complex kubectl commands during outages lead to operator error.
- **Prolonged Downtime (High MTTR)**: Workloads failing post-deployment (bad configuration, connection exhaustion, image pull errors) often linger undetected until users report outages.
- **Disconnected Toolchain**: Disjointed visibility across Git commits, Jenkins builds, container registries, Kubernetes pods, and cloud telemetry.
- **Lack of Realistic Failure Drills**: Engineering teams rarely test their recovery procedures prior to real production outages. CloudShip incorporates deliberate failure simulation to validate recovery paths.

---

## 2. Requirements Scope: MVP vs. Future Enhancements

| Feature Area | Minimum Viable Product (MVP) — Phases 1 to 8 | Future Enhancements — Phases 9 to 15 |
|---|---|---|
| **Primary Cloud** | **Microsoft Azure** (VNet, VMs, ACR) | **AWS Multi-Cloud** (ECR, EKS, VPC) |
| **VCS Integration** | Webhook / Polling trigger from GitHub repository | Multi-repo orchestration, branch preview environments |
| **CI/CD Pipeline** | Jenkins declarative pipeline: Build, Unit Test, Package | Parallelized matrix builds, static security analysis (SonarQube/Trivy) |
| **Containerization** | Multi-stage Docker build, minimal base image (Eclipse Temurin JRE) | Multi-architecture builds (AMD64 + ARM64) |
| **Registry** | **Azure Container Registry (ACR)** image push & pull | Harbor / Cross-cloud image replication (ACR ↔ ECR) |
| **Orchestration** | Single Kubernetes cluster (Minikube / kind / AKS when justified) | Multi-cluster federation, Service Mesh (Istio) |
| **Dashboard** | Web UI (HTML5/CSS3/JS): Deployments, Status, Logs, Triggers | Advanced analytics, DORA metrics dashboard, RBAC UI |
| **Frontend Hosting** | Served via Spring Boot or optionally **Vercel** (UI only) | Edge delivery optimizations |
| **Health Monitoring** | Spring Boot Actuator `/health`, K8s Liveness & Readiness Probes | **Azure Monitor**, Application Insights, CloudWatch (Future) |
| **Rollback Strategy** | Deterministic rollback to last-known-stable replica set upon failure | Blue/Green routing, Canary analysis with progressive traffic shifts |
| **Failure Recovery** | Pod crash detection, failed health check detection, auto-rollback | AI-driven root cause analysis, self-healing configuration drift |
| **Failure Simulation** | Triggerable test faults (CPU burn, unhealthy probe, DB failure) | Chaos Engineering suites (Chaos Mesh, LitmusChaos) |

> [!IMPORTANT]
> **Cloud Provider Boundary**:
> - The MVP is strictly **Azure-First** and does **NOT** depend on AWS.
> - AWS is reserved exclusively for Phase 13 (Multi-Cloud Expansion).
> - Vercel is scoped strictly to optional static frontend hosting and is not a substitute for cloud infrastructure.

---

## 3. Functional Requirements (FR)

### FR-01: Source Code & Version Control Management
- **FR-01.1**: The platform must interface with GitHub repositories via Git protocols and REST/Webhook APIs.
- **FR-01.2**: Changes merged to designated release branches (e.g., `main`) must initiate automated pipeline runs.
- **FR-01.3**: Every build artifact must be traceable to an immutable Git commit SHA.

### FR-02: Continuous Integration & Build Automation
- **FR-02.1**: The CI engine (Jenkins) must execute builds inside isolated, disposable execution agents.
- **FR-02.2**: The build step must compile the Spring Boot application using Maven/Gradle and execute all automated unit tests.
- **FR-02.3**: Pipeline execution must halt immediately upon any test failure or compilation error with explicit diagnostic logging.

### FR-03: Container Packaging & Registry Storage
- **FR-03.1**: The platform must generate container images utilizing multi-stage Dockerfiles to optimize image footprint.
- **FR-03.2**: Images must be tagged with both semantic release tags and the corresponding Git commit SHA (e.g., `v1.0.0-sha.91102b1`).
- **FR-03.3**: Tagged images must be pushed securely to **Azure Container Registry (ACR)** (or local registry in early development) using Azure Service Principal / Managed Identity credentials.

### FR-04: Deployment Orchestration
- **FR-04.1**: The deployment layer must apply declarative Kubernetes manifests (Deployments, Services, ConfigMaps, Secrets).
- **FR-04.2**: Workloads must use rolling update deployment strategies with configurable `maxUnavailable` and `maxSurge` parameters.
- **FR-04.3**: Deployments must inject runtime configurations via Kubernetes ConfigMaps and sensitive credentials via Kubernetes Secrets.

### FR-05: Runtime Health Checking & Verification
- **FR-05.1**: Applications must expose distinct Liveness (`/actuator/health/liveness`) and Readiness (`/actuator/health/readiness`) endpoints.
- **FR-05.2**: Kubernetes kubelet probes must continually poll these endpoints using defined failure thresholds and initial delay periods.
- **FR-05.3**: CloudShip application backend must actively query workload status via Kubernetes API to maintain a synchronized deployment state.

### FR-06: Automated Rollback
- **FR-06.1**: If a deployment's pods fail readiness checks within a designated deployment timeout window (e.g., 180 seconds), an automated rollback must trigger.
- **FR-06.2**: The rollback mechanism must revert the Kubernetes Deployment to the preceding stable revision (`kubectl rollout undo` equivalent).
- **FR-06.3**: An alert and incident event must be generated immediately upon rollback initiation.

### FR-07: Failure Simulation & Testing
- **FR-07.1**: The application must incorporate secured diagnostic endpoints to simulate operational failures (e.g., toggling readiness state to `DOWN`, simulating high CPU, or throwing unhandled database timeouts).
- **FR-07.2**: Simulators must have built-in self-resetting timers to prevent accidental permanent outages in non-sandboxed environments.

### FR-08: Incident Tracking & Audit Logging
- **FR-08.1**: All deployment attempts, pipeline runs, health status transitions, and rollbacks must be persisted in PostgreSQL.
- **FR-08.2**: Each incident record must document: timestamp, deployment ID, failure type, detection trigger, recovery action taken, and recovery duration (MTTR).

---

## 4. Non-Functional Requirements (NFR)

### 4.1 Performance & Responsiveness
- **NFR-01**: API response times for CloudShip core endpoints must not exceed 250ms under normal load (p95).
- **NFR-02**: Deployment trigger dispatch from GitHub to Jenkins webhook must initiate pipeline execution within 5 seconds.
- **NFR-03**: Automated rollback execution must initiate within 10 seconds of a confirmed failure condition.

### 4.2 Security & Compliance
- **NFR-04 (Zero Secret Persistence)**: No credentials, tokens, Azure secrets, or database passwords may ever be stored in source control.
- **NFR-05 (Least Privilege)**: Jenkins service accounts, Kubernetes ServiceAccounts, and Azure Service Principals must operate strictly under the Principle of Least Privilege.
- **NFR-06 (Container Isolation)**: Application containers must run as unprivileged, non-root users (UID 10001).
- **NFR-07 (Data at Rest & Transit)**: All external communications must use TLS 1.2+; database storage and container registries must use AES-256 encryption at rest.

### 4.3 Reliability & Availability
- **NFR-08**: Application workloads running on Kubernetes must maintain high availability through multi-replica deployments (minimum 2 replicas).
- **NFR-09**: Database connection pooling (HikariCP) must handle transient network disconnects gracefully with automatic reconnect and bounded wait times.
- **NFR-10**: The platform must be resilient to pod crashes without dropping active user HTTP connections.

### 4.4 Scalability & Portability
- **NFR-11 (Cloud-Agnostic Core)**: The core deployment orchestration engine must interface through abstract deployment provider contracts, decoupling platform business logic from Azure-specific SDKs to support AWS seamlessly.
- **NFR-12 (OCI Standard Compliance)**: Container images must be standard OCI compliant, portable across any CNCF-certified Kubernetes cluster (Minikube, AKS, EKS).

### 4.5 Observability & Maintainability
- **NFR-13**: System logs must be output in structured JSON format to stdout/stderr for ingestion by Azure Monitor / Log Analytics.
- **NFR-14**: Core platform metrics (deployment frequency, failure rate, pod restart counts) must be queryable via Prometheus-compatible endpoints.

---

## 5. Acceptance Criteria & Definition of Project Success

The CloudShip project will be considered successful when:
1. A code push to GitHub triggers an end-to-end automated pipeline in Jenkins.
2. The pipeline builds, tests, packages, and pushes a secure Docker container to **Azure Container Registry (ACR)**.
3. The Kubernetes cluster deploys the new image with zero downtime using rolling updates.
4. A deliberate failure injection triggers automated health probe failure detection.
5. The CloudShip platform automatically detects the degradation, executes a rollback to the previous stable release, restores traffic, and writes an incident audit log without human intervention.
6. The entire workflow is achieved under student credit constraints on Azure without requiring AWS for MVP delivery.
