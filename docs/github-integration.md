# GitHub Integration Guide

## 1. Overview

CloudShip Phase 2 establishes the source control integration layer, allowing CloudShip projects to connect to GitHub repositories. This enables automated repository verification, branch tracking, and prepares the platform for Phase 3 (Jenkins CI/CD webhook triggers).

---

## 2. Architecture & Data Model

### Database Schema (`git_repositories`)

The `git_repositories` table maintains a 1-to-1 relationship with the `projects` table:

```sql
CREATE TABLE IF NOT EXISTS git_repositories (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL DEFAULT 'GITHUB',
    repository_url VARCHAR(500) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    repository_name VARCHAR(100) NOT NULL,
    default_branch VARCHAR(100) NOT NULL DEFAULT 'main',
    connection_status VARCHAR(50) NOT NULL DEFAULT 'NOT_CONNECTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_git_repository_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_git_provider CHECK (provider IN ('GITHUB', 'GITLAB', 'BITBUCKET')),
    CONSTRAINT chk_git_connection_status CHECK (connection_status IN ('CONNECTED', 'NOT_CONNECTED', 'ERROR'))
);
```

### Entity Model

- **`GitRepository`**: JPA entity mapped to `git_repositories`.
- **`GitProvider`**: Enum (`GITHUB`).
- **`GitConnectionStatus`**: Enum (`CONNECTED`, `NOT_CONNECTED`, `ERROR`).
- Bidirectional association with `Project`: `project.getGitRepository()` / `gitRepo.getProject()`.

---

## 3. Supported Repository URL Formats

CloudShip parses and normalizes standard GitHub repository URLs into owner and repository components:

| Protocol | Format | Example |
|---|---|---|
| **HTTPS** | `https://github.com/{owner}/{repo}` | `https://github.com/octocat/Hello-World` |
| **HTTPS (.git)** | `https://github.com/{owner}/{repo}.git` | `https://github.com/Aashu31/Cloudship.git` |
| **SSH** | `git@github.com:{owner}/{repo}.git` | `git@github.com:octocat/Hello-World.git` |
| **SSH (no .git)** | `git@github.com:{owner}/{repo}` | `git@github.com:Aashu31/Cloudship` |

### URL Parsing & Validation

- Handled by `GitHubService.parseRepositoryUrl(url)`.
- Rejects non-GitHub domains and malformed paths.
- Returns a structured `GitHubRepoInfo(owner, repoName)`.

---

## 4. Verification Logic

`GitHubService.verifyRepository(url, branch)` executes real-time upstream verification:

1. Parses the URL to obtain `owner` and `repoName`.
2. Probes the public GitHub REST API (`https://api.github.com/repos/{owner}/{repo}`).
3. Uses a 5-second connection/read timeout via Spring `RestClient`.
4. Handles HTTP responses:
   - **`200 OK`**: Repository exists and is accessible -> status set to `CONNECTED`.
   - **`404 Not Found`**: Repository does not exist or is private -> status set to `ERROR` with descriptive message.
   - **`403 Forbidden`**: GitHub API rate limit hit -> status set to `CONNECTED` (assumed reachable, avoids blocking on rate limits).
   - **Network Timeout / Connection Failure**: Status set to `ERROR`.

---

## 5. REST API Endpoints

All repository operations are scoped under `/api/projects/{projectId}/repository`:

### 1. Connect Repository
- **Method**: `POST`
- **Path**: `/api/projects/{projectId}/repository`
- **Request Body**:
  ```json
  {
    "repositoryUrl": "https://github.com/octocat/Hello-World",
    "defaultBranch": "master"
  }
  ```
- **Response**: `201 Created`
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
- **Error**: `409 Conflict` if a repository is already connected to this project.

### 2. Get Repository Details
- **Method**: `GET`
- **Path**: `/api/projects/{projectId}/repository`
- **Response**: `200 OK` with `GitRepositoryResponse`.
- **Error**: `404 Not Found` if no repository is connected.

### 3. Update Repository Configuration
- **Method**: `PUT`
- **Path**: `/api/projects/{projectId}/repository`
- **Request Body**: Same as POST.
- **Response**: `200 OK` with updated `GitRepositoryResponse`.

### 4. Disconnect Repository
- **Method**: `DELETE`
- **Path**: `/api/projects/{projectId}/repository`
- **Response**: `204 No Content`.

### 5. Check Live Connection Status
- **Method**: `GET`
- **Path**: `/api/projects/{projectId}/repository/status`
- **Response**: `200 OK`
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

## 6. Frontend UI Integration

The slide-over Project Management drawer includes the **GitHub Integration Card**:
- **Target Project Selector**: Switch between managed projects.
- **Repository URL & Branch**: Configurable fields with inline validation.
- **Connection Status Pill**: Real-time status (`Connected` in green, `Not Connected` in gray, `Error` in red).
- **Last Sync / Verification**: Accurate relative timestamp (`formatTimeAgo`) or honest `"Never synced"`.
- **Action Buttons**: Connect Repository, Verify Connection, Disconnect.
