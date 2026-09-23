package com.cloudship.service;

import com.cloudship.dto.azure.*;
import com.cloudship.dto.monitoring.*;
import com.cloudship.entity.*;
import com.cloudship.event.*;
import com.cloudship.repository.*;
import com.cloudship.service.azure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private AzureInfrastructureService azureService;

    @Mock
    private AzureContainerRegistryService acrService;

    @Mock
    private AzureAksService aksService;

    @Mock
    private PipelineExecutionRepository pipelineExecutionRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MonitoringEventRepository monitoringEventRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(
                dataSource,
                azureService,
                acrService,
                aksService,
                pipelineExecutionRepository,
                deploymentRepository,
                projectRepository,
                monitoringEventRepository
        );
    }

    @Test
    @DisplayName("Tier 1: Application and Database health returns healthy when DB connection is valid")
    void testGetApplicationAndDatabaseHealth() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("18.0");

        Map<String, Object> appHealth = monitoringService.getApplicationHealth();
        assertNotNull(appHealth);
        assertEquals("UP", appHealth.get("status"));
        assertTrue((Long) appHealth.get("uptimeSeconds") >= 0);

        Map<String, Object> dbHealth = monitoringService.getDatabaseHealth();
        assertNotNull(dbHealth);
        assertEquals("CONNECTED", dbHealth.get("status"));
        assertEquals("PostgreSQL", dbHealth.get("databaseProduct"));
    }

    @Test
    @DisplayName("Tier 2: Workload and Pod health correctly identifies HEALTHY and DEGRADED states")
    void testGetWorkloadHealthList() {
        KubernetesWorkloadResponse w1 = new KubernetesWorkloadResponse(
                "cloudship-api", "default", 2, 2, 2, 2, "cloudshipcr.azurecr.io/api:v1", "RUNNING",
                OffsetDateTime.now()
        );
        KubernetesPodResponse p1 = new KubernetesPodResponse("cloudship-api-1", "default", "node-1", "Running", "1/1", 1, 1, 0, OffsetDateTime.now(), "1h", "Running");
        KubernetesPodResponse p2 = new KubernetesPodResponse("cloudship-api-2", "default", "node-1", "Running", "1/1", 1, 1, 0, OffsetDateTime.now(), "1h", "Running");

        when(aksService.listWorkloads("default")).thenReturn(List.of(w1));
        when(aksService.listPods("default", "cloudship-api")).thenReturn(List.of(p1, p2));

        List<WorkloadHealthResponse> list = monitoringService.getWorkloadHealthList("default");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("HEALTHY", list.get(0).getStatus());
        assertEquals(2, list.get(0).getPods().size());
        assertTrue(list.get(0).getPods().get(0).isReady());
    }

    @Test
    @DisplayName("Tier 2: Workload with Pending and CrashLoopBackOff pods is marked FAILED or DEGRADED")
    void testGetWorkloadHealth_DegradedOrFailed() {
        // Desired: 2, Ready: 1 -> DEGRADED
        KubernetesWorkloadResponse w2 = new KubernetesWorkloadResponse(
                "failing-svc", "default", 2, 1, 1, 1, "cloudshipcr.azurecr.io/fail:v1", "DEGRADED",
                OffsetDateTime.now()
        );
        KubernetesPodResponse pCrash = new KubernetesPodResponse("failing-svc-1", "default", "node-1", "Failed", "0/1", 0, 1, 5, OffsetDateTime.now(), "1h", "CrashLoopBackOff");

        when(aksService.listWorkloads("default")).thenReturn(List.of(w2));
        when(aksService.listPods("default", "failing-svc")).thenReturn(List.of(pCrash));

        List<WorkloadHealthResponse> list = monitoringService.getWorkloadHealthList("default");
        assertEquals(1, list.size());
        assertEquals("DEGRADED", list.get(0).getStatus());
        assertFalse(list.get(0).getPods().get(0).isReady());
    }

    @Test
    @DisplayName("Overview Caching: Second call within 15 seconds returns cached response without duplicate queries")
    void testOverviewCaching() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(pipelineExecutionRepository.count()).thenReturn(5L);
        when(pipelineExecutionRepository.countActive()).thenReturn(1L);
        when(pipelineExecutionRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(monitoringEventRepository.findByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(Collections.emptyList());

        MonitoringOverviewResponse first = monitoringService.getMonitoringOverview(false);
        assertNotNull(first);

        // Second call should hit in-memory cache
        MonitoringOverviewResponse second = monitoringService.getMonitoringOverview(false);
        assertSame(first, second);

        // Verify downstream DB connection only queried once
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    @DisplayName("Tier 3 Metrics: Calculates real pipeline success rate and deployment count without fake numbers")
    void testGetMetrics_CalculatesRealData() {
        Project project = new Project("test-app", "desc", "https://github.com/cloudship/test-app");
        PipelineExecution p1 = new PipelineExecution(project, "main", "sha1", CITriggerType.MANUAL);
        p1.setStatus(PipelineStatus.SUCCESS);
        p1.setDurationMs(12000L);

        PipelineExecution p2 = new PipelineExecution(project, "main", "sha2", CITriggerType.MANUAL);
        p2.setStatus(PipelineStatus.CI_FAILED);

        when(pipelineExecutionRepository.count()).thenReturn(2L);
        when(pipelineExecutionRepository.countActive()).thenReturn(0L);
        when(pipelineExecutionRepository.findAll()).thenReturn(List.of(p1, p2));
        when(deploymentRepository.count()).thenReturn(1L);
        when(deploymentRepository.findAll()).thenReturn(Collections.emptyList());
        when(aksService.listPods(isNull(), isNull())).thenReturn(Collections.emptyList());

        MonitoringMetricsResponse metrics = monitoringService.getMetrics();
        assertNotNull(metrics);
        assertEquals(2L, metrics.getTotalPipelines());
        assertEquals(1L, metrics.getSuccessfulPipelines());
        assertEquals(1L, metrics.getFailedPipelines());
        assertEquals(50.0, metrics.getPipelineSuccessRatePercent());
        assertEquals(12.0, metrics.getAvgPipelineDurationSeconds());
    }

    @Test
    @DisplayName("Event Listeners: DeploymentStatusChangedEvent records real audit event to PostgreSQL")
    void testDeploymentEventListener() {
        Project project = new Project("shop-api", "desc", "https://github.com/cloudship/shop-api");
        Deployment dep = new Deployment();
        dep.setProject(project);
        dep.setDeploymentName("shop-api");
        dep.setNamespace("default");
        dep.setStatus(DeploymentStatus.SUCCESS);
        dep.setReplicas(2);
        dep.setReadyReplicas(2);

        when(monitoringEventRepository.save(any(MonitoringEvent.class))).thenAnswer(i -> i.getArgument(0));

        monitoringService.handleDeploymentStatusChange(new DeploymentStatusChangedEvent(dep));

        verify(monitoringEventRepository, times(1)).save(argThat(event ->
                "DEPLOYMENT_SUCCEEDED".equals(event.getEventType()) &&
                "INFO".equals(event.getSeverity()) &&
                "KUBERNETES".equals(event.getSource()) &&
                event.getMessage().contains("shop-api")
        ));
    }

    @Test
    @DisplayName("Event Listeners: PipelineStatusChangedEvent records real audit event to PostgreSQL")
    void testPipelineEventListener() {
        Project project = new Project("core-api", "desc", "https://github.com/cloudship/core-api");
        PipelineExecution pipeline = new PipelineExecution(project, "develop", "commit123", CITriggerType.MANUAL);
        pipeline.setId(42L);
        pipeline.setStatus(PipelineStatus.SUCCESS);
        pipeline.setDurationMs(25000L);

        when(monitoringEventRepository.save(any(MonitoringEvent.class))).thenAnswer(i -> i.getArgument(0));

        monitoringService.handlePipelineStatusChange(new PipelineStatusChangedEvent(pipeline));

        verify(monitoringEventRepository, times(1)).save(argThat(event ->
                "PIPELINE_SUCCEEDED".equals(event.getEventType()) &&
                "PIPELINE".equals(event.getSource()) &&
                event.getMessage().contains("#42") &&
                event.getMessage().contains("25s")
        ));
    }
}
