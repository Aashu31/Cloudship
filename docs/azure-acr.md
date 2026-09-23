# CloudShip — Azure Container Registry (ACR) Integration Guide (Version 5)

## 1. Overview & Mission

CloudShip Version 5 extends the platform from Version 4 Azure Infrastructure Foundation into a secure, production-grade **Azure Container Registry (ACR)** integration layer.

This layer bridges Continuous Integration (Jenkins) with Azure cloud infrastructure, providing:
- Automated Docker image tagging with deterministic Git commit identifiers.
- Secure, credential-isolated authentication and pushing to Azure Container Registry.
- Real-time telemetry tracking of ACR push operations (push status, duration, digest, errors).
- Verification engine inspecting ACR repository catalogs and verifying image digest authenticity.
- First-class visibility on the CloudShip dashboard with honest telemetry and no synthetic data.

### System Architecture

```
                                  CloudShip Platform (Version 5)
                                                │
         ┌──────────────────────────────────────┼──────────────────────────────────────┐
         ▼                                      ▼                                      ▼
Database (PostgreSQL)              Azure ARM SDK Provider                 Jenkins Pipeline Engine
(V5 migration: acr push fields)     (AzureContainerRegistryService)       (Jenkinsfile / CI Callback)
         │                                      │                                      │
         │                                      │ Inspect / Verify                     │ Trigger CI Build
         │                                      ▼                                      ▼
         │                            Azure Container Registry            Docker Engine / Daemon
         │                            (cloudshipcr.azurecr.io)            (Build & ACR Login/Push)
         │                                      ▲                                      │
         │                                      │              Push Image              │
         │                                      └──────────────────────────────────────┘
         ▼
CloudShip Dashboard
(ACR Control Center Drawer + Step 5 "Registry — Pushed to ACR" + CI Build Details Modal)
```

> [!IMPORTANT]
> **Strict Version 5 Boundary Enforcement**:
> - **In Scope**: ACR repository inspection, image tag discovery, deterministic tagging, Jenkins ACR push stages, database push tracking, live digest verification, and dashboard control center.
> - **Strictly Deferred to Version 6+ (Do NOT Implement)**:
>   - Azure Kubernetes Service (AKS) cluster creation or node pool provisioning.
>   - Kubernetes manifests, Helm charts, Pod scheduling, or Ingress controllers.
>   - Canary / Blue-Green / Rolling deployment controllers.
>   - Multi-cloud orchestration (AWS, GCP).

---

## 2. Configuration Reference

### Application Configuration (`backend/src/main/resources/application.yml`)

```yaml
cloudship:
  azure:
    enabled: ${AZURE_ENABLED:false}
    subscription-id: ${AZURE_SUBSCRIPTION_ID:}
    tenant-id: ${AZURE_TENANT_ID:}
    client-id: ${AZURE_CLIENT_ID:}
    client-secret: ${AZURE_CLIENT_SECRET:}
    resource-group: ${AZURE_RESOURCE_GROUP:rg-cloudship-dev}
    location: ${AZURE_LOCATION:eastus}
    vnet-name: ${AZURE_VNET_NAME:vnet-cloudship}
    subnet-name: ${AZURE_SUBNET_NAME:snet-cloudship}
    acr-name: ${AZURE_ACR_NAME:cloudshipcr}
    acr-login-server: ${AZURE_ACR_LOGIN_SERVER:cloudshipcr.azurecr.io}
    acr-repository-prefix: ${AZURE_ACR_REPOSITORY_PREFIX:cloudship/}
```

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `AZURE_ENABLED` | No | `false` | Master toggle for Azure SDK connection |
| `AZURE_SUBSCRIPTION_ID` | When enabled | `""` | Azure Subscription UUID |
| `AZURE_TENANT_ID` | When enabled | `""` | Azure Entra ID / Directory Tenant UUID |
| `AZURE_CLIENT_ID` | When enabled | `""` | Service Principal Application (Client) ID |
| `AZURE_CLIENT_SECRET` | When enabled | `""` | Service Principal Secret Credential |
| `AZURE_RESOURCE_GROUP` | No | `rg-cloudship-dev` | Target Resource Group name |
| `AZURE_LOCATION` | No | `eastus` | Primary Azure region |
| `AZURE_ACR_NAME` | No | `cloudshipcr` | Azure Container Registry instance name |
| `AZURE_ACR_LOGIN_SERVER` | No | `cloudshipcr.azurecr.io` | Fully-qualified ACR login server domain |
| `AZURE_ACR_REPOSITORY_PREFIX` | No | `cloudship/` | Prefix prepended to repository names |

