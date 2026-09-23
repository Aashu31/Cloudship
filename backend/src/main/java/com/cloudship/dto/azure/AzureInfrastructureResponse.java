package com.cloudship.dto.azure;

public class AzureInfrastructureResponse {

    private AzureStatusResponse status;
    private AzureResourceGroupResponse resourceGroup;
    private AzureNetworkResponse network;
    private AzureRegistryResponse containerRegistry;

    public AzureInfrastructureResponse() {
    }

    public AzureInfrastructureResponse(AzureStatusResponse status,
                                       AzureResourceGroupResponse resourceGroup,
                                       AzureNetworkResponse network,
                                       AzureRegistryResponse containerRegistry) {
        this.status = status;
        this.resourceGroup = resourceGroup;
        this.network = network;
        this.containerRegistry = containerRegistry;
    }

    public AzureStatusResponse getStatus() {
        return status;
    }

    public void setStatus(AzureStatusResponse status) {
        this.status = status;
    }

    public AzureResourceGroupResponse getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(AzureResourceGroupResponse resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public AzureNetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(AzureNetworkResponse network) {
        this.network = network;
    }

    public AzureRegistryResponse getContainerRegistry() {
        return containerRegistry;
    }

    public void setContainerRegistry(AzureRegistryResponse containerRegistry) {
        this.containerRegistry = containerRegistry;
    }
}
