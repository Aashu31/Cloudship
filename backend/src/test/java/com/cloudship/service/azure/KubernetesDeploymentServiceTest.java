package com.cloudship.service.azure;

import com.cloudship.entity.Deployment;
import com.cloudship.entity.DeploymentStatus;
import com.cloudship.entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubernetesDeploymentServiceTest {

    @Mock
    private KubernetesClientProvider clientProvider;

    private KubernetesDeploymentServiceImpl deploymentService;

    @BeforeEach
    void setUp() {
        deploymentService = new KubernetesDeploymentServiceImpl(clientProvider);
    }

    @Test
    @DisplayName("Returns false and sets FAILED status when Kubernetes APIs are unavailable")
    void testApplyDeployment_ClientUnavailable() {
        when(clientProvider.getAppsV1Api()).thenReturn(null);
        when(clientProvider.getCoreV1Api()).thenReturn(null);
        when(clientProvider.getStatusMessage()).thenReturn("Kubernetes cluster offline");

        Project project = new Project("test-service", "Test service", "https://github.com/org/test-service.git");
        Deployment deployment = new Deployment(project, "1.0.0", DeploymentStatus.PENDING);
        deployment.setImageName("cloudshipcr.azurecr.io/cloudship/test-service");
        deployment.setImageTag("v1");

        boolean result = deploymentService.applyDeployment(deployment);

        assertThat(result).isFalse();
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.FAILED);
        assertThat(deployment.getRolloutStatus()).isEqualTo("FAILED");
        assertThat(deployment.getErrorMessage()).contains("Kubernetes client not available");
        assertThat(deployment.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Handles null deployment gracefully")
    void testApplyDeployment_NullDeployment() {
        boolean result = deploymentService.applyDeployment(null);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("checkRolloutStatus safely skips when AppsV1Api is unavailable")
    void testCheckRolloutStatus_ApiUnavailable() {
        when(clientProvider.getAppsV1Api()).thenReturn(null);

        Project project = new Project("test-service", "Test service", "https://github.com/org/test-service.git");
        Deployment deployment = new Deployment(project, "1.0.0", DeploymentStatus.RUNNING);

        deploymentService.checkRolloutStatus(deployment);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.RUNNING);
    }
}
