# CloudShip — Security Baseline & Governance Plan (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Core Security Tenets & Defense-in-Depth

CloudShip implements a multi-layered **Defense-in-Depth** and **Zero Trust Architecture** focused on Microsoft Azure cloud infrastructure and local development environments.

```text
       ┌─────────────────────────────────────────────────────────┐
       │                  Layer 1: Git & Code                    │
       │     Signed commits, branch protection, zero secrets     │
       ├─────────────────────────────────────────────────────────┤
       │                  Layer 2: CI/CD Pipeline                │
       │       Masked Jenkins credentials, ephemeral runners     │
       ├─────────────────────────────────────────────────────────┤
       │               Layer 3: Container & Registry             │
       │   Distroless/Alpine, non-root USER 10001, ACR Scans     │
       ├─────────────────────────────────────────────────────────┤
       │             Layer 4: Kubernetes Cluster                 │
       │     Restricted PSS, NetworkPolicies, K8s RBAC           │
       ├─────────────────────────────────────────────────────────┤
       │             Layer 5: Microsoft Azure Cloud              │
       │   Azure RBAC, Managed Identity, Key Vault, Private VNets│
       └─────────────────────────────────────────────────────────┘
```

---

## 2. Azure Identity & Access Management (Azure RBAC)

1. **Principle of Least Privilege (PoLP)**:
   - Automated systems are assigned scoped Azure Role-Based Access Control (RBAC) roles confined strictly to the `rg-cloudship-dev` resource group.
   - Broad subscription-level `Owner` or `Contributor` permissions are prohibited for automated pipelines.
2. **Azure Container Registry (ACR) Role Separation**:
   - **CI Pipeline Service Principal**: Assigned the `AcrPush` role only (allows pushing built images; cannot delete repositories or modify IAM).
   - **Kubernetes Cluster Identity**: Assigned the `AcrPull` role only (allows pulling images; cannot push or modify images).
   - **Disable ACR Admin Account**: The legacy shared admin user for ACR is disabled; authentication occurs strictly via Azure Entra ID tokens or Managed Identities.
3. **Azure Managed Identities**:
   - Workloads running in Azure authenticate to Azure Key Vault and other cloud resources using **User-Assigned Managed Identities** or **Workload Identity**, eliminating static secret storage on disk.

---

## 3. Secrets Management & Zero-Hardcoding Policy

1. **Zero Secret Persistence**:
   - Committing database passwords, Azure service principal secrets, API tokens, or SSH private keys into Git history is strictly forbidden.
   - Any secret accidentally committed must be revoked and rotated immediately, followed by Git history sanitization.
2. **Environment Variable Injection**:
   - Applications consume configuration through environment variables.
   - `.env.example` provides non-sensitive template defaults; the active `.env` file is excluded via `.gitignore`.
3. **Azure Key Vault (Target Architecture)**:
   - In cloud environments, production database passwords and API tokens reside in **Azure Key Vault**, synced via Secret Store CSI Driver or injected at container startup.

---

## 4. Container & Image Security

1. **Non-Root Execution**:
   - Application containers must execute under unprivileged user `UID 10001` (`USER 10001:10001`).
2. **Minimal Base Images**:
   - Multi-stage builds utilize verified `eclipse-temurin:17-jre-alpine` images, omitting package managers, compilers, and root shells.
3. **Immutable Image Tagging**:
   - Images pushed to ACR are tagged with both semantic version and Git commit SHA (`cloudship:v1.0.0-sha.91102b1`). Mutable `latest` tags are prohibited in deployment manifests.
4. **Vulnerability Assessment**:
   - Enable Microsoft Defender for Cloud / ACR vulnerability assessment to flag High/Critical CVEs prior to production release.

---

## 5. Kubernetes & Network Security

1. **Pod Security Standards (PSS)**:
   - Deployments enforce the `Restricted` PSS profile: disallow privilege escalation (`allowPrivilegeEscalation: false`), drop all capabilities (`capabilities.drop: ["ALL"]`), and enforce read-only root filesystems where practical.
2. **Network Security Groups (NSGs)**:
   - Azure NSGs enforce a default-deny ingress posture. Only port 80/443 (via Ingress/Load Balancer) and port 22 (restricted to authorized operator IPs) are permitted.
   - Database subnet does not accept any ingress traffic from the public Internet.

---

## 6. Logging Sanitization & Auditability

1. **Log Data Redaction**:
   - Application log formatters strip sensitive keywords (`password`, `token`, `secret`, `authorization`, `client_secret`) via regex masks before emission to stdout.
2. **Immutable Audit Trail**:
   - All deployments, rollbacks, and simulated failure triggers are logged to PostgreSQL with timestamp, initiating user, and client IP address.

---

## 7. Security Implementation Roadmap

| Capability | Phase Introduced | Deliverable |
|---|---|---|
| Zero-Secret `.gitignore` & `.env.example` | **Phase 0 (Active)** | Project Root |
| Spring Security Baseline & Input Validation | **Phase 1** | `backend/` |
| Non-Root Docker & Multi-stage Minimization | **Phase 2** | `docker/Dockerfile` |
| Jenkins Credential Masking | **Phase 3** | `jenkins/Jenkinsfile` |
| Azure RBAC & ACR Role Separation (`AcrPush`/`AcrPull`) | **Phase 5** | Azure Resource Config |
| K8s RBAC, NetworkPolicies & Pod Hardening | **Phase 6** | `k8s/base/security.yaml` |
| Azure Key Vault Integration & Security Audit | **Phase 14** | `docs/security-audit.md` |
