package com.cloudship.service.azure;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.cloudship.dto.azure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AzureInfrastructureService {

    private static final Logger log = LoggerFactory.getLogger(AzureInfrastructureService.class);

    private final AzureClientProvider clientProvider;

    public AzureInfrastructureService(AzureClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public AzureStatusResponse getAzureStatus() {
        if (!clientProvider.isConfigured()) {
            return new AzureStatusResponse(
                    false,
                    AzureConnectionStatus.NOT_CONFIGURED,
                    clientProvider.getSubscriptionId(),
                    clientProvider.getTenantId(),
                    clientProvider.getResourceGroupName(),
                    clientProvider.getLocation(),
                    "Azure integration is not configured or disabled in application properties",
                    OffsetDateTime.now()
            );
        }

        try {
            AzureResourceManager manager = clientProvider.getAzureResourceManager();
            if (manager == null) {
                return new AzureStatusResponse(
                        true,
                        AzureConnectionStatus.NOT_CONNECTED,
                        clientProvider.getSubscriptionId(),
                        clientProvider.getTenantId(),
                        clientProvider.getResourceGroupName(),
                        clientProvider.getLocation(),
                        "Could not authenticate with Azure credentials",
                        OffsetDateTime.now()
                );
            }

            // Probe subscription existence
            boolean exists = manager.resourceGroups().contain(clientProvider.getResourceGroupName());
            return new AzureStatusResponse(
                    true,
                    AzureConnectionStatus.CONNECTED,
                    clientProvider.getSubscriptionId(),
                    clientProvider.getTenantId(),
                    clientProvider.getResourceGroupName(),
                    clientProvider.getLocation(),
                    exists ? "Connected to Azure; target resource group verified" : "Connected to Azure; target resource group not found",
                    OffsetDateTime.now()
            );
        } catch (ManagementException e) {
            log.warn("Azure Management API returned error during status probe: {}", e.getMessage());
            return new AzureStatusResponse(
                    true,
                    AzureConnectionStatus.ERROR,
                    clientProvider.getSubscriptionId(),
                    clientProvider.getTenantId(),
                    clientProvider.getResourceGroupName(),
                    clientProvider.getLocation(),
                    "Azure Management error: " + (e.getValue() != null ? e.getValue().getMessage() : e.getMessage()),
                    OffsetDateTime.now()
            );
        } catch (Exception e) {
            log.warn("Unexpected error probing Azure connection: {}", e.getMessage());
            return new AzureStatusResponse(
                    true,
                    AzureConnectionStatus.ERROR,
                    clientProvider.getSubscriptionId(),
                    clientProvider.getTenantId(),
                    clientProvider.getResourceGroupName(),
                    clientProvider.getLocation(),
                    "Azure connection failed: " + e.getMessage(),
                    OffsetDateTime.now()
            );
        }
    }

    public AzureResourceGroupResponse getResourceGroup() {
        String rgName = clientProvider.getResourceGroupName();
        if (!clientProvider.isConfigured()) {
            return new AzureResourceGroupResponse(
                    rgName,
                    clientProvider.getLocation(),
                    null,
                    AzureResourceStatus.NOT_CONFIGURED,
                    "Azure is not configured",
                    Collections.emptyMap()
            );
        }

        try {
            AzureResourceManager manager = clientProvider.getAzureResourceManager();
            if (manager == null) {
                return new AzureResourceGroupResponse(
                        rgName,
                        clientProvider.getLocation(),
                        null,
                        AzureResourceStatus.ERROR,
                        "Azure client could not be initialized",
                        Collections.emptyMap()
                );
            }

            ResourceGroup rg = manager.resourceGroups().getByName(rgName);
            if (rg == null) {
                return new AzureResourceGroupResponse(
                        rgName,
                        clientProvider.getLocation(),
                        null,
                        AzureResourceStatus.NOT_FOUND,
                        "Resource Group '" + rgName + "' was not found in subscription",
                        Collections.emptyMap()
                );
            }

            return new AzureResourceGroupResponse(
                    rg.name(),
                    rg.regionName(),
                    rg.provisioningState(),
                    AzureResourceStatus.READY,
                    "Resource Group verified successfully",
                    rg.tags()
            );
        } catch (ManagementException e) {
            log.warn("Azure API error inspecting resource group {}: {}", rgName, e.getMessage());
            if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
                return new AzureResourceGroupResponse(
                        rgName, clientProvider.getLocation(), null,
                        AzureResourceStatus.NOT_FOUND, "Resource group not found", Collections.emptyMap()
                );
            }
            return new AzureResourceGroupResponse(
                    rgName, clientProvider.getLocation(), null,
                    AzureResourceStatus.ERROR, "Azure API error: " + e.getMessage(), Collections.emptyMap()
            );
        } catch (Exception e) {
            log.warn("Error inspecting resource group {}: {}", rgName, e.getMessage());
            return new AzureResourceGroupResponse(
                    rgName, clientProvider.getLocation(), null,
                    AzureResourceStatus.ERROR, "Failed to inspect resource group: " + e.getMessage(), Collections.emptyMap()
            );
        }
    }

    public AzureNetworkResponse getNetwork() {
        String vnetName = clientProvider.getVnetName();
        String subnetName = clientProvider.getSubnetName();
        String rgName = clientProvider.getResourceGroupName();

        if (!clientProvider.isConfigured()) {
            return new AzureNetworkResponse(
                    vnetName, subnetName, clientProvider.getLocation(), null,
                    Collections.emptyList(), Collections.emptyList(),
                    AzureResourceStatus.NOT_CONFIGURED, AzureResourceStatus.NOT_CONFIGURED,
                    "Azure is not configured"
            );
        }

        try {
            AzureResourceManager manager = clientProvider.getAzureResourceManager();
            if (manager == null) {
                return new AzureNetworkResponse(
                        vnetName, subnetName, clientProvider.getLocation(), null,
                        Collections.emptyList(), Collections.emptyList(),
                        AzureResourceStatus.ERROR, AzureResourceStatus.ERROR,
                        "Azure client not initialized"
                );
            }

            Network network = manager.networks().getByResourceGroup(rgName, vnetName);
            if (network == null) {
                return new AzureNetworkResponse(
                        vnetName, subnetName, clientProvider.getLocation(), null,
                        Collections.emptyList(), Collections.emptyList(),
                        AzureResourceStatus.NOT_FOUND, AzureResourceStatus.NOT_FOUND,
                        "Virtual Network '" + vnetName + "' not found in resource group '" + rgName + "'"
                );
            }

            List<String> addressSpaces = new ArrayList<>(network.addressSpaces());
            Subnet subnet = network.subnets().get(subnetName);
            AzureResourceStatus subnetStatus = (subnet != null) ? AzureResourceStatus.READY : AzureResourceStatus.NOT_FOUND;
            List<String> subnetPrefixes = (subnet != null && subnet.addressPrefix() != null)
                    ? List.of(subnet.addressPrefix())
                    : Collections.emptyList();

            return new AzureNetworkResponse(
                    network.name(),
                    subnetName,
                    network.regionName(),
                    null,
                    addressSpaces,
                    subnetPrefixes,
                    AzureResourceStatus.READY,
                    subnetStatus,
                    subnet != null ? "VNet and Subnet verified successfully" : "VNet verified; Subnet '" + subnetName + "' not found"
            );
        } catch (ManagementException e) {
            log.warn("Azure API error inspecting network {}: {}", vnetName, e.getMessage());
            if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
                return new AzureNetworkResponse(
                        vnetName, subnetName, clientProvider.getLocation(), null,
                        Collections.emptyList(), Collections.emptyList(),
                        AzureResourceStatus.NOT_FOUND, AzureResourceStatus.NOT_FOUND,
                        "Network resource not found"
                );
            }
            return new AzureNetworkResponse(
                    vnetName, subnetName, clientProvider.getLocation(), null,
                    Collections.emptyList(), Collections.emptyList(),
                    AzureResourceStatus.ERROR, AzureResourceStatus.ERROR,
                    "Azure API error: " + e.getMessage()
            );
        } catch (Exception e) {
            log.warn("Error inspecting network {}: {}", vnetName, e.getMessage());
            return new AzureNetworkResponse(
                    vnetName, subnetName, clientProvider.getLocation(), null,
                    Collections.emptyList(), Collections.emptyList(),
                    AzureResourceStatus.ERROR, AzureResourceStatus.ERROR,
                    "Failed to inspect network: " + e.getMessage()
            );
        }
    }

    public AzureRegistryResponse getContainerRegistry() {
        String acrName = clientProvider.getAcrName();
        String rgName = clientProvider.getResourceGroupName();

        if (!clientProvider.isConfigured()) {
            return new AzureRegistryResponse(
                    acrName, null, clientProvider.getLocation(), null, false, null,
                    AzureResourceStatus.NOT_CONFIGURED, "Azure is not configured"
            );
        }

        try {
            AzureResourceManager manager = clientProvider.getAzureResourceManager();
            if (manager == null) {
                return new AzureRegistryResponse(
                        acrName, null, clientProvider.getLocation(), null, false, null,
                        AzureResourceStatus.ERROR, "Azure client not initialized"
                );
            }

            Registry registry = manager.containerRegistries().getByResourceGroup(rgName, acrName);
            if (registry == null) {
                return new AzureRegistryResponse(
                        acrName, null, clientProvider.getLocation(), null, false, null,
                        AzureResourceStatus.NOT_FOUND, "Azure Container Registry '" + acrName + "' not found in resource group '" + rgName + "'"
                );
            }

            String provState = (registry.innerModel() != null && registry.innerModel().provisioningState() != null)
                    ? registry.innerModel().provisioningState().toString()
                    : "Succeeded";

            return new AzureRegistryResponse(
                    registry.name(),
                    registry.loginServerUrl(),
                    registry.regionName(),
                    registry.sku() != null ? registry.sku().tier().toString() : "UNKNOWN",
                    registry.adminUserEnabled(),
                    provState,
                    AzureResourceStatus.READY,
                    "Azure Container Registry verified successfully (Version 5 ACR integration active)"
            );
        } catch (ManagementException e) {
            log.warn("Azure API error inspecting container registry {}: {}", acrName, e.getMessage());
            if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
                return new AzureRegistryResponse(
                        acrName, null, clientProvider.getLocation(), null, false, null,
                        AzureResourceStatus.NOT_FOUND, "Container registry not found"
                );
            }
            return new AzureRegistryResponse(
                    acrName, null, clientProvider.getLocation(), null, false, null,
                    AzureResourceStatus.ERROR, "Azure API error: " + e.getMessage()
            );
        } catch (Exception e) {
            log.warn("Error inspecting container registry {}: {}", acrName, e.getMessage());
            return new AzureRegistryResponse(
                    acrName, null, clientProvider.getLocation(), null, false, null,
                    AzureResourceStatus.ERROR, "Failed to inspect container registry: " + e.getMessage()
            );
        }
    }

    public AzureInfrastructureResponse getInfrastructureOverview() {
        AzureStatusResponse status = getAzureStatus();
        AzureResourceGroupResponse rg = getResourceGroup();
        AzureNetworkResponse network = getNetwork();
        AzureRegistryResponse registry = getContainerRegistry();

        return new AzureInfrastructureResponse(status, rg, network, registry);
    }
}
