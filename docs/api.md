# CloudShip — REST API Specification

**Document Version:** 1.2.0  
**Phase:** Phase 4 (Azure Infrastructure Foundation)  
**Base URL:** `http://localhost:8088/api`  

---

## 1. Overview & Conventions

All endpoints return and accept JSON payloads with `Content-Type: application/json`.  
Standard HTTP status codes are strictly observed:
- `200 OK`: Successful retrieval or update.
- `201 Created`: Resource successfully created.
- `204 No Content`: Resource successfully deleted.
- `400 Bad Request`: Validation failure or malformed payload.
- `404 Not Found`: Resource does not exist.
- `409 Conflict`: Unique constraint violation (e.g. duplicate project name).
- `500 Internal Server Error`: Unhandled server condition (sanitized, zero secret leakage).

---

## 2. Endpoints Reference

### 2.1 Health Probe

#### `GET /api/health`
Probes application and database availability.

**Response `200 OK`**:
```json
{
  "status": "UP",
  "service": "cloudship",
  "database": "CONNECTED"
}
```

---

### 2.2 Projects API

#### `GET /api/projects`
Retrieves all registered CloudShip projects.

**Response `200 OK`**:
```json
[
  {
    "id": 1,
    "name": "payment-gateway",
    "description": "Payment microservice",
    "repositoryUrl": "https://github.com/org/payment-service",
    "deploymentsCount": 0,
    "createdAt": "2026-09-21T03:30:00Z",
    "updatedAt": "2026-09-21T03:30:00Z"
  }
]
```

#### `GET /api/projects/{id}`
Retrieves a single project by its primary key ID.

**Response `200 OK`**:
```json
{
  "id": 1,
  "name": "payment-gateway",
  "description": "Payment microservice",
  "repositoryUrl": "https://github.com/org/payment-service",
  "deploymentsCount": 0,
  "createdAt": "2026-09-21T03:30:00Z",
  "updatedAt": "2026-09-21T03:30:00Z"
}
```

**Response `404 Not Found`**:
```json
{
  "timestamp": "2026-09-21T03:31:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Project with ID '99' was not found",
  "path": "/api/projects/99"
}
```

#### `POST /api/projects`
Creates a new project.

**Request Body**:
```json
{
  "name": "order-service",
  "description": "Order processing worker",
  "repositoryUrl": "https://github.com/org/order-service"
}
```

**Validation Rules**:
- `name`: Required, 2–100 characters, unique.
- `description`: Optional, max 500 characters.
- `repositoryUrl`: Required, must be a valid HTTP/HTTPS URL or Git SSH address.

**Response `201 Created`**:
```json
{
  "id": 2,
  "name": "order-service",
  "description": "Order processing worker",
  "repositoryUrl": "https://github.com/org/order-service",
  "deploymentsCount": 0,
  "createdAt": "2026-09-21T03:32:00Z",
  "updatedAt": "2026-09-21T03:32:00Z"
}
```

**Response `400 Bad Request` (Validation Failure)**:
```json
{
  "timestamp": "2026-09-21T03:32:05Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/projects",
  "details": [
    "name: Project name is required",
    "repositoryUrl: Repository URL must be a valid HTTP/HTTPS or Git SSH address"
  ]
}
```

#### `DELETE /api/projects/{id}`
Deletes a project and cascades deletion to associated deployments.

**Response `204 No Content`** (empty body).

---

### 2.3 Deployments API

#### `GET /api/projects/{projectId}/deployments`
Retrieves deployment history for a specific project ordered by `createdAt` descending.

**Response `200 OK`**:
```json
[
  {
    "id": 101,
    "projectId": 1,
    "projectName": "payment-gateway",
    "version": "v1.0.0",
    "status": "SUCCESS",
    "createdAt": "2026-09-21T03:35:00Z",
    "completedAt": "2026-09-21T03:36:12Z"
  }
]
```
*(If no deployments exist, returns empty list `[]`)*.

#### `GET /api/deployments/{id}`
Retrieves a single deployment by its primary key ID.

**Response `200 OK`**:
```json
{
  "id": 101,
  "projectId": 1,
  "projectName": "payment-gateway",
  "version": "v1.0.0",
  "status": "SUCCESS",
  "createdAt": "2026-09-21T03:35:00Z",
  "completedAt": "2026-09-21T03:36:12Z"
}
```

#### `GET /api/deployments`
Retrieves recent deployments across all projects.

**Response `200 OK`**: List of deployments.

---

### 2.4 Users API