---

## 3. Azure Service Principal & RBAC Setup

To enable automated pushes from Jenkins and inspection from CloudShip, configure an Azure Service Principal with least-privilege role assignments:

### 1. Create Service Principal
```bash
az ad sp create-for-rbac \
  --name "sp-cloudship-ci" \
  --role "Reader" \
  --scopes /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/rg-cloudship-dev
```

### 2. Grant `AcrPush` Role for Jenkins Pushes
```bash
ACR_ID=$(az acr show --name cloudshipcr --resource-group rg-cloudship-dev --query id --output tsv)

az role assignment create \
  --assignee "<SERVICE_PRINCIPAL_CLIENT_ID>" \
  --role "AcrPush" \
  --scope "$ACR_ID"
```

### 3. Grant `AcrPull` / `Reader` Role for CloudShip Inspection
```bash
az role assignment create \
  --assignee "<SERVICE_PRINCIPAL_CLIENT_ID>" \
  --role "Reader" \
  --scope "$ACR_ID"
```

---

## 4. Docker Image Naming & Tagging Convention

All container images pushed to Azure Container Registry follow a deterministic naming and tagging standard validated by `DockerImageValidator`:

### Standard Format
```
<acrLoginServer>/<repositoryPrefix><serviceName>:<gitSha>
```

### Examples
- Primary Commit Tag: `cloudshipcr.azurecr.io/cloudship/backend:a3f9c2d`
- Build Number Tag: `cloudshipcr.azurecr.io/cloudship/backend:build-42`
- Latest Alias Tag: `cloudshipcr.azurecr.io/cloudship/backend:latest`

