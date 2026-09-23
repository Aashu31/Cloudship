package com.cloudship.service;

import com.cloudship.dto.DeploymentRequest;
import com.cloudship.dto.DeploymentResponse;
import com.cloudship.entity.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.azure.AzureClientProvider;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.KubernetesDeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceV6Test {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CIBuildRepository ciBuildRepository;

    @Mock
    private AzureClientProvider azureClientProvider;

    @Mock
    private AzureContainerRegistryService acrService;

    @Mock
    private KubernetesDeploymentService kubernetesDeploymentService;

    private DeploymentService deploymentService;

    private Project testProject;
    private CIBuild successfulBuild;

    @BeforeEach
    void setUp() {
        deploymentService = new DeploymentService(
                deploymentRepository,
                projectRepository,
                ciBuildRepository,
                azureClientProvider,
                acrService,
                kubernetesDeploymentService
        );

        testProject = new Project("auth-service", "Authentication service", "https://github.com/org/auth-service.git");
        testProject.setId(10L);

        successfulBuild = new CIBuild();
        successfulBuild.setId(101L);
        successfulBuild.setProject(testProject);
        successfulBuild.setStatus(CIBuildStatus.SUCCESS);
        successfulBuild.setPushStatus(CIPushStatus.SUCCESS);
        successfulBuild.setDockerImageName("cloudshipcr.azurecr.io/cloudship/auth-service");
        successfulBuild.setDockerImageTag("build-101");
        successfulBuild.setImageDigest("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
    }

    @Test
    @DisplayName("Successfully triggers rolling Kubernetes deployment using verified CIBuild ID")
    void testTriggerDeployment_WithVerifiedCiBuild() {
        DeploymentRequest request = new DeploymentRequest(10L, 101L, 2);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(ciBuildRepository.findByIdAndProjectId(101L, 10L)).thenReturn(Optional.of(successfulBuild));
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-cloudship-dev");
        when(azureClientProvider.getK8sNamespace()).thenReturn("production");

        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(invocation -> {
            Deployment d = invocation.getArgument(0);
            if (d.getId() == null) {
                d.setId(500L);
            }
            return d;
        });

        DeploymentResponse response = deploymentService.triggerDeployment(request);

        assertThat(response).isNotNull();
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getCiBuildId()).isEqualTo(101L);
        assertThat(response.getClusterName()).isEqualTo("aks-cloudship-dev");
        assertThat(response.getNamespace()).isEqualTo("production");
        assertThat(response.getDeploymentName()).isEqualTo("auth-service");
        assertThat(response.getServiceName()).isEqualTo("auth-service-service");
        assertThat(response.getImageName()).isEqualTo("cloudshipcr.azurecr.io/cloudship/auth-service");
        assertThat(response.getImageTag()).isEqualTo("build-101");
        assertThat(response.getImageDigest()).isEqualTo("sha256:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        assertThat(response.getReplicas()).isEqualTo(2);

        verify(kubernetesDeploymentService).applyDeployment(any(Deployment.class));
    }

    @Test
    @DisplayName("Rejects deployment when CI build push status is not SUCCESS")
    void testTriggerDeployment_UnverifiedBuildRejected() {
        CIBuild unpushedBuild = new CIBuild();
        unpushedBuild.setId(102L);
        unpushedBuild.setProject(testProject);
        unpushedBuild.setPushStatus(CIPushStatus.FAILED);

        DeploymentRequest request = new DeploymentRequest(10L, 102L, 1);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(ciBuildRepository.findByIdAndProjectId(102L, 10L)).thenReturn(Optional.of(unpushedBuild));

        assertThatThrownBy(() -> deploymentService.triggerDeployment(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only images verified and pushed to the registry can be deployed");

        verifyNoInteractions(kubernetesDeploymentService);
    }

    @Test
    @DisplayName("Auto-resolves latest successfully pushed build when ciBuildId is omitted")
    void testTriggerDeployment_AutoResolvesLatestPushedBuild() {
        DeploymentRequest request = new DeploymentRequest(10L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(ciBuildRepository.findFirstByProjectIdAndPushStatusOrderByCreatedAtDesc(10L, CIPushStatus.SUCCESS))
                .thenReturn(Optional.of(successfulBuild));
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-cloudship-dev");
        when(azureClientProvider.getK8sNamespace()).thenReturn("default");

        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentResponse response = deploymentService.triggerDeployment(request);

        assertThat(response).isNotNull();
        assertThat(response.getCiBuildId()).isEqualTo(101L);
        assertThat(response.getImageName()).isEqualTo("cloudshipcr.azurecr.io/cloudship/auth-service");
        verify(kubernetesDeploymentService).applyDeployment(any(Deployment.class));
    }

    @Test
    @DisplayName("Throws IllegalStateException when project has no verified container builds")
    void testTriggerDeployment_NoPushedBuildsFound() {
        DeploymentRequest request = new DeploymentRequest(10L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(testProject));
        when(ciBuildRepository.findFirstByProjectIdAndPushStatusOrderByCreatedAtDesc(10L, CIPushStatus.SUCCESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deploymentService.triggerDeployment(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No verified container image found in registry");

        verifyNoInteractions(kubernetesDeploymentService);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when project does not exist")
    void testTriggerDeployment_ProjectNotFound() {
        DeploymentRequest request = new DeploymentRequest(999L);
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deploymentService.triggerDeployment(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getDeploymentRolloutStatus checks rollout status when running")
    void testGetDeploymentRolloutStatus_Running() {
        Deployment deployment = new Deployment(testProject, "v1", DeploymentStatus.RUNNING);
        deployment.setId(42L);
        deployment.setRolloutStatus("RUNNING");

        when(deploymentRepository.findById(42L)).thenReturn(Optional.of(deployment));
        when(deploymentRepository.save(any(Deployment.class))).thenReturn(deployment);

        DeploymentResponse response = deploymentService.getDeploymentRolloutStatus(42L);

        assertThat(response).isNotNull();
        verify(kubernetesDeploymentService).checkRolloutStatus(deployment);
        verify(deploymentRepository).save(deployment);
    }
}