#### `GET /api/users`
Retrieves all registered users.

**Response `200 OK`**:
```json
[]
```
*(Authentication will be introduced in future phases; endpoints currently provide foundation)*.

---

### 2.5 GitHub Repositories API (Phase 2)

#### `GET /api/projects/{projectId}/repository`
Retrieves connected Git repository details for a project.

**Response `200 OK`**:
```json
{
  "id": 1,
  "projectId": 7,
  "provider": "GITHUB",
  "repositoryUrl": "https://github.com/octocat/Hello-World",
  "owner": "octocat",
  "repositoryName": "Hello-World",
  "defaultBranch": "master",
  "connectionStatus": "CONNECTED",
  "createdAt": "2026-09-21T04:44:46.562Z",
  "updatedAt": "2026-09-21T04:44:46.562Z"
}
```

#### `POST /api/projects/{projectId}/repository`
Connects a Git repository to a project. Performs automated upstream verification against GitHub REST API.

**Request Body**:
```json
{
  "repositoryUrl": "https://github.com/octocat/Hello-World",
  "defaultBranch": "master"
}
```

**Response `201 Created`**: Returns created `GitRepositoryResponse`.  
**Error `409 Conflict`**: If a repository is already connected to this project.

#### `PUT /api/projects/{projectId}/repository`
Updates the Git repository configuration for a project.

**Response `200 OK`**: Returns updated `GitRepositoryResponse`.

#### `DELETE /api/projects/{projectId}/repository`
Disconnects and deletes the Git repository configuration for a project.

**Response `204 No Content`**.

#### `GET /api/projects/{projectId}/repository/status`
Performs an active live check against the public GitHub API and updates the local repository status.

**Response `200 OK`**:
```json
{
  "projectId": 7,
  "repositoryUrl": "https://github.com/octocat/Hello-World",
  "owner": "octocat",
  "repositoryName": "Hello-World",
  "defaultBranch": "master",
  "connectionStatus": "CONNECTED",
  "message": "Repository verified successfully via GitHub API",
  "lastVerifiedAt": "2026-09-21T04:44:54.009Z"
}
```

---

### 2.6 Build & Container Info API (Phase 2)

#### `GET /api/build-info`
Retrieves backend build and Docker container runtime metadata.

**Response `200 OK`**:
```json
{
  "application": "cloudship-backend",
  "version": "1.0.0",
  "environment": "Local Dev",
  "dockerImage": "cloudship/backend:1.0.0",
  "javaVersion": "17.0.20.1",
  "containerStatus": "READY",
  "timestamp": "2026-09-21T04:44:10.779Z"
}
```

---

### 2.7 Continuous Integration (CI) API (Phase 3)

#### `GET /api/jenkins/status`
Checks connectivity and job availability for the Jenkins CI server.

**Response `200 OK`**:
```json
{
  "available": false,
  "connectionStatus": "NOT_CONNECTED",
  "baseUrl": "http://localhost:8080",
  "jobName": "cloudship-ci",
  "message": "Jenkins server unreachable"
}
```

#### `GET /api/projects/{projectId}/ci-builds` (or `/api/projects/{projectId}/ci/builds`)
Lists CI builds for a project ordered by creation date descending. Automatically reconciles `RUNNING` builds with Jenkins API if alive.

**Response `200 OK`**:
```json
[
  {
    "id": 1,
    "projectId": 1,
    "gitRepositoryId": 1,
    "commitSha": "3645398",
    "branch": "main",
    "triggerType": "MANUAL",
    "status": "SUCCESS",
    "dockerImageName": "cloudship/backend",
    "dockerImageTag": "3645398",
    "jenkinsBuildNumber": 12,
    "jenkinsJobName": "cloudship-ci",
    "durationMs": 45000,
    "startedAt": "2026-09-22T20:00:00Z",
    "completedAt": "2026-09-22T20:00:45Z",
    "createdAt": "2026-09-22T19:59:58Z"
  }
]
```

#### `POST /api/projects/{projectId}/ci-builds` (or `/api/projects/{projectId}/ci/build`)
Queues a new CI build. If `commitSha` is omitted, it is left null until resolved by SCM or Jenkins callback.

**Request Body**:
```json
{
  "branch": "main",
  "commitSha": "3645398",
  "triggerType": "MANUAL"
}
```

**Response `201 Created`**: Returns initialized `CIBuildResponse` with status `QUEUED`.

#### `GET /api/ci-builds/{id}` (or `/api/projects/{projectId}/ci-builds/{id}`)
Fetches single build details with live status reconciliation against Jenkins.

