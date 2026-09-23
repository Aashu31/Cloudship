# Jenkins Continuous Integration (CI) Architecture & Guide

## 1. Overview & Mission

CloudShip Phase 3 establishes the Continuous Integration (CI) foundation, orchestrating automated validation, compilation, automated testing, and Docker image artifact creation upon repository changes or manual invocation.

### Scope & Strict Boundaries

```
Developer Push
      ↓
GitHub Repository
      ↓ (Webhook / Manual Trigger)
CloudShip API / Jenkins CI
      ↓
1. Checkout SCM
      ↓
2. Validate Tools (JDK 17, Docker, Maven)
      ↓
3. Compile Application (Maven)
      ↓
4. Run Test Suite (JUnit 5)
      ↓
5. Build Docker Image (cloudship/backend:<sha>)
      ↓
6. Publish CI Metrics & Results
      ↓
CloudShip Command Center & Pipeline Stepper
```

> [!IMPORTANT]
> **Continuous Integration Boundary**: Phase 3 is strictly responsible for validation, building, testing, and creating the local Docker image. 
> 
> The resulting Docker image is **never pushed** to external registries (such as Azure Container Registry) in Phase 3. 
> 
> Deployment to Kubernetes (AKS), multi-cloud orchestration, canary rollouts, automated rollbacks, and production infrastructure are strictly deferred to Phase 4+.

---

## 2. CI Pipeline Architecture

### Pipeline Stages (`jenkins/Jenkinsfile` & root `Jenkinsfile`)

The pipeline is authored as a declarative Jenkinsfile ensuring repeatable, deterministic execution:

| Stage # | Stage Name | Description | Command / Execution |
|---|---|---|---|
| **1** | **Checkout** | Clones the target branch and commit SHA from GitHub | `checkout scm` |
| **2** | **Validate Environment** | Verifies presence of JDK 17, Maven 3.9+, and Docker runtime | `java -version`, `mvn -v`, `docker -v` |
| **3** | **Compile Backend** | Compiles Java classes without executing test phase | `mvn clean compile -DskipTests` |
| **4** | **Run Tests** | Executes unit and integration test suites with surefire | `mvn test` |
| **5** | **Build Docker Image** | Builds Docker image tagged with commit SHA | `docker build -t cloudship/backend:${GIT_COMMIT_SHORT} ./backend` |
| **6** | **Authenticate to ACR** | Logs in to Azure Container Registry using service principal | `az acr login --name ${ACR_NAME}` / `docker login` |
| **7** | **Tag for Registry** | Tags image with ACR login server and repository prefix | `docker tag ... ${ACR_LOGIN_SERVER}/${IMAGE_PREFIX}...` |
| **8** | **Push to ACR** | Pushes image layers and manifest to ACR | `docker push ${ACR_LOGIN_SERVER}/${IMAGE_PREFIX}...` |
| **9** | **Verify Push** | Extracts image digest (`sha256:...`) and validates push | `docker inspect --format='{{index .RepoDigests 0}}'` |
| **10** | **Publish CI & ACR Results** | Emits build status, push status, digest, and timings to CloudShip API | CloudShip webhook callback / status update |

### Failure Matrix

If any stage fails:
- Stage 3 (Compilation error) ➔ Build marked as `FAILED`, tests and Docker builds skipped.
- Stage 4 (Unit test failure) ➔ Build marked as `FAILED`, test results archived, Docker build skipped.
- Stage 5 (Docker daemon error) ➔ Build marked as `FAILED`, no image artifact created.
- Stage 6 (ACR Auth error) ➔ Build marked as `FAILED` or `SUCCESS` with `pushStatus = FAILED`.
- Stage 8 (ACR Push error) ➔ Build marked with `pushStatus = FAILED` and sanitized error recorded.
- Notifications and state are transmitted to the CloudShip backend with the stage logs excerpt and error description.

---

## 3. Database Schema & Entity Model

### Table: `ci_builds` (`V3__create_ci_builds.sql`)

