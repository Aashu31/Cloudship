package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;

public interface AzureClientProvider {

    boolean isConfigured();

    AzureResourceManager getAzureResourceManager();

    String getSubscriptionId();

    String getTenantId();

    String getResourceGroupName();

    String getLocation();

    String getVnetName();

    String getSubnetName();

    String getAcrName();
    String getAcrLoginServer();
    String getAcrRepositoryPrefix();
    String resolveAcrLoginServer();

    // Version 6: AKS & Kubernetes
    String getAksClusterName();
    String getAksNodeResourceGroup();
    String getK8sNamespace();
    String getK8sKubeconfigPath();
    String getK8sServiceType();
}
