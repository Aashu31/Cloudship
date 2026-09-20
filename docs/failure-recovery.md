# CloudShip — Failure & Recovery Model Specification (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Six-Stage Recovery Lifecycle

CloudShip handles system faults through a deterministic, six-stage state machine designed to restore service availability with zero human intervention:

```text
┌───────────┐    ┌───────────┐    ┌───────────┐    ┌───────────┐    ┌──────────────┐    ┌─────────────────┐
│  STAGE 1  │───►│  STAGE 2  │───►│  STAGE 3  │───►│  STAGE 4  │───►│   STAGE 5    │───►│     STAGE 6     │
│  Failure  │    │ Detection │    │ Decision  │    │ Recovery  │    │ Verification │    │ Incident Record │
└───────────┘    └───────────┘    └───────────┘    └───────────┘    └──────────────┘    └─────────────────┘
```

1. **Failure**: A runtime anomaly, defect, or infrastructure breakdown occurs.
2. **Detection**: Automated telemetry (probes, API status, kubelet events) discovers the fault within bounded timeout.
3. **Decision**: SRE recovery engine evaluates policy thresholds (e.g., retry count, error rate, rollout status).
4. **Recovery**: An automated remediation action (pod restart, rollback, failover) is executed.
5. **Verification**: Probes confirm the system has returned to a fully operational `UP` status.
6. **Incident Record**: The event metadata, root cause diagnosis, MTTR, and status are permanently written to PostgreSQL.

---

## 2. Failure Scenarios Matrix (10 Failure Modes)

### Scenario 1: Application Crash (JVM OOM / Unhandled Fatal Error)
- **Failure**: JVM process terminates abnormally or enters an unresponsive deadlock state.
- **Detection**: Kubernetes Liveness Probe (`/actuator/health/liveness`) fails consecutively 3 times (30s).
- **Decision**: Container is unresponsive; restart local container process.
- **Recovery**: Kubelet restarts container inside the pod; traffic remains routed to remaining healthy replica.
- **Verification**: Post-restart Liveness probe returns `200 OK`.
- **Incident Record**: `INC-APP-01`: Container restart event logged with exit code and JVM heap snapshot.

### Scenario 2: Container Crash (Native Segmentation Fault / Missing Library)
- **Failure**: Docker container immediately exits upon launch (`CrashLoopBackOff`).
- **Detection**: Container status reports `CrashLoopBackOff` via Kubernetes Pod status polling.
- **Decision**: New deployment version is defective; abort rollout and roll back.
- **Recovery**: CloudShip issues `kubectl rollout undo deployment/cloudship-app`.
- **Verification**: Previous replica set scaled back to desired count; readiness probes return `200 OK`.
- **Incident Record**: `INC-CTR-02`: Container startup crash logged; rollback completed; MTTR recorded.

### Scenario 3: Pod Failure (Node Eviction / Hardware Node Fault)
- **Failure**: Kubernetes worker node experiences hardware failure or memory eviction.
- **Detection**: Kubernetes control plane marks pod as `Terminating` or `Unknown`.
- **Decision**: Reschedule workload replica onto an alternate healthy cluster node.
- **Recovery**: Kubernetes Deployment controller automatically schedules replacement pod on healthy node.
- **Verification**: New pod passes readiness probes; Service endpoint pool updated.
- **Incident Record**: `INC-POD-03`: Node eviction detected; replacement pod scheduled and verified.

### Scenario 4: Failed Deployment (Defective Application Version Rolled Out)
- **Failure**: New release passes compilation but throws runtime exceptions during initialization.
- **Detection**: Deployment rollout timeout expires (`progressDeadlineSeconds: 180s`) without achieving available replicas.
- **Decision**: Deployment progress stalled; halt rollout and revert to previous stable revision.
- **Recovery**: CloudShip executes automated rollback order; traffic continues serving from undisturbed old replica set.
- **Verification**: Rollback verified via `kubectl rollout status`; active replicas report healthy.
- **Incident Record**: `INC-DEP-04`: Failed rollout aborted; previous stable image restored; zero downtime confirmed.

