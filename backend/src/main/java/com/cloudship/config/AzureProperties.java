package com.cloudship.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudship.azure")
public class AzureProperties {

    private boolean enabled = false;
    private String subscriptionId = "";
    private String tenantId = "";
    private String clientId = "";
    private String clientSecret = "";
    private String resourceGroup = "rg-cloudship-dev";
    private String location = "eastus";
    private String vnetName = "vnet-cloudship";
    private String subnetName = "snet-cloudship";
    private String acrName = "cloudshipcr";
    private String acrLoginServer = "";
    private String acrRepositoryPrefix = "cloudship";

    // Version 6: Azure Kubernetes Service (AKS) / Kubernetes configuration
    private String aksClusterName = "aks-cloudship-dev";
    private String aksNodeResourceGroup = "";
    private String k8sNamespace = "default";
    private String k8sKubeconfigPath = "";
    private String k8sServiceType = "ClusterIP";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVnetName() {
        return vnetName;
    }

    public void setVnetName(String vnetName) {
        this.vnetName = vnetName;
    }

    public String getSubnetName() {
        return subnetName;
    }

    public void setSubnetName(String subnetName) {
        this.subnetName = subnetName;
    }

    public String getAcrName() {
        return acrName;
    }

    public void setAcrName(String acrName) {
        this.acrName = acrName;
    }

    public String getAcrLoginServer() {
        return acrLoginServer;
    }

    public void setAcrLoginServer(String acrLoginServer) {
        this.acrLoginServer = acrLoginServer;
    }

    public String getAcrRepositoryPrefix() {
        return acrRepositoryPrefix;
    }

    public void setAcrRepositoryPrefix(String acrRepositoryPrefix) {
        this.acrRepositoryPrefix = acrRepositoryPrefix;
    }

    public String getAksClusterName() {
        return aksClusterName;
    }

    public void setAksClusterName(String aksClusterName) {
        this.aksClusterName = aksClusterName;
    }

    public String getAksNodeResourceGroup() {
        return aksNodeResourceGroup;
    }

    public void setAksNodeResourceGroup(String aksNodeResourceGroup) {
        this.aksNodeResourceGroup = aksNodeResourceGroup;
    }

    public String getK8sNamespace() {
        return k8sNamespace;
    }

    public void setK8sNamespace(String k8sNamespace) {
        this.k8sNamespace = k8sNamespace;
    }

    public String getK8sKubeconfigPath() {
        return k8sKubeconfigPath;
    }

    public void setK8sKubeconfigPath(String k8sKubeconfigPath) {
        this.k8sKubeconfigPath = k8sKubeconfigPath;
    }

    public String getK8sServiceType() {
        return k8sServiceType;
    }

    public void setK8sServiceType(String k8sServiceType) {
        this.k8sServiceType = k8sServiceType;
    }

    public String resolveAcrLoginServer() {
        if (acrLoginServer != null && !acrLoginServer.trim().isEmpty()) {
            return acrLoginServer.trim();
        }
        if (acrName != null && !acrName.trim().isEmpty()) {
            return acrName.trim() + ".azurecr.io";
        }
        return "";
    }

    public boolean hasCredentials() {
        return isNotBlank(subscriptionId) && isNotBlank(tenantId) && isNotBlank(clientId) && isNotBlank(clientSecret);
    }

    public boolean hasAksConfigured() {
        return isNotBlank(aksClusterName);
    }

    private boolean isNotBlank(String val) {
        return val != null && !val.trim().isEmpty() && !val.trim().startsWith("00000000-") && !val.trim().startsWith("change_me");
    }
}
