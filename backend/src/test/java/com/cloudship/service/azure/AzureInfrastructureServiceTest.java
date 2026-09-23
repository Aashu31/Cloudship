package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerregistry.models.Registries;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.azure.resourcemanager.containerregistry.models.Sku;
import com.azure.resourcemanager.containerregistry.models.SkuTier;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.Networks;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.cloudship.dto.azure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureInfrastructureServiceTest {

    @Mock
    private AzureClientProvider clientProvider;

    @Mock
    private AzureResourceManager azureManager;

    @Mock
    private ResourceGroups resourceGroups;

    @Mock
    private ResourceGroup resourceGroup;

    @Mock
    private Networks networks;

    @Mock
    private Network network;

    @Mock
    private Subnet subnet;

    @Mock
    private Registries registries;

    @Mock
    private Registry registry;

    private AzureInfrastructureService service;

    @BeforeEach
    void setUp() {
        service = new AzureInfrastructureService(clientProvider);
    }

    @Test
    @DisplayName("getAzureStatus should return NOT_CONFIGURED when clientProvider is not configured")
    void getStatusShouldReturnNotConfiguredWhenDisabled() {
        when(clientProvider.isConfigured()).thenReturn(false);
        when(clientProvider.getSubscriptionId()).thenReturn("sub-test");
        when(clientProvider.getTenantId()).thenReturn("tenant-test");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getLocation()).thenReturn("eastus");

        AzureStatusResponse status = service.getAzureStatus();

        assertNotNull(status);
        assertFalse(status.isConfigured());
        assertEquals(AzureConnectionStatus.NOT_CONFIGURED, status.getConnectionStatus());
        assertTrue(status.getMessage().contains("not configured or disabled"));
    }

    @Test
    @DisplayName("getAzureStatus should return CONNECTED when resource group is verified in subscription")
    void getStatusShouldReturnConnectedWhenClientAvailable() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getLocation()).thenReturn("eastus");
        when(clientProvider.getSubscriptionId()).thenReturn("sub-test");
        when(clientProvider.getTenantId()).thenReturn("tenant-test");

        when(azureManager.resourceGroups()).thenReturn(resourceGroups);
        when(resourceGroups.contain("rg-cloudship-dev")).thenReturn(true);

        AzureStatusResponse status = service.getAzureStatus();

        assertNotNull(status);
        assertTrue(status.isConfigured());
        assertEquals(AzureConnectionStatus.CONNECTED, status.getConnectionStatus());
        assertTrue(status.getMessage().contains("Connected to Azure"));
    }

    @Test
    @DisplayName("getResourceGroup should report READY when group exists")
    void getResourceGroupShouldReportReady() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.resourceGroups()).thenReturn(resourceGroups);
        when(resourceGroups.getByName("rg-cloudship-dev")).thenReturn(resourceGroup);
        when(resourceGroup.name()).thenReturn("rg-cloudship-dev");
        when(resourceGroup.regionName()).thenReturn("eastus");
        when(resourceGroup.provisioningState()).thenReturn("Succeeded");
        when(resourceGroup.tags()).thenReturn(Map.of("Environment", "Dev"));

        AzureResourceGroupResponse response = service.getResourceGroup();

        assertNotNull(response);
        assertEquals(AzureResourceStatus.READY, response.getStatus());
        assertEquals("rg-cloudship-dev", response.getName());
        assertEquals("eastus", response.getLocation());
        assertEquals("Succeeded", response.getProvisioningState());
        assertEquals("Dev", response.getTags().get("Environment"));
    }

    @Test
    @DisplayName("getResourceGroup should report NOT_FOUND when group is missing")
    void getResourceGroupShouldReportNotFoundWhenNull() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.resourceGroups()).thenReturn(resourceGroups);
        when(resourceGroups.getByName("rg-cloudship-dev")).thenReturn(null);

        AzureResourceGroupResponse response = service.getResourceGroup();

        assertNotNull(response);
        assertEquals(AzureResourceStatus.NOT_FOUND, response.getStatus());
        assertTrue(response.getMessage().contains("was not found"));
    }

    @Test
    @DisplayName("getNetwork should report READY when VNet and Subnet exist")
    void getNetworkShouldReportReady() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getVnetName()).thenReturn("vnet-cloudship");
        when(clientProvider.getSubnetName()).thenReturn("snet-cloudship");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);

        when(azureManager.networks()).thenReturn(networks);
        when(networks.getByResourceGroup("rg-cloudship-dev", "vnet-cloudship")).thenReturn(network);
        when(network.name()).thenReturn("vnet-cloudship");
        when(network.regionName()).thenReturn("eastus");
        when(network.addressSpaces()).thenReturn(List.of("10.0.0.0/16"));
        when(network.subnets()).thenReturn(Map.of("snet-cloudship", subnet));
        when(subnet.addressPrefix()).thenReturn("10.0.1.0/24");

        AzureNetworkResponse response = service.getNetwork();

        assertNotNull(response);
        assertEquals(AzureResourceStatus.READY, response.getStatus());
        assertEquals(AzureResourceStatus.READY, response.getSubnetStatus());
        assertEquals("vnet-cloudship", response.getVnetName());
        assertEquals("snet-cloudship", response.getSubnetName());
        assertEquals(List.of("10.0.0.0/16"), response.getAddressSpaces());
        assertEquals(List.of("10.0.1.0/24"), response.getSubnetAddressPrefixes());
    }

    @Test
    @DisplayName("getContainerRegistry should report READY when ACR exists")
    void getContainerRegistryShouldReportReady() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);

        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "cloudshipcr")).thenReturn(registry);
        when(registry.name()).thenReturn("cloudshipcr");
        when(registry.loginServerUrl()).thenReturn("cloudshipcr.azurecr.io");
        when(registry.regionName()).thenReturn("eastus");
        Sku sku = mock(Sku.class);
        when(sku.tier()).thenReturn(SkuTier.BASIC);
        when(registry.sku()).thenReturn(sku);
        when(registry.adminUserEnabled()).thenReturn(true);

        AzureRegistryResponse response = service.getContainerRegistry();

        assertNotNull(response);
        assertEquals(AzureResourceStatus.READY, response.getStatus());
        assertEquals("cloudshipcr", response.getName());
        assertEquals("cloudshipcr.azurecr.io", response.getLoginServer());
        assertEquals("Basic", response.getSku());
        assertTrue(response.isAdminUserEnabled());
    }

    @Test
    @DisplayName("getInfrastructureOverview should aggregate all Azure resources")
    void getInfrastructureOverviewShouldAggregateAll() {
        when(clientProvider.isConfigured()).thenReturn(false);
        when(clientProvider.getSubscriptionId()).thenReturn("sub-test");
        when(clientProvider.getTenantId()).thenReturn("tenant-test");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getLocation()).thenReturn("eastus");
        when(clientProvider.getVnetName()).thenReturn("vnet-cloudship");
        when(clientProvider.getSubnetName()).thenReturn("snet-cloudship");
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");

        AzureInfrastructureResponse infra = service.getInfrastructureOverview();

        assertNotNull(infra);
        assertNotNull(infra.getStatus());
        assertNotNull(infra.getResourceGroup());
        assertNotNull(infra.getNetwork());
        assertNotNull(infra.getContainerRegistry());
        assertEquals(AzureConnectionStatus.NOT_CONFIGURED, infra.getStatus().getConnectionStatus());
    }
}
