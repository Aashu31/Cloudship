# CloudShip — AKS & Kubernetes Deployment Foundation

**Document Version:** 1.0.0  
**Phase:** Version 6 — AKS / Kubernetes Deployment Foundation  
**Last Updated:** September 2026  

---

## 1. Overview

Version 6 extends CloudShip to deploy verified container images from Azure Container Registry (ACR) onto Azure Kubernetes Service (AKS) using Kubernetes-native resources.

The deployment chain is:

```
GitHub → Jenkins → Docker Build → ACR Push → ACR Verification
  → AKS → Kubernetes Deployment → Pod Ready → CloudShip Status
```

CloudShip acts as the orchestration layer. It does NOT execute arbitrary `kubectl` commands; all cluster operations go through the official [Kubernetes Java Client (`io.kubernetes:client-java:21.0.0`)](https://github.com/kubernetes-client/java).

---

## 2. Prerequisites

### 2.1 Azure Prerequisites

| Prerequisite | Details |
|---|---|
| Azure Subscription | Active subscription with adequate quota |
| Service Principal | With `Contributor` or `AKS Cluster User Role` |
| AKS Cluster | Provisioned and in `Running` / `Succeeded` state |
| ACR Instance | Already configured (Version 5) |
| ACR–AKS Integration | Attach ACR to AKS (preferred: `az aks update --attach-acr ...`) |

### 2.2 Required Azure RBAC

| Role | Scope | Purpose |
|---|---|---|
| `Azure Kubernetes Service Cluster User Role` | AKS Cluster | Retrieve cluster credentials |
| `AcrPull` | ACR Instance | Allow AKS node pools to pull images |
| `Reader` | Resource Group | Inspect resource group metadata |

> [!IMPORTANT]
> Do NOT grant `Owner` or `Contributor` at subscription scope unless absolutely required for other V4/V5 operations. Use least privilege.

### 2.3 ACR → AKS Image Pull Authentication

The recommended approach is the **Azure-native managed identity attachment**:

```bash
# Attach ACR to AKS (one-time operation — Azure handles token refresh automatically)
az aks update \
  --name aks-cloudship-dev \
  --resource-group rg-cloudship-dev \
  --attach-acr cloudshipcr
```

CloudShip does **not** store ACR passwords, tokens, or `imagePullSecrets` in the database. If the ACR–AKS attachment is missing, deployment will fail clearly with an image pull error from Kubernetes.

---

## 3. Configuration

All AKS/Kubernetes configuration is driven by environment variables. No credentials are committed to source control.

### 3.1 Environment Variables

```env
# Azure Credentials (shared with Version 4/5)
AZURE_ENABLED=true
AZURE_SUBSCRIPTION_ID=<uuid>
AZURE_TENANT_ID=<uuid>
AZURE_CLIENT_ID=<uuid>
AZURE_CLIENT_SECRET=<secret>
AZURE_RESOURCE_GROUP=rg-cloudship-dev

# AKS Cluster
AZURE_AKS_CLUSTER_NAME=aks-cloudship-dev
AZURE_AKS_NODE_RESOURCE_GROUP=         # Optional — auto-detected

# Kubernetes
K8S_NAMESPACE=cloudship
K8S_KUBECONFIG_PATH=                   # Optional — uses Azure credentials if blank
K8S_SERVICE_TYPE=ClusterIP
```

### 3.2 Configuration Priority

1. **`K8S_KUBECONFIG_PATH`** — If set, CloudShip loads credentials from the specified kubeconfig file. Used in local development or CI.
2. **Azure AKS credentials** — If no kubeconfig path is provided and `AZURE_ENABLED=true`, CloudShip retrieves admin/user kubeconfig from the AKS control plane via the Azure Resource Manager API.
3. **Not Configured** — If neither is available, the application remains functional but reports `NOT_CONFIGURED` for all AKS operations.

---

## 4. Kubernetes Client

CloudShip uses the official **Kubernetes Java Client** (`io.kubernetes:client-java:21.0.0`). There is no `kubectl` shell-out. All operations use the typed Java API:

| API Class | Operations Used |
|---|---|
| `AppsV1Api` | `createNamespacedDeployment`, `replaceNamespacedDeployment`, `readNamespacedDeployment`, `listNamespacedDeployment` |
| `CoreV1Api` | `createNamespace`, `readNamespace`, `createNamespacedService`, `replaceNamespacedService`, `listNamespacedPod`, `listNamespacedService` |

### 4.1 Client Initialization (`KubernetesClientProviderImpl`)

The client initializes lazily on first use. States:

| State | Condition |
|---|---|
| `isConfigured() = false` | No kubeconfig path and Azure not configured |
| `isConfigured() = true`, `isConnected() = false` | Configuration present but cluster unreachable |
| `isConnected() = true` | Client initialized and API client ready |

---

## 5. Namespace Management

CloudShip uses a dedicated namespace (default: `cloudship`). The namespace is:

- Validated against Kubernetes naming rules before any operation
- Auto-created if it does not exist (idempotent)
- Never deleted by CloudShip (to protect existing workloads)

Namespace name must match: `^[a-z0-9][a-z0-9\-]{0,62}[a-z0-9]?$`

---

## 6. Deployment Model

### 6.1 Database Fields (Added in V6 Migration)

The `deployments` table is extended with the following columns (Flyway migration `V6__add_kubernetes_fields_to_deployments.sql`):

| Column | Type | Description |
|---|---|---|
| `ci_build_id` | `BIGINT` | FK → ci_builds.id |
| `cluster_name` | `VARCHAR(100)` | AKS cluster name |
| `namespace` | `VARCHAR(100)` | Kubernetes namespace |
| `deployment_name` | `VARCHAR(100)` | Kubernetes Deployment name |
| `service_name` | `VARCHAR(100)` | Kubernetes Service name |
| `image_name` | `VARCHAR(255)` | Full ACR image path |
| `image_tag` | `VARCHAR(100)` | Image tag (e.g. `abc1234`) |
| `image_digest` | `VARCHAR(255)` | `sha256:...` digest if available |
| `replicas` | `INT` | Desired replica count (default: 1) |
| `ready_replicas` | `INT` | Kubernetes ready replica count |
| `updated_replicas` | `INT` | Kubernetes updated replica count |
| `available_replicas` | `INT` | Kubernetes available replica count |
| `rollout_status` | `VARCHAR(50)` | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED` |
| `error_message` | `VARCHAR(2000)` | Last known error |
| `started_at` | `TIMESTAMPTZ` | Deployment start time |

### 6.2 Deployment Status Lifecycle

```
PENDING → RUNNING → SUCCESS
                 ↘ FAILED
```

> [!IMPORTANT]
> `SUCCESS` is **only** set when Kubernetes confirms `readyReplicas >= desiredReplicas && updatedReplicas >= desiredReplicas`. CloudShip never marks a deployment as `SUCCESS` immediately after submitting the API request.

---

## 7. Kubernetes Resources Generated

### 7.1 Deployment Manifest (Programmatic)

CloudShip generates `V1Deployment` resources via the Kubernetes Java client API. Key properties:

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: <project-name>
  namespace: <namespace>
  labels:
    app: <project-name>
    app.kubernetes.io/name: <project-name>
    app.kubernetes.io/managed-by: cloudship
spec:
  replicas: <requested>
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
  selector:
    matchLabels:
      app: <project-name>
  template:
    spec:
      containers:
        - name: <project-name>
          image: <registry>/<repo>@sha256:<digest>   # or :<tag>
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8088
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits:   { cpu: 500m, memory: 512Mi }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8088 }
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8088 }
            initialDelaySeconds: 15
            periodSeconds: 5
```

### 7.2 Service Manifest

A `ClusterIP` service is created alongside each deployment. **No `LoadBalancer`, `Ingress`, or public IP is created in Version 6.**

```
kind: Service
spec:
  type: ClusterIP
  selector:
    app: <project-name>
  ports:
    - port: 8088
      targetPort: 8088
```

### 7.3 Image Reference Priority

1. If `imageDigest` is present: `registry/repo@sha256:<digest>` (immutable, preferred)
2. If only `imageTag` is present: `registry/repo:<tag>`
3. Fallback (should not occur in production): `registry/repo:latest`

---

## 8. Deployment Flow

1. **User** triggers deployment via `POST /api/projects/{projectId}/deployments`
2. **CloudShip** identifies the latest verified CI build with `pushStatus = SUCCESS`
3. **CloudShip** validates: project → image → ACR → AKS → namespace
4. **CloudShip** creates a `Deployment` record with `status = RUNNING`
5. **CloudShip** applies `V1Deployment` and `V1Service` via Kubernetes API
6. **Kubernetes** schedules pods
7. **CloudShip** polls `/api/deployments/{id}/status` to check `readyReplicas`
8. When ready: `status = SUCCESS`; on error or timeout: `status = FAILED`

---

## 9. Rollout Status Polling

Clients should poll `GET /api/deployments/{id}/status` at sensible intervals (e.g., every 5–10 seconds).

- Stop polling when `status` is `SUCCESS` or `FAILED` (terminal states)
- The server checks real Kubernetes rollout state on each status poll for `RUNNING` deployments
- There is **no server-side push / WebSocket** in Version 6

---

## 10. Failure States

| Failure | Kubernetes Cause | CloudShip Status |
|---|---|---|
| Image not found | `ImagePullBackOff` / `ErrImagePull` | `FAILED` |
| Insufficient resources | `Pending` pods, unschedulable | `FAILED` after timeout |
| Crash loop | `CrashLoopBackOff` | `FAILED` |
| Namespace creation error | API 403 / 409 | `FAILED` |
| AKS unreachable | Connection timeout | `FAILED` |
| Azure auth failure | 401 from ARM | `FAILED` |
| Deployment timeout | No ready replicas within limit | `FAILED` |

> [!WARNING]
> Version 6 does **not** perform automatic rollback. If a deployment fails, the previous deployment (if any) remains active in Kubernetes. Manual intervention or a new deployment is required.

---

## 11. Security Model

| Control | Implementation |
|---|---|
| No kubeconfig committed | `.gitignore` blocks `.kube/` and kubeconfig files |
| No cluster token in responses | API responses never include Kubernetes tokens |
| No kubectl shell-out | All operations use the typed Kubernetes Java client |
| No command injection | Namespace and deployment name are validated by regex |
| No arbitrary YAML execution | Resources are built programmatically via the Java client model |
| No user-controlled registry | Image registry is resolved from ACR configuration; arbitrary external registries are rejected |
| Project authorization | Deployment is always tied to an existing project; CIBuild must belong to the same project |
| Sanitized error messages | `DockerImageValidator.sanitizeErrorMessage()` strips credential patterns from error text |

### 11.1 Required Kubernetes RBAC

CloudShip's service account (or the identity used to access the cluster) requires:

```yaml
rules:
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "create", "update", "patch"]
  - apiGroups: [""]
    resources: ["namespaces", "pods", "services"]
    verbs: ["get", "list", "create", "update", "patch"]
```

`cluster-admin` is **not required** and should not be granted.

---

## 12. Local Development

When AKS is not available locally, the application remains fully functional:

- All non-AKS features (projects, CI, ACR) work normally
- AKS/Kubernetes endpoints return honest `NOT_CONFIGURED` or `NOT_CONNECTED` states
- No fake data is displayed in the UI
- To test Kubernetes locally, point `K8S_KUBECONFIG_PATH` to a local kubeconfig (e.g. minikube, kind)

```env
# Local Kubernetes (optional)
K8S_KUBECONFIG_PATH=/home/user/.kube/config
K8S_NAMESPACE=cloudship
```

---

## 13. Runtime Verification Checklist

| Component | Check | Expected |
|---|---|---|
| Azure credentials | `isConfigured()` | `true` if env vars set |
| AKS cluster | `getClusterDetails()` | `status: READY` |
| Kubernetes client | `isConnected()` | `true` |
| Namespace | Auto-created | `cloudship` namespace exists |
| ACR–AKS integration | Pod pulls image | No `ImagePullBackOff` |
| Deployment rollout | `readyReplicas >= replicas` | `status: SUCCESS` |

---

## 14. Troubleshooting

### `NOT_CONFIGURED`
- `AZURE_ENABLED` is `false`, or credentials are missing
- Set all `AZURE_*` env vars and restart

### `NOT_CONNECTED`
- Azure credentials are valid but the AKS cluster is not reachable
- Check cluster power state: `az aks show --name ... --query "powerState"`
- Start the cluster: `az aks start --name ... --resource-group ...`

### `ImagePullBackOff`
- AKS cannot pull from ACR
- Run: `az aks check-acr --name aks-cloudship-dev --resource-group rg-cloudship-dev --acr cloudshipcr`
- If not attached: `az aks update --attach-acr cloudshipcr --name aks-cloudship-dev --resource-group rg-cloudship-dev`

### Deployment stays `RUNNING` forever
- Check pod events: `kubectl describe pod -n cloudship`
- Check liveness/readiness probe paths match your application
- Default probes expect `/actuator/health/liveness` and `/actuator/health/readiness` on port 8088

---

## 15. Version 6 Phase Boundary

**Version 6 STOPS at:**

- AKS cluster discovery ✅
- Kubernetes namespace management ✅
- Kubernetes Deployment (rolling update strategy) ✅
- Kubernetes ClusterIP Service ✅
- ACR image pull via Azure-native integration ✅
- Deployment status and rollout polling ✅
- Pod inspection ✅
- Deployment failure handling (honest, no auto-rollback) ✅

**NOT implemented in Version 6 (future versions):**

- Automatic rollback
- Canary / blue-green deployments
- HPA / VPA autoscaling
- Prometheus / Grafana monitoring
- Application Insights
- AWS / GCP / multi-cloud
- Failure simulation
- AI-based recovery decisions
