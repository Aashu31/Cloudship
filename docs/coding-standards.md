# CloudShip — Engineering Coding Standards & Conventions (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Java & Spring Boot Standards

### 1.1 Architecture & Package Structure
Code must follow a domain-driven layered architecture:
```text
com.cloudship.app/
├── CloudShipApplication.java      # Application entrypoint
├── config/                         # Security, Web, and Bean configuration
├── controller/                     # REST API controllers (HTTP handling & validation)
├── dto/                            # Data Transfer Objects (Request/Response)
├── entity/                         # JPA entities mapping to database tables
├── exception/                      # Global exception handlers & custom exceptions
├── repository/                     # Spring Data JPA repositories
├── service/                        # Core business logic & orchestration interfaces
└── provider/                       # Cloud provider adapters (Azure / future AWS)
```

### 1.2 Naming Conventions
- **Classes**: `PascalCase` (e.g., `DeploymentService`, `HealthCheckController`).
- **Methods & Variables**: `camelCase` (e.g., `triggerRollback()`, `deploymentTimeout`).
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`).
- **Interfaces**: Domain nouns (e.g., `CloudDeploymentProvider`), implementations named `AzureDeploymentAdapter`.

### 1.3 Exception Handling
- Never swallow exceptions silently with empty `catch` blocks.
- Throw strongly typed domain exceptions (e.g., `DeploymentNotFoundException`, `RollbackFailedException`).
- Centralized error handling using `@RestControllerAdvice` returning structured error payloads:
```json
{
  "timestamp": "2026-09-21T01:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Deployment with ID 'dep-1029' does not exist.",
  "path": "/api/deployments/dep-1029"
}
```

### 1.4 Logging Conventions
- Use SLF4J (`log.info("Initiating deployment for image: {}", imageTag);`).
- Never concatenate strings in log statements; use parameter placeholders.
- Redact sensitive keys (`password`, `client_secret`, `token`).

---

## 2. Frontend Standards (HTML / CSS / JavaScript)

### 2.1 File Organization
```text
frontend/
├── index.html                      # Semantic single-page layout
├── css/
│   ├── variables.css               # Design tokens (colors, spacing)
│   └── main.css                    # Responsive layout & component styles
└── js/
    ├── api.js                      # Centralized Fetch API client
    ├── components.js               # Dynamic DOM renderers
    └── app.js                      # Application initialization & state
```

### 2.2 JavaScript Conventions
- Modern ES6+ syntax (`const`, `let`, arrow functions, `async`/`await`).
- Encapsulate code within modules or closures; avoid global namespace pollution.
- Sanitize any dynamic data injected into the DOM to prevent XSS (use `textContent` instead of `innerHTML`).
- Support optional hosting on Vercel as a static edge frontend.

---

## 3. Docker Standards

### 3.1 Dockerfile Best Practices
1. **Multi-Stage Builds**: Separate compile-time dependencies from the runtime layer.
2. **Minimal Base Images**: Use `eclipse-temurin:17-jre-alpine` for minimal attack surface.
3. **Non-Root Execution**: Always declare an explicit non-root user:
   ```dockerfile
   RUN addgroup -g 10001 -S appgroup && adduser -u 10001 -S appuser -G appgroup
   USER 10001:10001
   ```
4. **Mandatory `.dockerignore`**: Exclude `.git`, `.env`, target directories, and logs from build context.
5. **Tagging**: Tag images with commit SHA: `cloudshipcr.azurecr.io/app:v1.0.0-sha.91102b1`.

---

## 4. Kubernetes Standards

### 4.1 Resource Quotas & Probes
Every pod must declare explicit resource limits and health probes:
```yaml
resources:
  requests:
    cpu: "100m"
    memory: "256Mi"
  limits:
    cpu: "500m"
    memory: "512Mi"
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5
```

---

## 5. Shell Scripting Standards (Bash / Linux)

### 5.1 Strict Mode & Error Handling
```bash
#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'
```
- Quote all variables: `rm -rf "${TARGET_DIR:?}"`
- Deterministic exit codes (`0` for success, non-zero for failure).

---

## 6. Microsoft Azure Cloud Standards (Step 0.15)

### 6.1 Azure Resource Naming Conventions
All Azure resources must follow Microsoft Cloud Adoption Framework (CAF) prefixes:
- **Resource Group**: `rg-cloudship-<env>` (e.g., `rg-cloudship-dev`)
- **Virtual Network**: `vnet-cloudship-<env>` (e.g., `vnet-cloudship-dev`)
- **Subnet**: `snet-<workload>-<env>` (e.g., `snet-k8s-dev`, `snet-db-dev`)
- **Network Security Group**: `nsg-<workload>-<env>` (e.g., `nsg-k8s-dev`)
- **Azure Container Registry**: `cloudshipcr` (globally unique lowercase alphanumeric)
- **Virtual Machine**: `vm-cloudship-ci-<env>` (e.g., `vm-cloudship-ci-dev`)
- **PostgreSQL**: `psql-cloudship-<env>` (e.g., `psql-cloudship-dev`)

### 6.2 Mandatory Resource Tagging
Every Azure resource MUST be created with the following tags to ensure FinOps traceability:
```text
Environment = dev | stage | prod
Project     = CloudShip
Owner       = Aashu
ManagedBy   = AzureCLI | Bicep | Jenkins
CostCenter  = StudentSubscription
```

### 6.3 Environment Separation & Least Privilege
- Staging and development workloads reside in dedicated resource groups to facilitate rapid teardown.
- RBAC permissions are scoped to the resource group level, never subscription-wide.
- Service Principals used in CI/CD are granted `AcrPush` only on the specific registry.
