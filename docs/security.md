# CloudShip — Security Baseline & Governance Plan (Azure-First)

**Document Version:** 2.0.0  
**Phase:** Version 9 — Production Security + Cloudflare Zero-Trust + Identity Hardening  
**Status:** Approved

---

## 1. Core Security Tenets & Defense-in-Depth

CloudShip implements a multi-layered **Defense-in-Depth** and **Zero Trust Architecture** focused on Microsoft Azure cloud infrastructure, Cloudflare Zero-Trust Access, and local development environments.

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
       ├─────────────────────────────────────────────────────────┤
       │              Layer 6: Cloudflare Zero-Trust             │
       │   Access Policies, JWT Assertion, JWKS, Origin Protect  │
       └─────────────────────────────────────────────────────────┘
```

### Version 9 Zero-Trust Enhancements

- **Cloudflare Access Production Integration**: All API endpoints protected by Cloudflare Access JWT assertions (RS256, JWKS-verified)
- **JWT Validation Hardening**: Algorithm pinning (RS256), issuer/audience/expiration validation, key rotation via cached JWKS with rate limiting
- **Header Trust Model**: Only cryptographically verified JWT assertions trusted; `CF-Access-Authenticated-User-Email` header explicitly NOT trusted
- **Origin Protection**: Backend validates all requests through Cloudflare Access; direct Render hostname access blocked for protected endpoints
- **CORS Hardening**: Single CORS configuration in Spring Security; no wildcard origins in production; credentials allowed only for trusted origins
- **Authorization Hardening**: All resource endpoints enforce ownership checks (IDOR/BOLA prevention); admin-only operations explicitly guarded
- **User Provisioning**: First user no longer auto-admin in production; dev mode convenience preserved for local development
- **Secure Logout**: Validated Cloudflare logout URL prevents open redirects; fallback to page reload
- **Security Headers**: CSP, HSTS (production), Referrer-Policy, Permissions-Policy, X-Content-Type-Options, X-Frame-Options
- **Audit Logging**: Structured security events (LOGIN_SUCCESS, INVALID_JWT, ACCESS_DENIED, etc.) with correlation IDs; no sensitive data in logs

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
3. **Security Event Audit Log (Version 9)**:
   - Structured audit events: `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `INVALID_JWT`, `INVALID_AUDIENCE`, `INVALID_ISSUER`, `ACCESS_DENIED`, `UNAUTHORIZED_RESOURCE_ACCESS`, `ADMIN_ACTION`, `WEBHOOK_REJECTED`, `SECURITY_CONFIGURATION_ERROR`, `LOGOUT`.
   - Each event includes: timestamp, event type, user identity (sanitized), resource ID, request path, result, correlation ID.
   - No JWTs, passwords, secrets, tokens, or Authorization headers in audit logs.

---

## 7. Cloudflare Zero-Trust Integration (Version 9)

### Authentication Flow

```text
User → Cloudflare Access → JWT Assertion (Cf-Access-Jwt-Assertion) → Backend Validation
                                                    ↓
                              JWKS (https://team.cloudflareaccess.com/cdn-cgi/access/certs)
                                                    ↓
                              RS256 Signature Verification
                                                    ↓
                              Issuer / Audience / Expiration / Key ID Validation
                                                    ↓
                              User Provisioning (email → CloudShip User)
                                                    ↓
                              Spring Security Context Established
```

### Configuration Requirements (Production)

```yaml
cloudship:
  security:
    cloudflare:
      enabled: true
      team-domain: https://your-team.cloudflareaccess.com
      aud: your-cloudflare-application-aud-tag
      logout-url: https://your-team.cloudflareaccess.com/cdn-cgi/access/logout
    headers:
      hsts:
        enabled: true
```

### JWT Validation Requirements

- **Algorithm**: RS256 only (reject HS256, none, etc.)
- **Key ID (kid)**: Required in token header; matched against JWKS
- **Issuer (iss)**: Must match configured Cloudflare team domain
- **Audience (aud)**: Must match configured Cloudflare Application AUD tag
- **Expiration (exp)**: Validated with 30-second clock skew leeway
- **Not Before (nbf)**: Validated if present
- **Email Claim**: Required (`email` or `sub` claim)
- **JWKS Cache**: 24-hour TTL, 10 keys max, rate-limited to 10 requests/minute

---

## 8. Authorization Model (Version 9)

### Resource Ownership Enforcement

All protected resources enforce ownership checks:

