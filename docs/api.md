# CloudShip — REST API Specification

**Document Version:** 1.0.0  
**Phase:** Phase 1 (Application Foundation)  
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

