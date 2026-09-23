# CloudShip Version 8 — Observability, Monitoring & Operational Visibility

## 1. Overview & Architectural Philosophy

CloudShip Version 8 establishes a production-grade, three-tier observability and operational monitoring foundation. It replaces synthetic metrics with concrete, verifiable telemetry sourced directly from runtime environments, cloud providers, container registries, Kubernetes clusters, and PostgreSQL audit tables.

### Design Principles:
1. **Three-Tier Separation of Concerns**: Application & Database (Tier 1), Cloud & Workloads (Tier 2), and Pipelines & Operational Audits (Tier 3).
2. **Concrete Truth**: No manufactured numbers, fake "All Systems Nominal" fallbacks, or synthetic success percentages. If a pod is in `CrashLoopBackOff` or `Pending`, it is flagged with its exact status.
3. **Protection Against API Thrashing**: A thread-safe, 15-second in-memory rate-limiting cache protects downstream Azure and Kubernetes APIs from high-frequency dashboard polling. A `?refresh=true` flag allows on-demand cache invalidation.
4. **Decoupled Event Sourcing**: Event-driven auditing records every state transition (`CIBuildStatusChangedEvent`, `DeploymentStatusChangedEvent`, `PipelineStatusChangedEvent`) asynchronously into PostgreSQL without circular dependencies.

---

## 2. Three-Tier Observability Architecture

```
                               ┌─────────────────────────────────────────────────────────────┐
                               │                    CLOUDSHIP CONTROL CENTER                 │
                               │           (Vercel Frontend / Responsive Dark UI)            │
                               └──────────────────────────────┬──────────────────────────────┘
                                                              │ REST / SSE
                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                             CLOUDSHIP OBSERVABILITY ENGINE                                              │
│                                           (/api/monitoring/* Controller & Service)                                      │
├──────────────────────────────────────┬──────────────────────────────────────┬───────────────────────────────────────────┤
│ Tier 1: Application & Database       │ Tier 2: Cloud & Kubernetes           │ Tier 3: Pipelines & Operational Auditing  │
├──────────────────────────────────────┼──────────────────────────────────────┼───────────────────────────────────────────┤
│ • JVM Memory (Used / Max MB)         │ • Azure ARM Authentication & RG      │ • End-to-End Pipeline Execution Stats     │
│ • Application Uptime & PID           │ • Azure Container Registry (ACR)     │ • Real Success Rate % & Avg Duration (s)  │
│ • Thread Count & Garbage Collection  │ • Azure Kubernetes Service (AKS)     │ • Deployment Rollout Progression Metrics  │
│ • PostgreSQL Connection Validation   │ • Workload Replicas (Desired/Ready)  │ • Operational Audit Event Persistence     │
│ • Database Latency Ping (ms)         │ • Pod Phase & CrashLoopBackOff       │ • Immutable Event History (V8 Migration)  │
└──────────────────────────────────────┴──────────────────────────────────────┴───────────────────────────────────────────┘
```

---

## 3. Database Schema (Flyway Migration V8)

Operational events are permanently logged in the PostgreSQL table `monitoring_events`, defined in `V8__create_monitoring_events.sql`:

```sql
CREATE TABLE IF NOT EXISTS monitoring_events (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monitoring_events_created_at ON monitoring_events(created_at DESC);
CREATE INDEX idx_monitoring_events_project_id ON monitoring_events(project_id);
CREATE INDEX idx_monitoring_events_severity ON monitoring_events(severity);
```

### Event Classification:
- **Severities**: `INFO`, `WARN`, `ERROR`
- **Sources**: `PIPELINE`, `KUBERNETES`, `ACR`, `DATABASE`, `SYSTEM`
- **Event Types**:
  - `CI_BUILD_STARTED`, `CI_BUILD_SUCCEEDED`, `CI_BUILD_FAILED`
  - `IMAGE_PUSH_STARTED`, `IMAGE_PUSH_SUCCEEDED`, `IMAGE_PUSH_FAILED`
  - `DEPLOYMENT_STARTED`, `DEPLOYMENT_SUCCEEDED`, `DEPLOYMENT_FAILED`
  - `PIPELINE_STARTED`, `PIPELINE_SUCCEEDED`, `PIPELINE_FAILED`

---

## 4. API Endpoints Reference

All endpoints are hosted under `/api/monitoring`:

| Method | Endpoint | Description | Cache Behavior |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/monitoring/overview` | Complete 3-tier consolidated health snapshot | 15s in-memory cache (`?refresh=true` bypasses) |
| `GET` | `/api/monitoring/application` | JVM memory, thread count, runtime uptime | Real-time JVM probe |
| `GET` | `/api/monitoring/infrastructure` | Azure ARM, ACR registry, AKS cluster power states | Real-time Azure probe |
| `GET` | `/api/monitoring/kubernetes` | Kubernetes workloads, replica status, and pod breakdown | Real-time K8s API query (`?namespace=...`) |
| `GET` | `/api/monitoring/workloads/{deploymentName}` | Deep inspection of a single workload and its pods | Returns 404 if not found |
| `GET` | `/api/monitoring/events` | Operational audit event feed (`?projectId=...`, `?limit=50`) | Real-time PostgreSQL query |
| `GET` | `/api/monitoring/metrics` | Concrete pipeline/deployment success rates, pod totals | Real-time calculation |

---

## 5. Workload & Pod Health Derivation Rules

A workload’s health status is derived using Kubernetes-native condition logic:

```java
if (desiredReplicas == 0) {
    status = "STANDBY";
} else if (readyReplicas >= desiredReplicas && availableReplicas >= desiredReplicas) {
    status = "HEALTHY";
} else if (readyReplicas > 0) {
    status = "DEGRADED";
} else {
    status = "FAILED";
}
```

A pod is classified as ready **only** if:
- `readyContainers == totalContainers` and `phase == "Running"`
- A pod in `Pending`, `Failed`, or `CrashLoopBackOff` is strictly excluded from ready counters.
