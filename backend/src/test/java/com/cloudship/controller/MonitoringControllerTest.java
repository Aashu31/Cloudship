package com.cloudship.controller;

import com.cloudship.dto.monitoring.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.MonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoringController.class)
@ActiveProfiles("test")
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringService monitoringService;

    @Test
    @DisplayName("GET /api/monitoring/overview returns 200 with 3-tier health payload")
    void testGetOverview_Returns200() throws Exception {
        MonitoringOverviewResponse overview = new MonitoringOverviewResponse();
        overview.setApplication(Map.of("status", "HEALTHY", "jvmMemoryUsedMb", 128));
        overview.setDatabase(Map.of("status", "CONNECTED", "responseTimeMs", 4));
        overview.setPipelines(Map.of("totalPipelines", 5, "successRatePercent", 100.0));
        overview.setRecentEvents(Collections.emptyList());

        when(monitoringService.getMonitoringOverview(anyBoolean())).thenReturn(overview);

        mockMvc.perform(get("/api/monitoring/overview?refresh=true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("HEALTHY"))
                .andExpect(jsonPath("$.database.status").value("CONNECTED"))
                .andExpect(jsonPath("$.pipelines.successRatePercent").value(100.0));
    }

    @Test
    @DisplayName("GET /api/monitoring/application returns 200 with JVM and runtime metrics")
    void testGetApplication_Returns200() throws Exception {
        Map<String, Object> appHealth = Map.of(
                "status", "HEALTHY",
                "uptimeSeconds", 3600L,
                "jvmMemoryUsedMb", 120,
                "threadCount", 24
        );
        when(monitoringService.getApplicationHealth()).thenReturn(appHealth);

        mockMvc.perform(get("/api/monitoring/application"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.uptimeSeconds").value(3600))
                .andExpect(jsonPath("$.jvmMemoryUsedMb").value(120));
    }

    @Test
    @DisplayName("GET /api/monitoring/infrastructure returns 200 with Azure, ACR, and AKS states")
    void testGetInfrastructure_Returns200() throws Exception {
        Map<String, Object> infra = Map.of(
                "azure", Map.of("configured", true),
                "acr", Map.of("name", "cloudshipcr"),
                "aks", Map.of("status", "READY")
        );
        when(monitoringService.getInfrastructureHealth()).thenReturn(infra);

        mockMvc.perform(get("/api/monitoring/infrastructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.azure.configured").value(true))
                .andExpect(jsonPath("$.acr.name").value("cloudshipcr"));
    }

    @Test
    @DisplayName("GET /api/monitoring/kubernetes returns 200 with workloads and pod breakdown")
    void testGetKubernetes_Returns200() throws Exception {
        WorkloadHealthResponse workload = new WorkloadHealthResponse(
                "default", "cloudship-core", 2, 2, 2, 2, "HEALTHY", "cloudshipcr.azurecr.io/core:v1",
                List.of("Available"), Collections.emptyList()
        );
        when(monitoringService.getWorkloadHealthList(any())).thenReturn(List.of(workload));

        mockMvc.perform(get("/api/monitoring/kubernetes?namespace=default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deploymentName").value("cloudship-core"))
                .andExpect(jsonPath("$[0].status").value("HEALTHY"))
                .andExpect(jsonPath("$[0].readyReplicas").value(2));
    }

    @Test
    @DisplayName("GET /api/monitoring/workloads/{deploymentName} returns 200 when workload exists")
    void testGetWorkload_Returns200() throws Exception {
        WorkloadHealthResponse workload = new WorkloadHealthResponse(
                "default", "auth-service", 1, 1, 1, 1, "HEALTHY", "cloudshipcr.azurecr.io/auth:v2",
                List.of("Available"), Collections.emptyList()
        );
        when(monitoringService.getWorkloadHealth("default", "auth-service"))
                .thenReturn(Optional.of(workload));

        mockMvc.perform(get("/api/monitoring/workloads/auth-service?namespace=default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentName").value("auth-service"))
                .andExpect(jsonPath("$.status").value("HEALTHY"));
    }

    @Test
    @DisplayName("GET /api/monitoring/workloads/{deploymentName} returns 404 when workload not found")
    void testGetWorkload_NotFound_Returns404() throws Exception {
        when(monitoringService.getWorkloadHealth("default", "non-existent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/monitoring/workloads/non-existent?namespace=default"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/monitoring/events returns 200 with operational audit log")
    void testGetEvents_Returns200() throws Exception {
        MonitoringEventResponse event = new MonitoringEventResponse(
                1L, 10L, "sample-project", "DEPLOYMENT_SUCCEEDED", "INFO", "KUBERNETES",
                "Rollout verified healthy", null, OffsetDateTime.now()
        );
        when(monitoringService.getRecentEvents(isNull(), eq(50)))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/api/monitoring/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("DEPLOYMENT_SUCCEEDED"))
                .andExpect(jsonPath("$[0].severity").value("INFO"))
                .andExpect(jsonPath("$[0].source").value("KUBERNETES"));
    }

    @Test
    @DisplayName("GET /api/monitoring/metrics returns 200 with concrete metrics without fake percentages")
    void testGetMetrics_Returns200() throws Exception {
        MonitoringMetricsResponse metrics = new MonitoringMetricsResponse();
        metrics.setTotalPipelines(10L);
        metrics.setRunningPipelines(1L);
        metrics.setSuccessfulPipelines(8L);
        metrics.setFailedPipelines(1L);
        metrics.setPipelineSuccessRatePercent(88.9);
        metrics.setTotalDeployments(5L);
        metrics.setTotalPods(6);
        metrics.setRunningPods(6);
        metrics.setFailedPods(0);
        metrics.setTotalContainerRestarts(0);

        when(monitoringService.getMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/monitoring/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPipelines").value(10))
                .andExpect(jsonPath("$.successfulPipelines").value(8))
                .andExpect(jsonPath("$.pipelineSuccessRatePercent").value(88.9))
                .andExpect(jsonPath("$.runningPods").value(6));
    }
}
