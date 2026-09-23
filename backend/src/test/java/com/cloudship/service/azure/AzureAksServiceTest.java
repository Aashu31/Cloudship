package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool;
import com.azure.resourcemanager.containerservice.models.KubernetesClusters;
import com.cloudship.dto.azure.AksClusterResponse;
import com.cloudship.dto.azure.AksClusterStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureAksServiceTest {

    @Mock
    private AzureClientProvider azureClientProvider;

    @Mock
    private KubernetesClientProvider kubernetesClientProvider;

    @Mock
    private AzureResourceManager azureResourceManager;

    @Mock
    private KubernetesClusters kubernetesClusters;

    @Mock
    private KubernetesCluster kubernetesCluster;

    @Mock
    private KubernetesClusterAgentPool agentPool;

    private AzureAksServiceImpl aksService;

    @BeforeEach
    void setUp() {
        aksService = new AzureAksServiceImpl(azureClientProvider, kubernetesClientProvider);
    }

    @Test
    @DisplayName("Returns NOT_CONFIGURED when Azure credentials are not configured")
    void testGetClusterDetails_NotConfigured() {
        when(azureClientProvider.isConfigured()).thenReturn(false);

        AksClusterResponse response = aksService.getClusterDetails();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AksClusterStatus.NOT_CONFIGURED);
        assertThat(response.isConfigured()).isFalse();
        assertThat(response.getMessage()).contains("not configured");
    }

    @Test
    @DisplayName("Returns NOT_CONNECTED when AzureResourceManager is unavailable")
    void testGetClusterDetails_NotConnected() {
        when(azureClientProvider.isConfigured()).thenReturn(true);
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-cloudship-dev");
        when(azureClientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(azureClientProvider.getAzureResourceManager()).thenReturn(null);

        AksClusterResponse response = aksService.getClusterDetails();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AksClusterStatus.NOT_CONNECTED);
        assertThat(response.getName()).isEqualTo("aks-cloudship-dev");
    }

    @Test
    @DisplayName("Returns NOT_FOUND when cluster does not exist in resource group")
    void testGetClusterDetails_NotFound() {
        when(azureClientProvider.isConfigured()).thenReturn(true);
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-missing");
        when(azureClientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(azureClientProvider.getAzureResourceManager()).thenReturn(azureResourceManager);
        when(azureResourceManager.kubernetesClusters()).thenReturn(kubernetesClusters);
        when(kubernetesClusters.getByResourceGroup("rg-cloudship-dev", "aks-missing")).thenReturn(null);

        AksClusterResponse response = aksService.getClusterDetails();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AksClusterStatus.NOT_FOUND);
        assertThat(response.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Returns READY with full metadata when cluster exists and is healthy")
    void testGetClusterDetails_Ready() {
        when(azureClientProvider.isConfigured()).thenReturn(true);
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-cloudship-dev");
        when(azureClientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(azureClientProvider.getAzureResourceManager()).thenReturn(azureResourceManager);
        when(azureResourceManager.kubernetesClusters()).thenReturn(kubernetesClusters);
        when(kubernetesClusters.getByResourceGroup("rg-cloudship-dev", "aks-cloudship-dev")).thenReturn(kubernetesCluster);

        when(kubernetesCluster.name()).thenReturn("aks-cloudship-dev");
        when(kubernetesCluster.resourceGroupName()).thenReturn("rg-cloudship-dev");
        when(kubernetesCluster.nodeResourceGroup()).thenReturn("MC_rg-cloudship-dev_aks-cloudship-dev_eastus");
        when(kubernetesCluster.regionName()).thenReturn("eastus");
        when(kubernetesCluster.version()).thenReturn("1.28.5");
        when(kubernetesCluster.provisioningState()).thenReturn("Succeeded");
        when(kubernetesCluster.fqdn()).thenReturn("aks-cloudship-dev-dns.hcp.eastus.azmk8s.io");
        when(kubernetesCluster.dnsPrefix()).thenReturn("aks-cloudship-dev-dns");

        when(agentPool.count()).thenReturn(3);
        when(kubernetesCluster.agentPools()).thenReturn(Map.of("nodepool1", agentPool));

        AksClusterResponse response = aksService.getClusterDetails();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AksClusterStatus.READY);
        assertThat(response.isConfigured()).isTrue();
        assertThat(response.getName()).isEqualTo("aks-cloudship-dev");
        assertThat(response.getKubernetesVersion()).isEqualTo("1.28.5");
        assertThat(response.getNodeResourceGroup()).isEqualTo("MC_rg-cloudship-dev_aks-cloudship-dev_eastus");
        assertThat(response.getAgentPoolCount()).isEqualTo(1);
        assertThat(response.getTotalNodes()).isEqualTo(3);
        assertThat(response.getFqdn()).isEqualTo("aks-cloudship-dev-dns.hcp.eastus.azmk8s.io");
    }

    @Test
    @DisplayName("Returns ERROR status gracefully when Azure API throws exception")
    void testGetClusterDetails_ExceptionHandled() {
        when(azureClientProvider.isConfigured()).thenReturn(true);
        when(azureClientProvider.getAksClusterName()).thenReturn("aks-cloudship-dev");
        when(azureClientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(azureClientProvider.getAzureResourceManager()).thenThrow(new RuntimeException("Azure network timeout"));

        AksClusterResponse response = aksService.getClusterDetails();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AksClusterStatus.ERROR);
        assertThat(response.getMessage()).contains("Azure network timeout");
    }

    @Test
    @DisplayName("Returns empty workloads list when Kubernetes AppsV1Api is unavailable")
    void testListWorkloads_AppsApiNull() {
        when(azureClientProvider.getK8sNamespace()).thenReturn("default");
        when(kubernetesClientProvider.getAppsV1Api()).thenReturn(null);

        var workloads = aksService.listWorkloads(null);
        assertThat(workloads).isEmpty();
    }

    @Test
    @DisplayName("Returns empty pods list when Kubernetes CoreV1Api is unavailable")
    void testListPods_CoreApiNull() {
        when(azureClientProvider.getK8sNamespace()).thenReturn("default");
        when(kubernetesClientProvider.getCoreV1Api()).thenReturn(null);

        var pods = aksService.listPods(null, null);
        assertThat(pods).isEmpty();
    }

    @Test
    @DisplayName("Returns empty services list when Kubernetes CoreV1Api is unavailable")
    void testListServices_CoreApiNull() {
        when(azureClientProvider.getK8sNamespace()).thenReturn("default");
        when(kubernetesClientProvider.getCoreV1Api()).thenReturn(null);

        var services = aksService.listServices(null);
        assertThat(services).isEmpty();
    }
}
