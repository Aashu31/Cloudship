package com.cloudship.dto.azure;

public class AksClusterResponse {

    private String name;
    private String resourceGroup;
    private String nodeResourceGroup;
    private String location;
    private String kubernetesVersion;
    private String provisioningState;
    private String powerState;
    private Integer agentPoolCount;
    private Integer totalNodes;
    private String fqdn;
    private String dnsPrefix;
    private AksClusterStatus status;
    private boolean configured;
    private String message;

    public AksClusterResponse() {
    }

    public static AksClusterResponse notConfigured(String message) {
        AksClusterResponse resp = new AksClusterResponse();
        resp.setStatus(AksClusterStatus.NOT_CONFIGURED);
        resp.setConfigured(false);
        resp.setMessage(message != null ? message : "Azure Kubernetes Service integration is not configured or disabled.");
        return resp;
    }

    public static AksClusterResponse notConnected(String clusterName, String message) {
        AksClusterResponse resp = new AksClusterResponse();
        resp.setName(clusterName);
        resp.setStatus(AksClusterStatus.NOT_CONNECTED);
        resp.setConfigured(true);
        resp.setMessage(message != null ? message : "Azure client authenticated but unable to connect to AKS cluster API.");
        return resp;
    }

    public static AksClusterResponse notFound(String clusterName, String resourceGroup) {
        AksClusterResponse resp = new AksClusterResponse();
        resp.setName(clusterName);
        resp.setResourceGroup(resourceGroup);
        resp.setStatus(AksClusterStatus.NOT_FOUND);
        resp.setConfigured(true);
        resp.setMessage(String.format("AKS cluster '%s' not found in resource group '%s'.", clusterName, resourceGroup));
        return resp;
    }

    public static AksClusterResponse error(String clusterName, String message) {
        AksClusterResponse resp = new AksClusterResponse();
        resp.setName(clusterName);
        resp.setStatus(AksClusterStatus.ERROR);
        resp.setConfigured(true);
        resp.setMessage(message != null ? message : "An error occurred while inspecting AKS cluster.");
        return resp;
    }

    public static AksClusterResponse ready(
            String name,
            String resourceGroup,
            String nodeResourceGroup,
            String location,
            String kubernetesVersion,
            String provisioningState,
            String powerState,
            Integer agentPoolCount,
            Integer totalNodes,
            String fqdn,
            String dnsPrefix) {
        AksClusterResponse resp = new AksClusterResponse();
        resp.setName(name);
        resp.setResourceGroup(resourceGroup);
        resp.setNodeResourceGroup(nodeResourceGroup);
        resp.setLocation(location);
        resp.setKubernetesVersion(kubernetesVersion);
        resp.setProvisioningState(provisioningState);
        resp.setPowerState(powerState);
        resp.setAgentPoolCount(agentPoolCount != null ? agentPoolCount : 0);
        resp.setTotalNodes(totalNodes != null ? totalNodes : 0);
        resp.setFqdn(fqdn);
        resp.setDnsPrefix(dnsPrefix);
        resp.setStatus(AksClusterStatus.READY);
        resp.setConfigured(true);
        resp.setMessage("AKS cluster is online and reachable.");
        return resp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getNodeResourceGroup() {
        return nodeResourceGroup;
    }

    public void setNodeResourceGroup(String nodeResourceGroup) {
        this.nodeResourceGroup = nodeResourceGroup;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
    }

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public String getPowerState() {
        return powerState;
    }

    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }

    public Integer getAgentPoolCount() {
        return agentPoolCount;
    }

    public void setAgentPoolCount(Integer agentPoolCount) {
        this.agentPoolCount = agentPoolCount;
    }

    public Integer getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(Integer totalNodes) {
        this.totalNodes = totalNodes;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getDnsPrefix() {
        return dnsPrefix;
    }

    public void setDnsPrefix(String dnsPrefix) {
        this.dnsPrefix = dnsPrefix;
    }

    public AksClusterStatus getStatus() {
        return status;
    }

    public void setStatus(AksClusterStatus status) {
        this.status = status;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