| Resource | Owner Field | Access Control |
|---|---|---|
| Project | `Project.owner` (User) | Users see only own projects; Admin sees all |
| Deployment | `Deployment.project.owner` | Users see only own project deployments |
| CI Build | `CIBuild.project.owner` | Users see only own project builds |
| Pipeline | `PipelineExecution.project.owner` | Users see only own project pipelines |
| Repository | `GitRepository.project.owner` | Users see only own project repositories |
| Azure Resources | Project-scoped | Azure operations scoped to project |

### Admin Privileges

- Admin role (`ROLE_ADMIN`) bypasses ownership checks for cross-project management
- First user in **production mode** (Cloudflare enabled) does NOT automatically become admin
- First user in **dev mode** (Cloudflare disabled) becomes admin for local development convenience

---

## 9. Security Implementation Roadmap (Updated)

| Capability | Phase Introduced | Deliverable |
|---|---|---|
| Zero-Secret `.gitignore` & `.env.example` | **Phase 0 (Active)** | Project Root |
| Spring Security Baseline & Input Validation | **Phase 1** | `backend/` |
| Non-Root Docker & Multi-stage Minimization | **Phase 2** | `docker/Dockerfile` |
| Jenkins Credential Masking | **Phase 3** | `jenkins/Jenkinsfile` |
| Azure RBAC & ACR Role Separation (`AcrPush`/`AcrPull`) | **Phase 5** | Azure Resource Config |
| K8s RBAC, NetworkPolicies & Pod Hardening | **Phase 6** | `k8s/base/security.yaml` |
| Azure Key Vault Integration & Security Audit | **Phase 14** | `docs/security-audit.md` |
| **Cloudflare Zero-Trust JWT Validation** | **Version 9** | `backend/src/main/java/com/cloudship/security/` |
| **Security Headers (CSP, HSTS, Referrer-Policy, Permissions-Policy)** | **Version 9** | `SecurityConfig.java`, `vercel.json` |
| **Audit Logging (Structured Security Events)** | **Version 9** | `SecurityAuditLogger.java` |
| **Secure Logout (Validated Cloudflare URL)** | **Version 9** | `AuthController.java`, `api.js` |
| **CORS Consolidation & Hardening** | **Version 9** | `SecurityConfig.java` |
| **IDOR/BOLA Prevention (Ownership Checks)** | **Version 9** | `ProjectService`, `DeploymentService`, etc. |

---

## 10. Incident Response & Failure Modes

### Authentication Failure Modes

| Failure | HTTP Status | Frontend State | Audit Event |
|---|---|---|---|
| Missing JWT | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_JWT` |
| Malformed JWT | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_JWT` |
| Invalid Signature | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_JWT` |
| Wrong Issuer | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_ISSUER` |
| Wrong Audience | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_AUDIENCE` |
| Expired Token | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_JWT` |
| Unknown Key ID | 401 | `AUTHENTICATION_REQUIRED` | `INVALID_JWT` |
| JWKS Unavailable | 401/500 | `API_UNAVAILABLE` | `SECURITY_CONFIGURATION_ERROR` |
| Cloudflare Config Missing | 401/500 | `API_UNAVAILABLE` | `SECURITY_CONFIGURATION_ERROR` |

### Authorization Failure Modes

| Failure | HTTP Status | Frontend State | Audit Event |
|---|---|---|---|
| Resource Not Owned | 403 | `ACCESS_DENIED` | `UNAUTHORIZED_RESOURCE_ACCESS` |
| Admin Required | 403 | `ACCESS_DENIED` | `ACCESS_DENIED` |

---

## 11. Rate Limiting & Abuse Protection

- **Cloudflare Edge**: Recommended for production (DDoS, bot mitigation, rate limiting)
- **Backend**: Stateless JWT validation (no session store to exhaust)
- **Webhook Endpoints**: HMAC-SHA256 validation with constant-time comparison
- **Public Endpoints**: `/api/health`, `/api/version`, `/api/auth/me` (lightweight, no auth required)

---

## 12. Frontend Security UX (Version 9)

The frontend distinguishes between security states:

- **AUTHENTICATION_REQUIRED**: Zero-Trust Gate shown; user must authenticate via Cloudflare
- **ACCESS_DENIED**: Zero-Trust Gate shown with "Access Denied" message; toast notification
- **API_UNAVAILABLE**: Zero-Trust Gate shown with "API Unavailable" message
- **SERVER_ERROR**: Zero-Trust Gate shown with "Server Error" message
- **Authenticated**: Full workspace access; user identity displayed in header

No fake authentication, no fake users, no fake Cloudflare state. Honest "Not Available" states for unreachable services.
