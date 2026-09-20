# CloudShip — Git Workflow & Contribution Conventions

**Document Version:** 1.0.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Branching Strategy (GitFlow Adapted for Continuous Delivery)

CloudShip employs an adapted GitFlow branching model that balances rapid trunk progression with rigorous release stability.

```
       hotfix/* ───────────────┐
                               ▼
main    ──●──────────●─────────●──────────● (Production / Protected)
          ▲          │         ▲          ▲
          │          │         │          │ release tags (v1.0.0)
develop ──●────●─────●────●────●────●─────● (Integration / Default)
               ▲          ▲         ▲
               │          │         │
feature/* ─────┴──────────┘         │
bugfix/*  ──────────────────────────┘
```

### 1.1 Core Branches

| Branch | Lifecycle | Protection Level | Purpose |
|---|---|---|---|
| `main` | Permanent | **High**: Requires signed commits, PR approval, and passing CI | Represents stable, release-ready code deployed to production. |
| `develop` | Permanent | **Medium**: Requires passing CI and code review | Main integration branch where active features converge. |
| `feature/*` | Ephemeral | Unprotected (Developer owned) | Dedicated to developing discrete new capabilities. |
| `bugfix/*` | Ephemeral | Unprotected (Developer owned) | Targeted non-critical defect fixes during integration. |
| `hotfix/*` | Ephemeral | Unprotected (Branches off `main`) | Urgent production patches merged back to `main` and `develop`. |

---

## 2. Branch Naming Conventions

All branch names must follow lowercase, hyphen-separated notation prefixed by their purpose:

```text
feature/<jira-key>-<short-description>    e.g., feature/CS-101-health-check-endpoint
bugfix/<jira-key>-<short-description>     e.g., bugfix/CS-142-db-connection-leak
hotfix/<jira-key>-<short-description>     e.g., hotfix/CS-205-revert-broken-manifest
docs/<short-description>                  e.g., docs/update-architecture-spec
infra/<short-description>                 e.g., infra/setup-aws-ecr-repo
```

---

## 3. Commit Message Standards (Conventional Commits)

Commit messages must conform strictly to the **Conventional Commits v1.0.0** specification.

### 3.1 Format
```text
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

### 3.2 Allowed Commit Types

| Type | Intent & Usage |
|---|---|
| `feat` | Introduces a new user-facing or platform feature |
| `fix` | Patches a bug or regression in application or pipeline |
| `docs` | Documentation updates, architecture guides, specifications |
| `infra` | Infrastructure changes (Terraform, AWS, Kubernetes manifests) |
| `ci` | Modifications to CI/CD pipelines (Jenkinsfile, GitHub Actions) |
| `refactor` | Code refactoring without changing functionality or fixing bugs |
| `test` | Adding missing unit/integration tests or refactoring test suites |
| `chore` | Routine build scripts, dependency bumps, or tool configuration |

### 3.3 Commit Examples
- Good: `feat(api): implement deployment trigger endpoint with payload validation`
- Good: `fix(k8s): adjust liveness probe initialDelaySeconds to prevent premature restart`
- Good: `docs(security): document pod security standards and secret rotation policy`
- Bad: `updated stuff`
- Bad: `fixed bug`
- Bad: `commit before lunch`

---

## 4. Pull Request & Code Review Process

1. **Self-Review**:
   - The author must run local unit tests, check linting, and inspect diffs prior to opening a PR.
2. **PR Description Template**:
   - Every PR must document:
     - **Summary of Changes**: What was added or modified.
     - **Related Jira Ticket / Issue**: e.g., `CS-101`.
     - **Testing Evidence**: Local test run output or logs.
     - **Breaking Changes Notice**: Any schema, API, or config incompatibilities.
3. **Review Requirements**:
   - Minimum 1 peer engineering approval required.
   - All automated CI checks (compilation, unit tests, security scans) must be green.
4. **Merge Strategy**:
   - **Squash and Merge** is the default strategy for `feature/*` and `bugfix/*` into `develop` to preserve a clean, linear project history.
   - **Merge Commit** is used when promoting `develop` into `main` to preserve release boundaries.

---

## 5. Release Tagging & Versioning

CloudShip adheres to **Semantic Versioning 2.0.0** (`v<MAJOR>.<MINOR>.<PATCH>`):
- **MAJOR**: Incompatible architectural shifts or breaking API changes.
- **MINOR**: Backward-compatible functionality (e.g., adding a new failure simulator).
- **PATCH**: Backward-compatible bug fixes or security patches.

Tag creation:
```bash
git checkout main
git pull origin main
git tag -a v1.0.0 -m "Release v1.0.0: Initial CloudShip application and deployment pipeline"
git push origin v1.0.0
```
