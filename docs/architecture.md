# CloudShip — System Architecture & Technical Design (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  
**Primary Cloud:** Microsoft Azure (Student Subscription)  
**Future Cloud:** Amazon Web Services (AWS Multi-Cloud)  

---

## 1. System Overview & Conceptual Flow

CloudShip is architected as an intelligent DevOps deployment and automated recovery platform. It integrates source control, continuous integration, container packaging, Azure Container Registry (ACR), declarative Kubernetes orchestration, Azure cloud infrastructure, real-time observability, and an automated rollback engine into a unified operational loop.

### 1.1 Conceptual End-to-End Flow

```text
Developer
   │ (git commit & push)
   ▼
GitHub (Repository & Webhook trigger)
   │ (Webhook notification)
   ▼
Jenkins (CI Automation Engine)
   │ (Maven/Gradle compilation & unit testing)
   ▼
Build & Test (Verification Artifacts)
   │ (Docker multi-stage build)
   ▼
Docker (Hardened OCI Container Image)
   │ (docker push with Azure Service Principal / Managed Identity)
   ▼
Azure Container Registry (ACR: cloudshipcr.azurecr.io)
   │ (kubectl apply / rollout update)
   ▼
Kubernetes (Local Minikube/kind or Azure AKS)
   │ (Pod provisioning & service routing)
   ▼
Azure Infrastructure (Resource Group, VNet, Subnets, NSGs, B-series VMs)
   │ (Hosts workloads, Jenkins & PostgreSQL)
   ▼
Application Running (Spring Boot REST Service & Web UI)
   │ (Exposes /actuator/health, telemetry & logs)
   ▼
Monitoring & Observability (Azure Monitor / App Insights / K8s Probes)
   │ (Evaluates health against thresholds)
   ▼
Automated Recovery Engine ──[Failed Health?]──► Triggers Rollback
   │                                               │
   │ (Healthy status & audit)                      ▼
   ▼                                      Kubernetes Rollout Undo
CloudShip Operational Dashboard (Live Web UI)
```

---

## 2. Cloud-Agnostic Design & Provider Abstraction (Step 0.5)

To prevent tight coupling to Azure SDKs and enable effortless future integration with Amazon Web Services (AWS), the deployment subsystem is cleanly decoupled:

```text
               ┌─────────────────────────────────────────┐
               │         CloudShip Core Engine           │
               │   (State Machine, Rollback Logic, SRE)  │
               └────────────────────┬────────────────────┘
                                    │
               ┌────────────────────▼────────────────────┐
               │      Deployment Provider Interface      │
               │   (deploy(), rollback(), getStatus())   │
               └──────────┬───────────────────┬──────────┘
                          │                   │
                          ▼                   ▼
                 ┌─────────────────┐ ┌─────────────────┐
                 │  Azure Adapter  │ │   AWS Adapter   │
                 │   (ACR / AKS)   │ │   (ECR / EKS)   │
                 └────────┬────────┘ └────────┬────────┘
                          │                   │
                          ▼                   ▼
                  [Microsoft Azure]       [FUTURE AWS]
                   (Primary Cloud)      (Phased Scope)
```

### 2.1 Provider Decoupling Rules
- **Core Orchestration**: Handles deployment state transitions (`PENDING`, `DEPLOYING`, `VERIFYING`, `HEALTHY`, `FAILED`, `ROLLED_BACK`), consecutive failure counters, and timeout schedules without importing any cloud vendor SDK.
- **Provider Interface**:
  ```java
  public interface CloudDeploymentProvider {
      DeploymentResult deploy(DeploymentRequest request);
      RollbackResult rollback(String deploymentId);
      WorkloadHealthStatus checkHealth(String deploymentId);
  }
  ```
- **Azure Implementation (`AzureDeploymentAdapter`)**: Implements the interface using Azure CLI / Azure SDK and Kubernetes API against Azure ACR and clusters.
- **AWS Implementation (`AwsDeploymentAdapter`)**: Defined purely as a future placeholder interface to be implemented in **Phase 13**. Zero AWS code is implemented in early phases.

---

