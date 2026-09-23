package com.cloudship.service;

import com.cloudship.dto.azure.*;
import com.cloudship.dto.monitoring.*;
import com.cloudship.entity.CIBuild;
import com.cloudship.entity.MonitoringEvent;
import com.cloudship.entity.PipelineExecution;
import com.cloudship.entity.Project;
import com.cloudship.event.CIBuildStatusChangedEvent;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.MonitoringEventRepository;
import com.cloudship.repository.PipelineExecutionRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.azure.AzureAksService;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.AzureInfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CloudShip Version 8 — Core Observability & Operational Monitoring Engine.
 * Aggregates Application & Database (Tier 1), Cloud & Workload (Tier 2), and CI/CD (Tier 3) health.
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final long CACHE_TTL_MS = 15_000; // 15s in-memory rate-limiting cache

    private final DataSource dataSource;
    private final AzureInfrastructureService azureService;
    private final AzureContainerRegistryService acrService;
    private final AzureAksService aksService;
    private final PipelineExecutionRepository pipelineExecutionRepository;
    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final MonitoringEventRepository monitoringEventRepository;

    private final Instant applicationStartTime = Instant.now();
    private final AtomicReference<CachedOverview> cachedOverview = new AtomicReference<>();

    private record CachedOverview(MonitoringOverviewResponse response, Instant cachedAt) {}

    public MonitoringService(DataSource dataSource,
                             AzureInfrastructureService azureService,
                             AzureContainerRegistryService acrService,
                             AzureAksService aksService,
                             PipelineExecutionRepository pipelineExecutionRepository,
                             DeploymentRepository deploymentRepository,
                             ProjectRepository projectRepository,
                             MonitoringEventRepository monitoringEventRepository) {
        this.dataSource = dataSource;
        this.azureService = azureService;
        this.acrService = acrService;
        this.aksService = aksService;
        this.pipelineExecutionRepository = pipelineExecutionRepository;
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
        this.monitoringEventRepository = monitoringEventRepository;
    }

    /**
     * Retrieves the consolidated 3-tier monitoring overview snapshot.
     */
    public MonitoringOverviewResponse getMonitoringOverview() {
        return getMonitoringOverview(false);
    }

    public MonitoringOverviewResponse getMonitoringOverview(boolean forceRefresh) {
        Instant now = Instant.now();
        CachedOverview current = cachedOverview.get();

        if (!forceRefresh && current != null && Duration.between(current.cachedAt(), now).toMillis() < CACHE_TTL_MS) {
            log.debug("Serving monitoring overview from 15s cache");
            return current.response();
        }

        long checkStartMs = System.currentTimeMillis();
        MonitoringOverviewResponse overview = new MonitoringOverviewResponse();

        // 1. Tier 1: Application & Database Health
        overview.setApplication(getApplicationHealth());
        overview.setDatabase(getDatabaseHealth());

        // 2. Tier 2: Cloud Infrastructure & Workloads
        try {
            overview.setAzure(azureService.getAzureStatus());
        } catch (Exception e) {
            log.warn("Azure status query failed: {}", e.getMessage());
            overview.setAzure(new AzureStatusResponse(false, AzureConnectionStatus.NOT_CONFIGURED, null, null, null, null, "Query failed: " + e.getMessage(), OffsetDateTime.now()));
        }

        try {
            overview.setAcr(acrService.getRegistryDetails());
        } catch (Exception e) {
            log.warn("ACR status query failed: {}", e.getMessage());
            overview.setAcr(new AzureRegistryResponse("cloudshipcr", "cloudshipcr.azurecr.io", "eastus", "Standard", false, "Failed", AzureResourceStatus.ERROR, e.getMessage()));
        }

        try {
            overview.setAks(aksService.getClusterDetails());
        } catch (Exception e) {
            log.warn("AKS status query failed: {}", e.getMessage());
            overview.setAks(AksClusterResponse.error("aks-cloudship-dev", e.getMessage()));
        }

        overview.setKubernetes(getKubernetesWorkloadsSummary());

        // 3. Tier 3: Pipelines & Deployments
        overview.setPipelines(getPipelineSummary());

        // 4. Recent Events (up to 10 latest)
        overview.setRecentEvents(getRecentEvents(null, 10));

        // 5. Self-Health
        long checkDuration = System.currentTimeMillis() - checkStartMs;
        overview.setSelfHealth(Map.of(
                "status", "HEALTHY",
                "lastCheckedAt", OffsetDateTime.now().toString(),
                "checkDurationMs", checkDuration,
                "cacheTtlMs", CACHE_TTL_MS
        ));

        cachedOverview.set(new CachedOverview(overview, now));
        return overview;
    }

    /**
     * Tier 1: Application Health & JVM Metrics.
     */
    public Map<String, Object> getApplicationHealth() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedMb = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMb = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long uptimeSeconds = Duration.between(applicationStartTime, Instant.now()).getSeconds();

        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("uptimeSeconds", uptimeSeconds);
        map.put("jvmMemoryUsedMb", usedMb);
        map.put("jvmMemoryMaxMb", maxMb);
        map.put("startedAt", applicationStartTime.toString());
        return map;
    }

    /**
     * Tier 1: Database Health via explicit valid connection ping.
     */
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> map = new HashMap<>();
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            long latency = System.currentTimeMillis() - start;
            map.put("status", valid ? "CONNECTED" : "DISCONNECTED");
            map.put("latencyMs", latency);
            map.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            map.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            log.warn("Database health probe check failed: {}", e.getMessage());
            map.put("status", "DISCONNECTED");
            map.put("latencyMs", -1);
            map.put("error", e.getMessage());
        }
        return map;
    }

    /**
     * Tier 2: Summary of Kubernetes workloads & pods.
     */
    public Map<String, Object> getKubernetesWorkloadsSummary() {
        Map<String, Object> summary = new HashMap<>();
        try {
            List<KubernetesWorkloadResponse> workloads = aksService.listWorkloads(null);
            List<KubernetesPodResponse> pods = aksService.listPods(null, null);

            int totalWorkloads = workloads.size();
            int healthyWorkloads = 0;
            int degradedWorkloads = 0;

            for (KubernetesWorkloadResponse w : workloads) {
                if (w.getReadyReplicas() >= w.getDesiredReplicas() && w.getDesiredReplicas() > 0) {
                    healthyWorkloads++;
                } else if (w.getReadyReplicas() > 0) {
                    degradedWorkloads++;
                }
            }

            int totalPods = pods.size();
            int runningPods = 0;
            int pendingPods = 0;
            int failedPods = 0;
            int totalRestarts = 0;

            for (KubernetesPodResponse p : pods) {
                if (isPodReady(p)) {
                    runningPods++;
                } else if ("Pending".equalsIgnoreCase(p.getPhase())) {
                    pendingPods++;
                } else if (isPodFailed(p)) {
                    failedPods++;
                }
                if (p.getRestartCount() != null) {
                    totalRestarts += p.getRestartCount();
                }
            }

            summary.put("status", "CONNECTED");
            summary.put("totalWorkloads", totalWorkloads);
            summary.put("healthyWorkloads", healthyWorkloads);
            summary.put("degradedWorkloads", degradedWorkloads);
            summary.put("totalPods", totalPods);
            summary.put("runningPods", runningPods);
            summary.put("pendingPods", pendingPods);
            summary.put("failedPods", failedPods);
            summary.put("totalRestarts", totalRestarts);
        } catch (Exception e) {
            log.warn("Kubernetes workloads summary check failed: {}", e.getMessage());
            summary.put("status", "NOT_CONNECTED");
            summary.put("totalWorkloads", 0);
            summary.put("healthyWorkloads", 0);
            summary.put("degradedWorkloads", 0);
            summary.put("totalPods", 0);
            summary.put("runningPods", 0);
            summary.put("pendingPods", 0);
            summary.put("failedPods", 0);
            summary.put("totalRestarts", 0);
            summary.put("error", e.getMessage());
        }
        return summary;
    }

    /**
     * Tier 2: Deep inspection of a specific workload or all workloads in namespace.
     */
    public List<WorkloadHealthResponse> getWorkloadHealthList(String namespace) {
        List<WorkloadHealthResponse> results = new ArrayList<>();
        try {
            List<KubernetesWorkloadResponse> workloads = aksService.listWorkloads(namespace);
            for (KubernetesWorkloadResponse w : workloads) {
                List<KubernetesPodResponse> pods = aksService.listPods(w.getNamespace(), w.getName());
                List<PodHealthSummary> podSummaries = new ArrayList<>();

                int readyCount = 0;
                for (KubernetesPodResponse p : pods) {
                    boolean isReady = isPodReady(p);
                    if (isReady) readyCount++;

                    String state = p.getStatusMessage();
                    if (state == null || state.isBlank()) {
                        state = p.getPhase();
                    }

                    podSummaries.add(new PodHealthSummary(
                            p.getName(),
                            p.getNamespace(),
                            p.getPhase(),
                            isReady,
                            p.getRestartCount() != null ? p.getRestartCount() : 0,
                            state,
                            null,
                            p.getStatusMessage(),
                            null,
                            p.getStartTime()
                    ));
                }

                String healthStatus = "UNKNOWN";
                if (w.getDesiredReplicas() == 0) {
                    healthStatus = "STANDBY";
                } else if (w.getReadyReplicas() >= w.getDesiredReplicas() && w.getAvailableReplicas() >= w.getDesiredReplicas()) {
                    healthStatus = "HEALTHY";
                } else if (w.getReadyReplicas() > 0) {
                    healthStatus = "DEGRADED";
                } else {
                    healthStatus = "FAILED";
                }

                results.add(new WorkloadHealthResponse(
                        w.getNamespace(),
                        w.getName(),
                        w.getDesiredReplicas(),
                        w.getUpdatedReplicas(),
                        w.getAvailableReplicas(),
                        w.getReadyReplicas(),
                        healthStatus,
                        w.getImage(),
                        Collections.emptyList(),
                        podSummaries
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to query workload health: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Deep inspection of a specific workload by deployment name.
     */
    public Optional<WorkloadHealthResponse> getWorkloadHealth(String namespace, String deploymentName) {
        return getWorkloadHealthList(namespace).stream()
                .filter(w -> w.getDeploymentName() != null && w.getDeploymentName().equalsIgnoreCase(deploymentName))
                .findFirst();
    }

    /**
     * Tier 2: Cloud Infrastructure Health.
     */
    public Map<String, Object> getInfrastructureHealth() {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            map.put("azure", azureService.getAzureStatus());
        } catch (Exception e) {
            map.put("azure", Map.of("status", "ERROR", "message", e.getMessage()));
        }
        try {
            map.put("acr", acrService.getRegistryDetails());
        } catch (Exception e) {
            map.put("acr", Map.of("status", "ERROR", "message", e.getMessage()));
        }
        try {
            map.put("aks", aksService.getClusterDetails());
        } catch (Exception e) {
            map.put("aks", Map.of("status", "ERROR", "message", e.getMessage()));
        }
        return map;
    }

    /**
     * Tier 3: Summary of pipeline executions and success rates.
     */
    public Map<String, Object> getPipelineSummary() {
        Map<String, Object> map = new HashMap<>();
        long total = pipelineExecutionRepository.count();
        long running = pipelineExecutionRepository.countActive();
        List<PipelineExecution> recent = pipelineExecutionRepository.findTop10ByOrderByCreatedAtDesc();

        long successful = 0;
        long failed = 0;
        for (PipelineExecution p : recent) {
            if (p.getStatus() == com.cloudship.entity.PipelineStatus.SUCCESS) {
                successful++;
            } else if (p.getStatus().isFailure()) {
                failed++;
            }
        }

        double successRate = (successful + failed) > 0 ? ((double) successful / (successful + failed)) * 100.0 : 0.0;

        map.put("totalPipelines", total);
        map.put("activeRunning", running);
        map.put("recentSuccessful", successful);
        map.put("recentFailed", failed);
        map.put("successRatePercent", Math.round(successRate * 10.0) / 10.0);
        return map;
    }

    /**
     * Tier 3: Full operational metrics calculation without synthetic numbers.
     */
    public MonitoringMetricsResponse getMetrics() {
        MonitoringMetricsResponse metrics = new MonitoringMetricsResponse();
        metrics.setCalculatedAt(OffsetDateTime.now());

        // Pipelines
        long totalPipelines = pipelineExecutionRepository.count();
        long runningPipelines = pipelineExecutionRepository.countActive();
        metrics.setTotalPipelines(totalPipelines);
        metrics.setRunningPipelines(runningPipelines);

        List<PipelineExecution> allPipelines = pipelineExecutionRepository.findAll();
        long successPipes = 0;
        long failPipes = 0;
        long totalPipeDuration = 0;
        long pipeWithDurationCount = 0;

        for (PipelineExecution p : allPipelines) {
            if (p.getStatus() == com.cloudship.entity.PipelineStatus.SUCCESS) {
                successPipes++;
            } else if (p.getStatus().isFailure()) {
                failPipes++;
            }
            if (p.getDurationSeconds() != null && p.getDurationSeconds() > 0) {
                totalPipeDuration += p.getDurationSeconds();
                pipeWithDurationCount++;
            }
        }
        metrics.setSuccessfulPipelines(successPipes);
        metrics.setFailedPipelines(failPipes);
        metrics.setPipelineSuccessRatePercent(
                (successPipes + failPipes) > 0 ? Math.round(((double) successPipes / (successPipes + failPipes)) * 1000.0) / 10.0 : 0.0
        );
        metrics.setAvgPipelineDurationSeconds(
                pipeWithDurationCount > 0 ? (double) (totalPipeDuration / pipeWithDurationCount) : null
        );

        // Deployments
        long totalDeployments = deploymentRepository.count();
        metrics.setTotalDeployments(totalDeployments);
        var allDeployments = deploymentRepository.findAll();
        long activeDeps = 0;
        long successDeps = 0;
        long failDeps = 0;
        for (var d : allDeployments) {
            if (d.getStatus() == com.cloudship.entity.DeploymentStatus.RUNNING || d.getStatus() == com.cloudship.entity.DeploymentStatus.PENDING) {
                activeDeps++;
            } else if (d.getStatus() == com.cloudship.entity.DeploymentStatus.SUCCESS) {
                successDeps++;
            } else if (d.getStatus() == com.cloudship.entity.DeploymentStatus.FAILED) {
                failDeps++;
            }
        }
        metrics.setActiveDeployments(activeDeps);
        metrics.setSuccessfulDeployments(successDeps);
        metrics.setFailedDeployments(failDeps);
        metrics.setDeploymentSuccessRatePercent(
                (successDeps + failDeps) > 0 ? Math.round(((double) successDeps / (successDeps + failDeps)) * 1000.0) / 10.0 : 0.0
        );

        // Workloads & Pods
        try {
            List<KubernetesPodResponse> pods = aksService.listPods(null, null);
            metrics.setTotalPods(pods.size());
            int running = 0;
            int pending = 0;
            int failed = 0;
            int restarts = 0;
            for (KubernetesPodResponse p : pods) {
                if (isPodReady(p)) running++;
                else if ("Pending".equalsIgnoreCase(p.getPhase())) pending++;
                else if (isPodFailed(p)) failed++;
                if (p.getRestartCount() != null) {
                    restarts += p.getRestartCount();
                }
            }
            metrics.setRunningPods(running);
            metrics.setPendingPods(pending);
            metrics.setFailedPods(failed);
            metrics.setTotalContainerRestarts(restarts);
        } catch (Exception e) {
            metrics.setTotalPods(0);
            metrics.setRunningPods(0);
            metrics.setPendingPods(0);
            metrics.setFailedPods(0);
            metrics.setTotalContainerRestarts(0);
        }

        return metrics;
    }

    private boolean isPodReady(KubernetesPodResponse p) {
        if (p == null) return false;
        if (p.getReadyContainers() != null && p.getTotalContainers() != null && p.getTotalContainers() > 0) {
            return p.getReadyContainers().equals(p.getTotalContainers());
        }
        if (p.getReady() != null && p.getReady().contains("/")) {
            String[] parts = p.getReady().split("/");
            return parts.length == 2 && parts[0].equals(parts[1]) && !parts[0].equals("0");
        }
        return "Running".equalsIgnoreCase(p.getPhase());
    }

    private boolean isPodFailed(KubernetesPodResponse p) {
        if (p == null) return false;
        if ("Failed".equalsIgnoreCase(p.getPhase())) return true;
        if (p.getStatusMessage() != null && p.getStatusMessage().toLowerCase().contains("crashloopbackoff")) return true;
        return false;
    }

    /**
     * Operational Events Query with project and limit filters.
     */
    public List<MonitoringEventResponse> getRecentEvents(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageRequest page = PageRequest.of(0, safeLimit);

        List<MonitoringEvent> events;
        if (projectId != null) {
            events = monitoringEventRepository.findByProjectIdOrderByCreatedAtDesc(projectId, page);
        } else {
            events = monitoringEventRepository.findByOrderByCreatedAtDesc(page);
        }

        return events.stream().map(e -> new MonitoringEventResponse(
                e.getId(),
                e.getProject() != null ? e.getProject().getId() : null,
                e.getProject() != null ? e.getProject().getName() : "System",
                e.getEventType(),
                e.getSeverity(),
                e.getSource(),
                e.getMessage(),
                e.getDetailsJson(),
                e.getCreatedAt()
        )).toList();
    }

    /**
     * Records a real operational event into PostgreSQL.
     */
    @Transactional
    public MonitoringEvent recordEvent(Long projectId, String eventType, String severity,
                                       String source, String message, String detailsJson) {
        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId).orElse(null);
        }

        MonitoringEvent event = new MonitoringEvent(project, eventType, severity, source, message, detailsJson);
        MonitoringEvent saved = monitoringEventRepository.save(event);
        log.info("Recorded operational event [{} - {}]: {}", severity, eventType, message);

        // Invalidate short-lived cache so dashboard reflects recent state
        cachedOverview.set(null);
        return saved;
    }

    /**
     * Listens to CI build status updates and records operational events.
     */
    @EventListener
    public void handleCIBuildStatusChange(CIBuildStatusChangedEvent event) {
        CIBuild build = event.getBuild();
        if (build == null) return;

        Long projectId = build.getProject() != null ? build.getProject().getId() : null;
        String bNum = build.getJenkinsBuildNumber() != null ? "#" + build.getJenkinsBuildNumber() : "#" + build.getId();

        if (build.getStatus() == com.cloudship.entity.CIBuildStatus.RUNNING) {
            recordEvent(projectId, "CI_BUILD_STARTED", "INFO", "PIPELINE",
                    "Jenkins CI build " + bNum + " started for branch " + build.getBranch(), null);
        } else if (build.getStatus() == com.cloudship.entity.CIBuildStatus.SUCCESS) {
            recordEvent(projectId, "CI_BUILD_SUCCEEDED", "INFO", "PIPELINE",
                    "Jenkins CI build " + bNum + " passed unit tests and packaged container image", null);
        } else if (build.getStatus() == com.cloudship.entity.CIBuildStatus.FAILED) {
            recordEvent(projectId, "CI_BUILD_FAILED", "ERROR", "PIPELINE",
                    "Jenkins CI build " + bNum + " failed: " + (build.getErrorMessage() != null ? build.getErrorMessage() : "Build error"), null);
        }

        if (build.getPushStatus() == com.cloudship.entity.CIPushStatus.RUNNING) {
            recordEvent(projectId, "IMAGE_PUSH_STARTED", "INFO", "ACR",
                    "Pushing container image " + build.getDockerImageTag() + " to Azure Container Registry", null);
        } else if (build.getPushStatus() == com.cloudship.entity.CIPushStatus.SUCCESS) {
            recordEvent(projectId, "IMAGE_PUSH_SUCCEEDED", "INFO", "ACR",
                    "Pushed image " + build.getDockerImageTag() + " to ACR with digest " + (build.getImageDigest() != null ? build.getImageDigest() : "--"), null);
        } else if (build.getPushStatus() == com.cloudship.entity.CIPushStatus.FAILED) {
            recordEvent(projectId, "IMAGE_PUSH_FAILED", "ERROR", "ACR",
                    "Failed pushing container image to ACR: " + (build.getPushErrorMessage() != null ? build.getPushErrorMessage() : "ACR error"), null);
        }
    }

    /**
     * Listens to Kubernetes deployment events and records operational events.
     */
    @EventListener
    public void handleDeploymentStatusChange(com.cloudship.event.DeploymentStatusChangedEvent event) {
        com.cloudship.entity.Deployment d = event.getDeployment();
        if (d == null) return;

        Long projectId = d.getProject() != null ? d.getProject().getId() : null;
        String name = d.getDeploymentName() != null ? d.getDeploymentName() : "Workload";
        String ns = d.getNamespace() != null ? d.getNamespace() : "default";

        if (d.getStatus() == com.cloudship.entity.DeploymentStatus.RUNNING) {
            recordEvent(projectId, "DEPLOYMENT_STARTED", "INFO", "KUBERNETES",
                    "Deployment rollout initiated for workload '" + name + "' in namespace '" + ns + "'", null);
        } else if (d.getStatus() == com.cloudship.entity.DeploymentStatus.SUCCESS) {
            recordEvent(projectId, "DEPLOYMENT_SUCCEEDED", "INFO", "KUBERNETES",
                    "Deployment rollout verified healthy for workload '" + name + "' (" + d.getReadyReplicas() + "/" + d.getReplicas() + " replicas ready)", null);
        } else if (d.getStatus() == com.cloudship.entity.DeploymentStatus.FAILED) {
            recordEvent(projectId, "DEPLOYMENT_FAILED", "ERROR", "KUBERNETES",
                    "Deployment rollout failed for workload '" + name + "': " + (d.getErrorMessage() != null ? d.getErrorMessage() : "Rollout error"), null);
        }
    }

    /**
     * Listens to end-to-end pipeline execution events and records operational events.
     */
    @EventListener
    public void handlePipelineStatusChange(com.cloudship.event.PipelineStatusChangedEvent event) {
        PipelineExecution p = event.getPipelineExecution();
        if (p == null) return;

        Long projectId = p.getProject() != null ? p.getProject().getId() : null;
        String pId = "#" + p.getId();

        if (p.getStatus() == com.cloudship.entity.PipelineStatus.QUEUED || p.getStatus() == com.cloudship.entity.PipelineStatus.CI_RUNNING) {
            recordEvent(projectId, "PIPELINE_STARTED", "INFO", "PIPELINE",
                    "Pipeline " + pId + " started for branch " + p.getBranch(), null);
        } else if (p.getStatus() == com.cloudship.entity.PipelineStatus.SUCCESS) {
            recordEvent(projectId, "PIPELINE_SUCCEEDED", "INFO", "PIPELINE",
                    "Pipeline " + pId + " completed successfully in " + (p.getDurationSeconds() != null ? p.getDurationSeconds() + "s" : "--"), null);
        } else if (p.getStatus().isFailure()) {
            recordEvent(projectId, "PIPELINE_FAILED", "ERROR", "PIPELINE",
                    "Pipeline " + pId + " failed at step " + p.getStatus() + ": " + (p.getErrorMessage() != null ? p.getErrorMessage() : "Error"), null);
        }
    }
}
