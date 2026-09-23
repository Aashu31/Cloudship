package com.cloudship.dto.azure;

public class AzureRegistryResponse {

    private String name;
    private String loginServer;
    private String location;
    private String sku;
    private boolean adminUserEnabled;
    private String provisioningState;
    private AzureResourceStatus status;
    private String message;

    public AzureRegistryResponse() {
    }

    public AzureRegistryResponse(String name, String loginServer, String location,
                                 String sku, boolean adminUserEnabled, String provisioningState,
                                 AzureResourceStatus status, String message) {
        this.name = name;
        this.loginServer = loginServer;
        this.location = location;
        this.sku = sku;
        this.adminUserEnabled = adminUserEnabled;
        this.provisioningState = provisioningState;
        this.status = status;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginServer() {
        return loginServer;
    }

    public void setLoginServer(String loginServer) {
        this.loginServer = loginServer;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public boolean isAdminUserEnabled() {
        return adminUserEnabled;
    }

    public void setAdminUserEnabled(boolean adminUserEnabled) {
        this.adminUserEnabled = adminUserEnabled;
    }

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public AzureResourceStatus getStatus() {
        return status;
    }

    public void setStatus(AzureResourceStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