## 3. Component Decomposition

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CloudShip Platform                            │
├──────────────────────────────┬──────────────────────────────────────────┤
│ Frontend UI Layer            │ HTML5 / CSS3 / Vanilla JS (Optional Vercel)
│ Application API Core         │ Java 17/21 + Spring Boot 3.x             │
│ Persistence Layer            │ PostgreSQL 15+ (Local Container / Azure) │
│ CI/CD Pipeline Layer         │ Jenkins (Declarative Jenkinsfile)        │
│ Containerization Layer       │ Docker (Multi-stage Eclipse Temurin)     │
│ Registry Layer               │ Azure Container Registry (ACR)           │
│ Orchestration Layer          │ Kubernetes (Minikube / kind / Azure AKS) │
│ Cloud Infrastructure Layer   │ Microsoft Azure (Resource Group, VNet)   │
│ Monitoring & Telemetry Layer │ Azure Monitor, App Insights, K8s Probes  │
│ Automated Recovery Engine    │ Health Evaluation, Rollback & Incident Log│
│ Future Multi-Cloud           │ AWS (ECR, EKS, VPC) — FUTURE SCOPE       │
└──────────────────────────────┴──────────────────────────────────────────┘
```

---

## 4. Detailed Component Specifications

### 4.1 Frontend Layer
- **Responsibility**: Provides the user interface for developers and operators to view real-time deployment status, trigger manual deployments, inspect pipeline logs, execute simulated failure drills, and monitor incident recovery histories.
- **Technology**: HTML5, modern CSS3 (responsive grid/flexbox), Vanilla JavaScript (Fetch API, DOM updates).
- **Hosting Options**:
  - *Option A (Default)*: Served directly from Spring Boot backend static resources (`src/main/resources/static`).
  - *Option B (Optional)*: Hosted on **Vercel** for instant global previewing. (Note: Vercel serves only static client UI assets; it does not replace backend cloud infrastructure).
- **Inputs**: User input actions; REST API responses from Application Core.
- **Outputs**: Rendered DOM, HTTP requests to REST endpoints.
- **Dependencies**: CloudShip Application API Core.
- **Local Role**: Tested directly in browser via `localhost:8080`.
- **Cloud Role**: Deployed alongside backend or routed via reverse proxy / Vercel edge CDN.

### 4.2 Application / API Core
- **Responsibility**: Core orchestration engine. Manages pipeline job metadata, executes deployment state machines, queries Kubernetes API for pod statuses, handles incoming webhooks, and serves client dashboard requests.
- **Technology**: Java 17/21, Spring Boot 3.x, Spring Data JPA, Spring Web.
- **Inputs**: HTTP REST requests from Frontend; GitHub webhook payloads; telemetry data from probes; configuration from environment variables.
- **Outputs**: JSON REST responses; SQL queries to PostgreSQL; CLI/API commands to Kubernetes/Jenkins.
- **Dependencies**: PostgreSQL; Kubernetes API Server; Jenkins API.
- **Local Role**: Runs locally via `mvn spring-boot:run` or local Docker container.
- **Cloud Role**: Runs as containerized workload on Azure VM or Kubernetes.

### 4.3 Database Layer (PostgreSQL)
- **Responsibility**: System of record for deployment history, pipeline execution states, system health snapshots, failure events, and incident audit logs.
- **Technology**: PostgreSQL 15+ (Flyway migrations).
- **Inputs**: SQL DML/DDL queries via JPA / Hibernate with Flyway migrations.
- **Outputs**: Tabular result sets.
- **Dependencies**: Persistent storage volume, network connectivity within private VNet/internal Docker network.
- **Local Role**: Runs via local PostgreSQL service or Docker container (`postgres:15-alpine`).
- **Cloud Role**: Containerized on Azure VM or Azure Database for PostgreSQL Flexible Server (stopped when idle to save student credits).

### 4.4 CI/CD Layer (Jenkins)
- **Responsibility**: Automates code integration, testing, packaging, and artifact generation via declarative pipelines.
- **Technology**: Jenkins LTS running as a containerized controller with ephemeral agent nodes.
- **Inputs**: Webhook triggers or polling triggers from GitHub; Git source code repository.
- **Outputs**: Compilation test reports, verified binary JARs, built Docker images, pipeline stage telemetry.
- **Dependencies**: Git, JDK, Docker daemon access.
- **Local Role**: Runs locally in Docker on `localhost:8080`.
- **Cloud Role**: Runs on an Azure B-series VM (`Standard_B2s`), deallocated when not in use.

### 4.5 Containerization Layer (Docker)
- **Responsibility**: Standardizes packaging of the application and runtime environment into an immutable, portable, reproducible OCI container image.
- **Technology**: Dockerfile with multi-stage builds (`eclipse-temurin:17-jdk` for build, `eclipse-temurin:17-jre-alpine` for production runtime).
- **Inputs**: Compiled application JAR, base image, runtime configuration templates.
- **Outputs**: Tagged OCI-compliant container image (`cloudship-app:tag`).
- **Dependencies**: Docker daemon.
- **Local Role**: Local build & test via `docker build`.
- **Cloud Role**: Built in CI pipeline and pushed to Azure Container Registry.

### 4.6 Container Registry Layer (Azure Container Registry - ACR)
- **Responsibility**: Secure, private artifact storage and distribution for versioned Docker images.
- **Technology**: Azure Container Registry (Basic SKU).
- **Inputs**: Docker images tagged with Git commit SHA and semantic version.
- **Outputs**: Secure image pull access for Kubernetes worker nodes.
- **Dependencies**: Azure Service Principal / Managed Identity authentication.
- **Local Role**: Local Docker cache or ACR via `az acr login`.
- **Cloud Role**: Centralized cloud image repository (`cloudshipcr.azurecr.io`).

### 4.7 Kubernetes Orchestration Layer
- **Responsibility**: Manages container scheduling, scaling, networking, rolling updates, self-healing, and service discovery.
- **Technology**: Kubernetes 1.28+ (Minikube/kind locally; Azure AKS evaluated for Phase 6+).
- **Inputs**: Declarative YAML manifests (`Deployment`, `Service`, `ConfigMap`, `Secret`, `Ingress`).
- **Outputs**: Running pods, load-balanced networking endpoints, health metrics.
- **Dependencies**: Container registry access, underlying compute infrastructure.
- **Local Role**: Minikube or kind cluster for rapid, free local experimentation.
- **Cloud Role**: Azure Kubernetes Service (AKS) or lightweight K3s on Azure VM to minimize student credit burn.

### 4.8 Cloud Infrastructure Layer (Microsoft Azure)
- **Responsibility**: Foundation compute, network, identity, and persistence infrastructure.
- **Technology**: Azure (Resource Group `rg-cloudship-dev`, VNet, Subnets, Network Security Groups, Managed Identities, B-series VMs).
- **Inputs**: Declarative infrastructure definitions (Azure CLI / Bicep / Terraform).
- **Outputs**: Provisioned Azure infrastructure.
- **Dependencies**: Active Azure for Students subscription.
- **Local Role**: Not applicable (simulated via local Linux environment).
- **Cloud Role**: Production & staging cloud environment.

### 4.9 Monitoring & Observability Layer
- **Responsibility**: Continuous real-time assessment of system vitality, application performance metrics, and infrastructure health.
- **Technology**: Spring Boot Actuator (`/health`, `/metrics`), Kubernetes Probes, Azure Monitor & Application Insights.
- **Inputs**: HTTP probe requests, application execution logs, CPU/Memory telemetry.
- **Outputs**: Structured JSON log streams, metrics endpoints, threshold alerts.
- **Dependencies**: Network reachability to probe endpoints.
- **Local Role**: Actuator endpoints inspected via browser/curl.
- **Cloud Role**: Log ingestion into Azure Log Analytics workspace and App Insights dashboard.

### 4.10 Automated Recovery Layer
- **Responsibility**: Evaluates deployment health post-rollout; detects unrecoverable faults (probe failures, high restart count, error spikes); issues automated rollback orders; and generates immutable incident post-mortems.
- **Technology**: CloudShip SRE Supervisor (embedded within Spring Boot application).
- **Inputs**: Probe transition events, Kubernetes rollout status, consecutive failure counter.
- **Outputs**: Rollback command (`kubectl rollout undo`), alert dispatches, database incident records.
- **Dependencies**: Kubernetes API access with deployment update permissions.
- **Local Role**: Tested against local Minikube/kind cluster with simulated failure drills.
- **Cloud Role**: Autonomous supervisor safeguarding Azure workloads.

### 4.11 Future Multi-Cloud Layer: Amazon Web Services (AWS)
- **Status**: **FUTURE EXPANSION (Phase 13)**.
- **Responsibility**: Secondary cloud deployment target validating multi-cloud portability.
- **Components Planned**: AWS ECR, AWS EKS, AWS VPC, AWS CloudWatch.
- **Phase 0 Rule**: Explicitly not required for MVP; zero active dependencies.