#### `PATCH /api/ci-builds/{id}/status`
Callback endpoint invoked by CI runners / Jenkins pipeline `post` blocks to report final status, duration, error messages, and resolved commit SHA.
Protected by `X-CloudShip-CI-Token` verification when webhook secret is configured.

**Request Body**:
```json
{
  "status": "SUCCESS",
  "commitSha": "3645398",
  "jenkinsBuildNumber": 12,
  "dockerImageTag": "3645398",
  "errorMessage": null
}
```

#### `POST /api/webhooks/github`
Handles GitHub push events to automatically queue a CI build when the branch matches the configured project repository.

---

### 2.8 Azure Infrastructure Foundation API (Phase 4)

Base paths supported: `/api/azure/*` and `/api/infrastructure/azure/*`.

#### `GET /api/azure/status`
Retrieves Azure Resource Manager connectivity and authentication status.

**Response `200 OK`**:
```json
{
  "configured": true,
  "connectionStatus": "CONNECTED",
  "subscriptionId": "00000000-0000-0000-0000-000000000000",
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "resourceGroup": "rg-cloudship-dev",
  "location": "eastus",
  "message": "Azure connection established and verified successfully",
  "lastCheckedAt": "2026-09-22T22:00:00Z"
}
```

#### `GET /api/azure/resource-group` (alias: `/api/azure/resources`)
Inspects target Azure Resource Group existence, provisioning state, region, and tags.

**Response `200 OK`**:
```json
{
  "name": "rg-cloudship-dev",
  "location": "eastus",
  "provisioningState": "Succeeded",
  "status": "READY",
  "message": "Resource Group verified successfully",
  "tags": { "Environment": "dev" }
}
```

#### `GET /api/azure/network`
Inspects Azure Virtual Network and Subnet topology, address prefixes, and status.

**Response `200 OK`**:
```json
{
  "vnetName": "vnet-cloudship",
  "subnetName": "snet-cloudship",
  "location": "eastus",
  "provisioningState": "Succeeded",
  "addressSpaces": ["10.0.0.0/16"],
  "subnetAddressPrefixes": ["10.0.1.0/24"],
  "status": "READY",
  "subnetStatus": "READY",
  "message": "Virtual network and subnet verified successfully"
}
```

#### `GET /api/azure/registry`
Inspects Azure Container Registry (ACR) name, login server, SKU, and admin user status.

**Response `200 OK`**:
```json
{
  "name": "cloudshipcr",
  "loginServer": "cloudshipcr.azurecr.io",
  "location": "eastus",
  "sku": "Standard",
  "adminUserEnabled": false,
  "provisioningState": "Succeeded",
  "status": "READY",
  "message": "Azure Container Registry verified successfully"
}
```

#### `GET /api/azure/infrastructure` (alias: `GET /api/azure`)
Returns aggregated infrastructure overview comprising status, resource group, virtual network, and container registry.

#### `GET /api/azure/health`
Health probe for container orchestrators and platform monitoring.

---

### 2.8 Azure Container Registry (ACR) API (Version 5)

All ACR endpoints are accessible under both `/api/azure/registry` and `/api/infrastructure/azure/registry`.

#### `GET /api/azure/registry/status`
Probes ACR configuration and SDK connectivity.

**Response `200 OK`**:
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

#### `GET /api/azure/registry/health`
Health check endpoint reporting ACR connectivity.

**Response `200 OK`**:
```json
{
  "status": "UP",
  "registry": "cloudshipcr",
  "loginServer": "cloudshipcr.azurecr.io",
  "adminUserEnabled": false
}
```

#### `GET /api/azure/registry/repositories`
Lists all container repositories registered in Azure Container Registry.

**Response `200 OK`**:
```json
[
  "cloudship/backend"
]
```

#### `GET /api/azure/registry/images`
Lists all images across all repositories in ACR with tags and digests.

**Response `200 OK`**:
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

#### `GET /api/azure/registry/images/{repository}`
Lists all image tags and digests for a specific repository.

#### `GET /api/azure/registry/images/{repository}/{tag}`
Retrieves detailed metadata for a specific image repository and tag.

#### `POST /api/azure/registry/verify`
Verifies whether a specific image repository, tag, and optional digest exists in the Azure Container Registry.

**Request Body**:
```json
{
  "repository": "cloudship/backend",
  "tag": "a3f9c2d",
  "digest": "sha256:4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b553e6363964c10"
}
```

**Response `200 OK`**:
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