### Scenario 5: Failed Health Check (Readiness Probe Degradation)
- **Failure**: Application running but downstream dependency or internal buffer saturation degrades readiness.
- **Detection**: Readiness probe `/actuator/health/readiness` returns `503 Service Unavailable`.
- **Decision**: Pod is alive but cannot serve traffic; remove from active load balancer pool immediately.
- **Recovery**: Kubelet removes pod IP from Kubernetes Endpoints; if unrecovered in 60s, pod is restarted or rolled back.
- **Verification**: Readiness probe recovers or replacement pod returns `200 OK`.
- **Incident Record**: `INC-HLT-05`: Traffic diverted from degraded pod; recovered after restart.

### Scenario 6: Database Connection Failure (Transient Network Partition / Connection Exhaustion)
- **Failure**: PostgreSQL network drops or HikariCP pool exhausts all available connections.
- **Detection**: Spring Boot Actuator DB health indicator toggles to `DOWN` with connection timeout error.
- **Decision**: Transient connection error; invoke exponential backoff reconnection before pod restart.
- **Recovery**: HikariCP automatically retries connection; if connection recovers within 30s, no pod restart needed.
- **Verification**: Database health indicator returns `UP`; readiness probe succeeds.
- **Incident Record**: `INC-DBC-06`: Transient database reconnection logged; connection pool stats captured.

### Scenario 7: Image Pull Failure (`ImagePullBackOff` / ACR Auth Expired)
- **Failure**: Kubernetes cannot pull the specified image tag from Azure Container Registry (missing tag, typo, expired Service Principal secret).
- **Detection**: Pod status reports `ErrImagePull` or `ImagePullBackOff`.
- **Decision**: Deployment image unresolvable; halt rollout immediately to prevent terminating existing healthy pods.
- **Recovery**: Cancel deployment update; rollback to last-known good image tag in registry.
- **Verification**: Stable pods maintained; cluster status clean.
- **Incident Record**: `INC-IMG-07`: Image pull failure detected for tag; deployment aborted; operator notified.

### Scenario 8: Configuration Failure (Malformed ConfigMap or Missing Secret)
- **Failure**: Pod fails to start because expected environment variable or secret key is missing.
- **Detection**: Pod container status reports `CreateContainerConfigError`.
- **Decision**: Configuration invalid; cannot proceed with deployment.
- **Recovery**: Abort new deployment; restore configuration bindings of the previous revision.
- **Verification**: Pods with valid configuration achieve `Running` and `Ready` state.
- **Incident Record**: `INC-CFG-08`: Malformed configuration prevented rollout; restored previous config.

### Scenario 9: Azure Resource Failure (Azure Zone / VM Host Degradation)
- **Failure**: An underlying Azure VM host or zone experiences hardware interruption.
- **Detection**: Azure Monitor alerts on host failure; Kubernetes node status transitions to `NotReady`.
- **Decision**: Reschedule workload pods away from degraded Azure VM.
- **Recovery**: Workloads rescheduled to healthy nodes within the VNet subnet; Azure auto-heals underlying VM.
- **Verification**: Pods running and ready on surviving host; traffic restored.
- **Incident Record**: `INC-AZR-09`: Azure host degradation remediated via pod rescheduling.

### Scenario 10: Network Failure (Azure VNet / Subnet Routing Partition)
- **Failure**: Network Security Group misconfiguration or transient Azure VNet routing drop breaks backend-to-database communication.
- **Detection**: Probes and database health checks fail across all pods simultaneously.
- **Decision**: Platform-wide network partition detected.
- **Recovery**: Fail over to cached readiness state, trigger alert notification, and re-apply baseline NSG rules.
- **Verification**: TCP connectivity to port 5432 restored; database health indicator recovers to `UP`.
- **Incident Record**: `INC-NET-10`: Network partition detected and resolved; NSG baseline validated.

---

## 3. Incident Record Data Schema

```sql
CREATE TABLE incidents (
    id                      BIGSERIAL PRIMARY KEY,
    incident_code           VARCHAR(32) NOT NULL,
    failure_type            VARCHAR(64) NOT NULL,
    detection_source        VARCHAR(64) NOT NULL,
    affected_deployment_id  VARCHAR(64),
    target_image_tag        VARCHAR(128),
    recovery_action_taken   VARCHAR(128) NOT NULL,
    recovery_status         VARCHAR(32) NOT NULL,
    mttd_seconds            INTEGER NOT NULL,
    mttr_seconds            INTEGER NOT NULL,
    detected_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    recovered_at            TIMESTAMP WITH TIME ZONE,
    error_summary           TEXT
);
```
