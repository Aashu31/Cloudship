package com.cloudship.controller;

import com.cloudship.dto.monitoring.MonitoringEventResponse;
import com.cloudship.dto.monitoring.MonitoringMetricsResponse;
import com.cloudship.dto.monitoring.MonitoringOverviewResponse;
import com.cloudship.dto.monitoring.WorkloadHealthResponse;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CloudShip Version 8 — Operational Monitoring & Observability Controller.
 * Exposes three-tier health status, live Kubernetes workload metrics, real pipeline telemetry,
 * and operational audit event logging.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringController.class);

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Complete Three-Tier Observability Overview (Cached with 15s TTL).
     */
    @GetMapping("/overview")
    public ResponseEntity<MonitoringOverviewResponse> getOverview(
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        log.debug("Fetching complete monitoring overview (refresh={})", refresh);
        return ResponseEntity.ok(monitoringService.getMonitoringOverview(refresh));
    }

    /**
     * Tier 1: Application Health & JVM Metrics.
     */
    @GetMapping("/application")
    public ResponseEntity<Map<String, Object>> getApplicationHealth() {
        log.debug("Fetching application & JVM health");
        return ResponseEntity.ok(monitoringService.getApplicationHealth());
    }

    /**
     * Tier 2: Cloud Infrastructure Health (Azure, ACR, AKS).
     */
    @GetMapping("/infrastructure")
    public ResponseEntity<Map<String, Object>> getInfrastructureHealth() {
        log.debug("Fetching cloud infrastructure health");
        return ResponseEntity.ok(monitoringService.getInfrastructureHealth());
    }

    /**
     * Tier 2: Kubernetes Workloads Health & Pod status.
     */
    @GetMapping("/kubernetes")
    public ResponseEntity<List<WorkloadHealthResponse>> getKubernetesWorkloads(
            @RequestParam(required = false) String namespace) {
        log.debug("Fetching Kubernetes workload health for namespace: {}", namespace);
        return ResponseEntity.ok(monitoringService.getWorkloadHealthList(namespace));
    }

    /**
     * Tier 2: Deep inspection of a specific workload / deployment.
     */
    @GetMapping("/workloads/{deploymentName}")
    public ResponseEntity<WorkloadHealthResponse> getWorkloadHealth(
            @PathVariable String deploymentName,
            @RequestParam(required = false, defaultValue = "default") String namespace) {
        log.debug("Fetching workload health for deployment '{}' in namespace '{}'", deploymentName, namespace);
        return monitoringService.getWorkloadHealth(namespace, deploymentName)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Workload deployment '" + deploymentName + "' not found in namespace '" + namespace + "'"));
    }

    /**
     * Tier 3: Operational Audit Events Feed.
     */
    @GetMapping("/events")
    public ResponseEntity<List<MonitoringEventResponse>> getEvents(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        log.debug("Fetching operational events (projectId: {}, limit: {})", projectId, limit);
        return ResponseEntity.ok(monitoringService.getRecentEvents(projectId, limit));
    }

    /**
     * Tier 3: Concrete Operational Metrics (no fake numbers, no synthetic percentages).
     */
    @GetMapping("/metrics")
    public ResponseEntity<MonitoringMetricsResponse> getMetrics() {
        log.debug("Fetching concrete operational metrics");
        return ResponseEntity.ok(monitoringService.getMetrics());
    }
}
