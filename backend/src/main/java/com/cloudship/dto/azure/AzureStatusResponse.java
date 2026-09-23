package com.cloudship.dto.azure;

import java.time.OffsetDateTime;

public class AzureStatusResponse {

    private boolean configured;
    private AzureConnectionStatus connectionStatus;
    private String subscriptionId;
    private String tenantId;
    private String resourceGroup;
    private String location;
    private String message;
    private OffsetDateTime lastCheckedAt;

    public AzureStatusResponse() {
    }

    public AzureStatusResponse(boolean configured, AzureConnectionStatus connectionStatus,
                               String subscriptionId, String tenantId, String resourceGroup,
                               String location, String message, OffsetDateTime lastCheckedAt) {
        this.configured = configured;
        this.connectionStatus = connectionStatus;
        this.subscriptionId = subscriptionId;
        this.tenantId = tenantId;
        this.resourceGroup = resourceGroup;
        this.location = location;
        this.message = message;
        this.lastCheckedAt = lastCheckedAt;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public AzureConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(AzureConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
