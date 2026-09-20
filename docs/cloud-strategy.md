# CloudShip — Cloud Strategy Specification (Azure-First)

**Document Version:** 1.0.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Executive Summary & Strategic Direction

CloudShip adopts an **Azure-First** cloud engineering strategy, with an intentional **cloud-agnostic architectural design** to allow seamless future expansion to **Amazon Web Services (AWS)** as a secondary cloud provider.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      Cloud Engineering Strategy                        │
├──────────────────────────┬─────────────────────────────────────────────┤
│ PRIMARY CLOUD (Current)  │ Microsoft Azure (Active Student Account)    │
│ FOUNDATION / LOCAL DEV   │ Docker, Local PostgreSQL, Minikube / kind   │
│ OPTIONAL FRONTEND HOST   │ Vercel (Static Web UI only — not infra)     │
│ FUTURE MULTI-CLOUD       │ Amazon Web Services (AWS)                   │
└──────────────────────────┴─────────────────────────────────────────────┘
```

---

## 2. Primary Cloud: Microsoft Azure

### 2.1 Context & Motivation
The developer currently maintains an active **Azure for Students** subscription with allocated credits. AWS access is currently unavailable. Therefore:
- All cloud infrastructure, continuous delivery targets, container registries, and runtime environments for the MVP and core platform are built on **Microsoft Azure**.
- Under no circumstances is AWS required or assumed for the MVP.

### 2.2 Planned Azure Services & Selection Rationale

| Azure Service | MVP Status | Purpose in CloudShip | Financial & Practical Justification |
|---|---|---|---|
| **Azure Container Registry (ACR)** | **Required (Phase 5)** | Private OCI image registry storing versioned build artifacts | Basic SKU provides low-cost, secure image storage with native Docker CLI compatibility. |
| **Azure Virtual Machines (B-series)** | **Required (Phase 4)** | Hosts Jenkins CI controller and test runners if not run locally | B1s / B2s burstable instances are economical and free/low-cost tier eligible. |
| **Azure Virtual Network (VNet)** | **Required (Phase 4)** | Isolated networking, subnets, Network Security Groups (NSGs) | Included in Azure subscription; fundamental for private workload isolation. |
| **Azure Monitor & Log Analytics** | **Required (Phase 9)** | Ingestion of container logs, platform telemetry, and alert triggers | Free tier allocation (5GB/month ingestion) suffices for development. |
| **Application Insights** | **Optional (Phase 9)** | Deep APM telemetry, HTTP latency tracking, exception profiling | High utility for Spring Boot observability via JVM agent. |
| **Azure Database for PostgreSQL** | **Optional / Staging** | Managed relational store for deployment and incident history | Flexible Server (Burstable B1ms) can be stopped when inactive; local container used for dev. |
| **Azure Key Vault** | **Phase 14 (Hardening)**| Centralized secret, key, and certificate management | Protects service principal credentials and database passwords. |
| **Azure Kubernetes Service (AKS)** | **Evaluated (Phase 6)** | Managed Kubernetes orchestration | Only provisioned when required; local Minikube/kind is utilized first to protect student credits. |

### 2.3 Student Credit FinOps & Preservation Policy
Azure for Students provides a fixed credit allowance ($100 credit pool). To ensure long-term viability:
1. **Local-First Verification**: Code, Docker images, and Kubernetes manifests must be tested locally (via Docker Compose and Minikube/kind) before any cloud provisioning.
2. **De-allocate Inactive Resources**: Azure VMs must be stopped (`deallocated`) when not in use. Stopped VMs do not incur compute charges.
3. **No Unmanaged Public IPs**: Minimize public IP addresses; route management traffic through secure SSH tunneling or temporary bastion hosts.
4. **Automated Resource Tagging**: All resources must be tagged `Environment=Development`, `Project=CloudShip`, and `Owner=Aashu`.

---

## 3. Frontend Hosting Strategy: Vercel vs. Azure

- **Vercel Usage Boundary**:
  - Vercel may be used **ONLY** for hosting the static client frontend dashboard (`frontend/`), leveraging its global edge CDN for rapid UI previewing.
  - Vercel is **NOT** a replacement for cloud infrastructure. It does not run the Spring Boot API, does not host the PostgreSQL database, does not build Docker images, and does not execute deployment automation.
- **Azure Role**:
  - Azure remains the authoritative infrastructure provider hosting backend containers, registries, compute, and networking.

---

## 4. Cloud-Agnostic Architectural Decoupling

To prevent vendor lock-in and enable future AWS integration, the deployment subsystem separates **core orchestration logic** from **cloud provider adapters**:

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
                 │ (ACR / VM / AKS)│ │   (ECR / EKS)   │
                 └────────┬────────┘ └────────┬────────┘
                          │                   │
                          ▼                   ▼
                  [Microsoft Azure]       [FUTURE AWS]
                   (Active Target)       (Phased Scope)
```

- **Phase 0–12**: Implements `AzureDeploymentAdapter` using Azure CLI, ACR, and Kubernetes manifests.
- **Phase 13**: Implements `AwsDeploymentAdapter` targeting AWS ECR, EKS, and VPC without modifying core application business logic.

---

## 5. Phase 0 Rule: Zero Premature Cloud Provisioning

> [!IMPORTANT]
> No Azure resources or subscriptions are provisioned during Phase 0. Phase 0 establishes the engineering blueprint and repository structure. Cloud provisioning begins in **Phase 4 (Azure Infrastructure)** and **Phase 5 (Azure Container Registry)**.