### Validation Rules
- **Repository Name**: Lowercase alphanumeric characters, dots (`.`), underscores (`_`), and forward slashes (`/`). Must conform to RFC 1123 naming rules. Length: 1–255 characters.
- **Tag**: Alphanumeric characters, underscores (`_`), periods (`.`), and hyphens (`-`). Length: 1–128 characters.
- **Security Check**: Rejects all shell metacharacters (`;`, `&`, `|`, `` ` ``, `$`, `\n`, `\r`, `\0`), spaces, leading hyphens, and credential leaks.

---

## 5. Jenkins CI Pipeline Integration

The pipeline (`Jenkinsfile` and `jenkins/Jenkinsfile`) defines an end-to-end container integration lifecycle:

### Pipeline Stages
1. **Checkout**: Clones the verified project branch.
2. **Validate**: Verifies Maven, JDK 21, and Docker CLI binaries.
3. **Build & Package**: Runs `mvn clean package -DskipTests` to compile the Spring Boot JAR.
4. **Test**: Runs the complete unit and integration test suite (`mvn test`).
5. **Docker Build**: Builds local image `cloudship/backend:<commitSha>`.
6. **Authenticate to ACR**: Performs `az acr login --name cloudshipcr` or `docker login` using masked Jenkins credentials (`AZURE_CLIENT_ID` / `AZURE_CLIENT_SECRET`).
7. **Tag Image**: Tags local container with `<acrLoginServer>/<prefix><service>:<commitSha>` and `latest`.
8. **Push Image**: Executes `docker push` to upload image layers to Azure Container Registry, recording duration and capturing image digest.
9. **Verify Push**: Queries local Docker daemon or ACR manifest inspect to extract `repoDigest` / `sha256:...`.
10. **CloudShip Callback**: Sends final push telemetry to `PUT /api/ci/builds/{id}/status` or `POST /api/ci/callback`:
    ```json
    {
      "status": "SUCCESS",
      "pushStatus": "SUCCESS",
      "pushDurationMs": 22400,
      "imageDigest": "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b553e6363964c10",
      "dockerImageTag": "cloudshipcr.azurecr.io/cloudship/backend:a3f9c2d"
    }
    ```

---

## 6. Database Schema Extension

Database migration `V5__add_acr_registry_fields_to_ci_builds.sql`:

```sql
ALTER TABLE ci_builds
    ADD COLUMN IF NOT EXISTS registry_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS registry_login_server VARCHAR(255),
    ADD COLUMN IF NOT EXISTS push_status VARCHAR(50) DEFAULT 'NOT_ATTEMPTED',
    ADD COLUMN IF NOT EXISTS push_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS push_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS push_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS push_error_message TEXT,
    ADD COLUMN IF NOT EXISTS image_digest VARCHAR(255);

ALTER TABLE ci_builds
    ADD CONSTRAINT chk_ci_builds_push_status
    CHECK (push_status IN ('NOT_ATTEMPTED', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'));

CREATE INDEX IF NOT EXISTS idx_ci_builds_push_status ON ci_builds(push_status);
CREATE INDEX IF NOT EXISTS idx_ci_builds_image_digest ON ci_builds(image_digest);
```

---

## 7. REST API Reference

All endpoints support both `/api/azure/registry` and `/api/infrastructure/azure/registry`.

### 7.1 ACR Connection Status
- **Method**: `GET /api/azure/registry/status`
- **Response**:
```json
{
  "status": "READY",
  "connected": true,
  "registryName": "cloudshipcr",
  "loginServer": "cloudshipcr.azurecr.io",
  "resourceGroup": "rg-cloudship-dev",
  "location": "eastus",
  "adminUserEnabled": false
}
```

### 7.2 ACR Health Probe
- **Method**: `GET /api/azure/registry/health`
- **Response**:
```json
{
  "status": "UP",
  "registry": "cloudshipcr",
  "loginServer": "cloudshipcr.azurecr.io",
  "adminUserEnabled": false
}
```

### 7.3 List Repositories
- **Method**: `GET /api/azure/registry/repositories`
- **Response**:
```json
[
  "cloudship/backend"
]
```

### 7.4 List All Images
- **Method**: `GET /api/azure/registry/images`
- **Response**:
```json
[
  {
    "repository": "cloudship/backend",
    "tag": "a3f9c2d",
    "digest": "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b553e6363964c10",
    "fullImageName": "cloudshipcr.azurecr.io/cloudship/backend:a3f9c2d",
    "loginServer": "cloudshipcr.azurecr.io",
    "lastUpdateTime": "2026-09-22T22:30:00Z"
  }
]
```

### 7.5 Verify Image in ACR
- **Method**: `POST /api/azure/registry/verify`
- **Request Body**:
```json
{
  "repository": "cloudship/backend",
  "tag": "a3f9c2d",
  "digest": "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b553e6363964c10"
}
```
- **Response (`200 OK`)**:
```json
{
  "repository": "cloudship/backend",
  "tag": "a3f9c2d",
  "digest": "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b553e6363964c10",
  "verified": true,
  "status": "FOUND",
  "loginServer": "cloudshipcr.azurecr.io",
  "message": "Image verified in Azure Container Registry",
  "verifiedAt": "2026-09-22T22:35:10Z"
}
```

---

## 8. Dashboard User Experience

1. **ACR Control Center Drawer Card**:
   - Shows Registry Name, Login Server, Resource Group, Region, SKU, Repository Count, Image Count, Last Push Status, and Image Digest.
   - Provides "🔍 Inspect Registry" and "✓ Verify" actions.
2. **Deployment Pipeline Stepper**:
   - Reflects the 8-step pipeline from `cloudship-master-reference.png`.
   - Step 5 is explicitly **Registry — Pushed to ACR**, updating dynamically based on live build push status (`Pushed to ACR`, `Pushing to ACR...`, `ACR push failed`, or `ACR push standby`).
3. **CI Build Details Modal**:
   - Shows independent Build status and ACR Push status.
   - Displays Login Server, Image Digest (monospace), Push Duration, and sanitized error messages.
4. **ACR Inspection Modal**:
   - Lists repositories, artifact images, and provides an interactive verification tool to verify any repository + tag + digest against the live registry.
