package com.cloudship.service;

import com.cloudship.dto.*;
import com.cloudship.dto.azure.AcrVerificationResponse;
import com.cloudship.dto.azure.AzureResourceStatus;
import com.cloudship.entity.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.PipelineExecutionRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.azure.AzureContainerRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineExecutionRepository pipelineExecutionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GitRepositoryRepository gitRepositoryRepository;

    @Mock
    private CIBuildRepository ciBuildRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private CIService ciService;

    @Mock
    private AzureContainerRegistryService acrService;

    @Mock
    private DeploymentService deploymentService;

    private PipelineService pipelineService;

    private Project testProject;
    private GitRepository testRepo;
    private CIBuild testBuild;
    private PipelineExecution testPipeline;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        pipelineService = new PipelineService(
                pipelineExecutionRepository,
                projectRepository,
                gitRepositoryRepository,
                ciBuildRepository,
                deploymentRepository,
                ciService,
                acrService,
                deploymentService,
                transactionTemplate
        );

        testProject = new Project("payment-service", "Payment API", "https://github.com/org/payment-service.git");
        testProject.setId(1L);

        testRepo = new GitRepository(testProject, "https://github.com/org/payment-service.git", "org", "payment-service", "main", GitConnectionStatus.CONNECTED);

        testBuild = new CIBuild();
        testBuild.setId(10L);
        testBuild.setProject(testProject);
        testBuild.setStatus(CIBuildStatus.RUNNING);
        testBuild.setDockerImageName("cloudshipcr.azurecr.io/cloudship/payment-service");
        testBuild.setDockerImageTag("main-abc1234");
        testBuild.setImageDigest("sha256:1111222233334444555566667777888899990000aaaaabbbbbcccccdddddeeeee");

        testPipeline = new PipelineExecution(testProject, "main", "abc1234", CITriggerType.MANUAL);
        testPipeline.setId(100L);
        testPipeline.setCiBuild(testBuild);
        testPipeline.setStatus(PipelineStatus.CI_RUNNING);
        testPipeline.setImageName(testBuild.getDockerImageName());
        testPipeline.setImageTag(testBuild.getDockerImageTag());
        testPipeline.setImageDigest(testBuild.getImageDigest());
    }

    @Test
    @DisplayName("1. CI trigger and pipeline initialization: transitions to CI_RUNNING")
    void testTriggerPipeline_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(gitRepositoryRepository.findByProjectId(1L)).thenReturn(Optional.of(testRepo));
        when(pipelineExecutionRepository.findFirstByProjectIdAndStatusInOrderByCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(Optional.empty());

        when(pipelineExecutionRepository.save(any(PipelineExecution.class)))
                .thenAnswer(invocation -> {
                    PipelineExecution p = invocation.getArgument(0);
                    if (p.getId() == null) p.setId(100L);
                    return p;
                });

        CIBuildResponse ciResponse = new CIBuildResponse();
        ciResponse.setId(10L);
        ciResponse.setStatus(CIBuildStatus.RUNNING);
        when(ciService.triggerBuild(eq(1L), any(), any())).thenReturn(ciResponse);
        when(ciBuildRepository.findById(10L)).thenReturn(Optional.of(testBuild));

        PipelineTriggerRequest request = new PipelineTriggerRequest("main", "abc1234", "Initial commit", "Dev");
        PipelineExecutionResponse response = pipelineService.triggerPipeline(1L, request, CITriggerType.MANUAL);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(PipelineStatus.CI_RUNNING);
        assertThat(response.getStage()).isEqualTo("BUILD");
        verify(ciService).triggerBuild(eq(1L), any(), eq(CITriggerType.MANUAL));
    }

    @Test
    @DisplayName("2. CI Failure propagates into CI_FAILED and halts pipeline")
    void testHandleCIBuildUpdate_CiFailure() {
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.FAILED);
        testBuild.setErrorMessage("Maven compilation failed: Compilation error");

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.CI_FAILED);
        assertThat(testPipeline.getErrorMessage()).contains("Maven compilation failed");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
        verify(acrService, never()).verifyImage(any(), any(), any());
        verify(deploymentService, never()).triggerDeployment(any());
        verify(pipelineExecutionRepository).save(testPipeline);
    }

    @Test
    @DisplayName("3. ACR push running transitions pipeline to IMAGE_PUSHING")
    void testHandleCIBuildUpdate_AcrPushRunning() {
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.RUNNING);
        testBuild.setPushStatus(CIPushStatus.RUNNING);

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.IMAGE_PUSHING);
        assertThat(testPipeline.getStatus().isRunning()).isTrue();
        verify(pipelineExecutionRepository).save(testPipeline);
    }

    @Test
    @DisplayName("4. ACR push failure transitions pipeline to IMAGE_PUSH_FAILED and never deploys")
    void testHandleCIBuildUpdate_AcrPushFailure() {
        testPipeline.setStatus(PipelineStatus.IMAGE_PUSHING);
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.FAILED);
        testBuild.setPushStatus(CIPushStatus.FAILED);
        testBuild.setPushErrorMessage("Failed to push image to ACR: unauthorized");

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.IMAGE_PUSH_FAILED);
        assertThat(testPipeline.getErrorMessage()).contains("Failed to push image to ACR");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
        verify(deploymentService, never()).triggerDeployment(any());
        verify(pipelineExecutionRepository).save(testPipeline);
    }

    @Test
    @DisplayName("5. Image verification success allows deployment to proceed to AKS")
    void testHandleCIBuildUpdate_ImageVerificationSuccess_DeploysToAks() {
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.SUCCESS);
        testBuild.setPushStatus(CIPushStatus.SUCCESS);

        when(acrService.verifyImage(eq(testBuild.getDockerImageName()), eq(testBuild.getDockerImageTag()), eq(testBuild.getImageDigest())))
                .thenReturn(new AcrVerificationResponse(true, testBuild.getDockerImageName(), testBuild.getDockerImageTag(), testBuild.getImageDigest(), "cloudshipcr.azurecr.io", AzureResourceStatus.READY, "Verified"));

        DeploymentResponse depResp = new DeploymentResponse();
        depResp.setId(50L);
        depResp.setStatus(DeploymentStatus.RUNNING);
        depResp.setRolloutStatus("RUNNING");
        when(deploymentService.triggerDeployment(any())).thenReturn(depResp);

        Deployment depEntity = new Deployment(testProject, "main-abc1234", DeploymentStatus.RUNNING);
        depEntity.setId(50L);
        when(deploymentRepository.findById(50L)).thenReturn(Optional.of(depEntity));

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.ROLLOUT_VERIFYING);
        assertThat(testPipeline.getDeployment()).isEqualTo(depEntity);
        verify(deploymentService).triggerDeployment(any());
        verify(pipelineExecutionRepository, atLeastOnce()).save(testPipeline);
    }

    @Test
    @DisplayName("6. Image verification failure halts pipeline with IMAGE_PUSH_FAILED and never deploys")
    void testHandleCIBuildUpdate_ImageVerificationFailure_NeverDeploys() {
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.SUCCESS);
        testBuild.setPushStatus(CIPushStatus.SUCCESS);

        when(acrService.verifyImage(any(), any(), any()))
                .thenReturn(new AcrVerificationResponse(false, testBuild.getDockerImageName(), testBuild.getDockerImageTag(), null, "cloudshipcr.azurecr.io", AzureResourceStatus.ERROR, "Digest mismatch detected"));

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.IMAGE_PUSH_FAILED);
        assertThat(testPipeline.getErrorMessage()).contains("Digest mismatch detected");
        verify(deploymentService, never()).triggerDeployment(any());
        verify(pipelineExecutionRepository, atLeastOnce()).save(testPipeline);
    }

    @Test
    @DisplayName("7. AKS deployment failure transitions pipeline to DEPLOYMENT_FAILED")
    void testHandleCIBuildUpdate_AksDeploymentFailure() {
        when(pipelineExecutionRepository.findByCiBuildId(10L)).thenReturn(Optional.of(testPipeline));

        testBuild.setStatus(CIBuildStatus.SUCCESS);
        testBuild.setPushStatus(CIPushStatus.SUCCESS);

        when(acrService.verifyImage(any(), any(), any()))
                .thenReturn(new AcrVerificationResponse(true, testBuild.getDockerImageName(), testBuild.getDockerImageTag(), testBuild.getImageDigest(), "cloudshipcr.azurecr.io", AzureResourceStatus.READY, "Verified"));

        when(deploymentService.triggerDeployment(any())).thenThrow(new RuntimeException("Kubernetes API error: 403 Forbidden"));

        pipelineService.handleCIBuildUpdate(testBuild);

        assertThat(testPipeline.getStatus()).isEqualTo(PipelineStatus.DEPLOYMENT_FAILED);
        assertThat(testPipeline.getErrorMessage()).contains("403 Forbidden");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("8. Rollout status check verifies rollout success and marks pipeline SUCCESS")
    void testGetPipelineStatus_RolloutSuccess() {
        Deployment depEntity = new Deployment(testProject, "main-abc1234", DeploymentStatus.RUNNING);
        depEntity.setId(50L);
        testPipeline.setDeployment(depEntity);
        testPipeline.setStatus(PipelineStatus.ROLLOUT_VERIFYING);
        testPipeline.setStartedAt(OffsetDateTime.now());

        when(pipelineExecutionRepository.findById(100L)).thenReturn(Optional.of(testPipeline));
        when(pipelineExecutionRepository.save(any(PipelineExecution.class))).thenReturn(testPipeline);

        DeploymentResponse depResp = new DeploymentResponse();
        depResp.setId(50L);
        depResp.setStatus(DeploymentStatus.SUCCESS);
        depResp.setRolloutStatus("SUCCESS");
        when(deploymentService.getDeploymentRolloutStatus(50L)).thenReturn(depResp);

        PipelineExecutionResponse response = pipelineService.getPipelineStatus(100L);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
        assertThat(response.getStage()).isEqualTo("LIVE");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("9. Rollout timeout (>180s) transitions pipeline to ROLLOUT_FAILED")
    void testGetPipelineStatus_RolloutTimeout() {
        Deployment depEntity = new Deployment(testProject, "main-abc1234", DeploymentStatus.RUNNING);
        depEntity.setId(50L);
        testPipeline.setDeployment(depEntity);
        testPipeline.setStatus(PipelineStatus.ROLLOUT_VERIFYING);
        // Set startedAt 200 seconds ago
        testPipeline.setStartedAt(OffsetDateTime.now().minusSeconds(200));

        when(pipelineExecutionRepository.findById(100L)).thenReturn(Optional.of(testPipeline));
        when(pipelineExecutionRepository.save(any(PipelineExecution.class))).thenReturn(testPipeline);

        PipelineExecutionResponse response = pipelineService.getPipelineStatus(100L);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.ROLLOUT_FAILED);
        assertThat(response.getErrorMessage()).contains("Rollout verification timed out");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
        verify(deploymentService, never()).getDeploymentRolloutStatus(any());
    }

    @Test
    @DisplayName("10. Rollout failure in Kubernetes transitions pipeline to ROLLOUT_FAILED")
    void testGetPipelineStatus_RolloutFailure() {
        Deployment depEntity = new Deployment(testProject, "main-abc1234", DeploymentStatus.RUNNING);
        depEntity.setId(50L);
        testPipeline.setDeployment(depEntity);
        testPipeline.setStatus(PipelineStatus.ROLLOUT_VERIFYING);
        testPipeline.setStartedAt(OffsetDateTime.now().minusSeconds(30));

        when(pipelineExecutionRepository.findById(100L)).thenReturn(Optional.of(testPipeline));
        when(pipelineExecutionRepository.save(any(PipelineExecution.class))).thenReturn(testPipeline);

        DeploymentResponse depResp = new DeploymentResponse();
        depResp.setId(50L);
        depResp.setStatus(DeploymentStatus.FAILED);
        depResp.setRolloutStatus("FAILED");
        depResp.setErrorMessage("Kubernetes rollout failed: ProgressDeadlineExceeded");
        when(deploymentService.getDeploymentRolloutStatus(50L)).thenReturn(depResp);

        PipelineExecutionResponse response = pipelineService.getPipelineStatus(100L);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.ROLLOUT_FAILED);
        assertThat(response.getErrorMessage()).contains("ProgressDeadlineExceeded");
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("11. Concurrency guard rejects duplicate pipeline trigger when one is already active")
    void testTriggerPipeline_ConcurrentTriggerRejected() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(gitRepositoryRepository.findByProjectId(1L)).thenReturn(Optional.of(testRepo));

        PipelineExecution runningPipeline = new PipelineExecution(testProject, "main", "xyz", CITriggerType.MANUAL);
        runningPipeline.setId(99L);
        runningPipeline.setStatus(PipelineStatus.CI_RUNNING);

        when(pipelineExecutionRepository.findFirstByProjectIdAndStatusInOrderByCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(Optional.of(runningPipeline));

        PipelineTriggerRequest request = new PipelineTriggerRequest("main", "abc1234", "Trigger 2", "Dev");

        assertThatThrownBy(() -> pipelineService.triggerPipeline(1L, request, CITriggerType.MANUAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A pipeline execution is already in progress");

        verify(ciService, never()).triggerBuild(any(), any(), any());
    }

    @Test
    @DisplayName("12. Cancelling an active pipeline transitions to CANCELLED")
    void testCancelPipeline_Success() {
        when(pipelineExecutionRepository.findById(100L)).thenReturn(Optional.of(testPipeline));
        when(pipelineExecutionRepository.save(any(PipelineExecution.class))).thenReturn(testPipeline);

        PipelineExecutionResponse response = pipelineService.cancelPipeline(100L);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.CANCELLED);
        assertThat(testPipeline.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("13. Cancelling an already terminal pipeline throws IllegalStateException")
    void testCancelPipeline_AlreadyTerminal() {
        testPipeline.setStatus(PipelineStatus.SUCCESS);
        when(pipelineExecutionRepository.findById(100L)).thenReturn(Optional.of(testPipeline));

        assertThatThrownBy(() -> pipelineService.cancelPipeline(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel pipeline");
    }

    @Test
    @DisplayName("14. Triggering pipeline for non-existent project throws ResourceNotFoundException")
    void testTriggerPipeline_ProjectNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.triggerPipeline(999L, new PipelineTriggerRequest(), CITriggerType.MANUAL))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