```sql
CREATE TABLE IF NOT EXISTS ci_builds (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    git_repository_id BIGINT,
    commit_sha VARCHAR(100),
    branch VARCHAR(100) NOT NULL DEFAULT 'main',
    trigger_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    commit_message VARCHAR(500),
    commit_author VARCHAR(100),
    jenkins_build_number INT,
    jenkins_job_name VARCHAR(100),
    docker_image_name VARCHAR(200),
    docker_image_tag VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ci_builds_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_builds_git_repository FOREIGN KEY (git_repository_id) REFERENCES git_repositories(id) ON DELETE SET NULL,
    CONSTRAINT chk_ci_builds_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'ABORTED')),
    CONSTRAINT chk_ci_builds_trigger CHECK (trigger_type IN ('MANUAL', 'WEBHOOK'))
);

CREATE INDEX IF NOT EXISTS idx_ci_builds_project_id ON ci_builds(project_id);
CREATE INDEX IF NOT EXISTS idx_ci_builds_status ON ci_builds(status);
CREATE INDEX IF NOT EXISTS idx_ci_builds_created_at ON ci_builds(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ci_builds_commit_sha ON ci_builds(commit_sha);
```

### Domain Entities

- **`CIBuild`**: Represents an individual execution of the CI pipeline for a `Project`.
- **`CIBuildStatus`**: Enum: `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `ABORTED`.
- **`CITriggerType`**: Enum: `MANUAL`, `WEBHOOK`.

---

## 4. REST API Reference

### 1. Probe Jenkins Service Status
- **Endpoint**: `GET /api/jenkins/status`
- **Response**:
```json
{
  "available": false,
  "connectionStatus": "UNAVAILABLE",
  "baseUrl": "http://localhost:8080",
  "jobName": "cloudship-ci",
  "message": "Jenkins server unreachable (offline fallback active)"
}
```

### 2. List CI Builds for Project
- **Endpoint**: `GET /api/projects/{projectId}/ci-builds`
- **Response**: Array of `CIBuildResponse` ordered by `createdAt DESC`.

### 3. Trigger CI Build (Manual)
- **Endpoint**: `POST /api/projects/{projectId}/ci-builds` or `POST /api/projects/{projectId}/ci/build`
- **Request**:
```json
{
  "branch": "main",
  "commitSha": "a1b2c3d4e5f6...",
  "triggerType": "MANUAL"
}
```
- **Response**: `201 Created` with initialized `CIBuildResponse`.

### 4. Fetch CI Build Details
- **Endpoint**: `GET /api/ci-builds/{id}` or `GET /api/projects/{projectId}/ci-builds/{id}`
- **Response**: `200 OK` with full stages execution breakdown, timing, and log summary.

### 5. Update CI Build Status (Callback / Status Reporter)
- **Endpoint**: `PATCH /api/ci-builds/{id}`
- **Request**:
```json
{
  "status": "SUCCESS",
  "dockerImageTag": "cloudship/backend:a1b2c3d",
  "stagesJson": "[{\"name\":\"Checkout\",\"status\":\"SUCCESS\"},...]",
  "logsSummary": "Maven test suite passed: 50 tests executed, 0 failures.",
  "durationSeconds": 42
}
```

### 6. GitHub Webhook Handler
- **Endpoint**: `POST /api/webhooks/github`
- **Headers**:
  - `X-GitHub-Event`: `push` | `ping`
  - `X-Hub-Signature-256`: HMAC-SHA256 signature (`sha256=<hex_digest>`)
- **Payload**: Standard GitHub Push or Ping event payload (JSON).
- **HMAC-SHA256 Signature Verification**:
  - The signature is calculated as `HmacSHA256(rawRequestBody, JENKINS_WEBHOOK_SECRET)` and compared using constant-time comparison (`MessageDigest.isEqual`) to eliminate timing attacks.
  - **When `JENKINS_WEBHOOK_SECRET` is configured (production/secure mode)**:
    - The `X-Hub-Signature-256` header is **mandatory**.
    - If the header is missing, malformed, or the cryptographic hash does not match, the request is immediately rejected with HTTP `401 Unauthorized` and payload `{"status":"UNAUTHORIZED","message":"Invalid or missing webhook signature"}`.
    - Secrets and signature values are never logged or exposed in API responses.
  - **When `JENKINS_WEBHOOK_SECRET` is unset/empty (local development fallback)**:
    - Webhook verification is bypassed, and a security warning is logged at `WARN` level.
- **Behavior**:
  - `ping` ➔ Responds with HTTP 200 `PONG` acknowledgment.
  - `push` ➔ Matches repository URL to registered project, extracts branch and commit SHA, and automatically enqueues a new `CIBuild` with `triggerType = WEBHOOK`.

---

## 5. Environment Variables & Configuration

The Jenkins CI subsystem is configured via `backend/src/main/resources/application.yml` and overridable via `.env`:

```yaml
cloudship:
  jenkins:
    base-url: ${JENKINS_BASE_URL:http://localhost:8080}
    username: ${JENKINS_USERNAME:}
    api-token: ${JENKINS_API_TOKEN:}
    default-job-name: ${JENKINS_JOB_NAME:cloudship-ci}
    webhook-secret: ${JENKINS_WEBHOOK_SECRET:}
```

### Configuration Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `JENKINS_BASE_URL` | `http://localhost:8080` | URL of the Jenkins controller instance |
| `JENKINS_USERNAME` | *(empty)* | Jenkins user account for API authentication |
| `JENKINS_API_TOKEN` | *(empty)* | Jenkins API token or crumb for authenticated REST calls |
| `JENKINS_JOB_NAME` | `cloudship-ci` | Target Jenkins pipeline job name |
| `JENKINS_WEBHOOK_SECRET` | *(empty)* | Shared secret for verifying GitHub `X-Hub-Signature-256` |

### Webhook Secret Behavior (`JENKINS_WEBHOOK_SECRET`)

- **Configured (Recommended for Production)**: Every inbound webhook request must contain a valid `X-Hub-Signature-256` computed with this secret. Unauthorized requests return HTTP 401.
- **Unset / Empty**: Requests without signatures are accepted with a logged warning to simplify local development environments.

### Local Resilience & Offline Fallback

CloudShip is architected to be completely functional even when Jenkins is not running locally:
1. When `JenkinsClient` cannot establish a socket connection to `localhost:8080`, it gracefully catches connection timeouts.
2. The `CIService` marks the build as processed via deterministic offline simulation, completing the standard compile and test lifecycle so developers can test the end-to-end UX, stepper, and database audit trail without requiring a heavy Jenkins VM on developer laptops.
3. Once Jenkins is started on port 8080 with job `cloudship-ci`, CloudShip automatically transitions to live execution without restarting the backend.

---

## 6. Frontend & Command Center Integration

In conformance with the **Permanent CloudShip Design Constitution** and `/frontend/cloudship-master-reference.png`:

1. **Pipeline Stepper**:
   - Step 1: `Source` (GitHub repository connected)
   - Step 2: `Checkout` (Branch cloned)
   - Step 3: `Validate` (Java 17, Maven, Docker)
   - Step 4: `Build` (Compiled JAR)
   - Step 5: `Test` (JUnit 5 test suite)
   - Step 6: `Docker Build` (Tagged `cloudship/backend:<sha>`)
   - Step 7: `Deploy` (Strictly `Pending (Phase 4+)`)
   - Step 8: `Live` (Strictly `Pending (Phase 4+)`)
2. **Jenkins CI Card**:
   - Displays real-time live Jenkins status pill (`Online` / `Offline (Simulated)`).
   - Shows active pipeline job name (`cloudship-ci`).
   - Shows latest CI build status, duration, and Docker artifact tag.
   - Interactive `▶ Trigger CI Build` and `↻` refresh buttons.
   - Recent CI builds list with status badges and timestamp.
3. **CI Details Modal**:
   - Inspects commit SHA, branch, duration, stages breakdown, and log excerpt without mock data.

---

## 7. Next Steps & Phase 4 Handoff

In Phase 4, the platform will consume artifacts produced by Phase 3:
- Push verified `cloudship/backend:<sha>` images to Azure Container Registry (ACR).
- Deploy containerized services to Kubernetes (AKS).
- Implement rolling update strategies, health probes, and live deployment telemetry.
