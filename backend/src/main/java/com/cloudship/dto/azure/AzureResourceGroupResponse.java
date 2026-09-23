package com.cloudship.dto.azure;

import java.util.HashMap;
import java.util.Map;

public class AzureResourceGroupResponse {

    private String name;
    private String location;
    private String provisioningState;
    private AzureResourceStatus status;
    private String message;
    private Map<String, String> tags = new HashMap<>();

    public AzureResourceGroupResponse() {
    }

    public AzureResourceGroupResponse(String name, String location, String provisioningState,
                                      AzureResourceStatus status, String message, Map<String, String> tags) {
        this.name = name;
        this.location = location;
        this.provisioningState = provisioningState;
        this.status = status;
        this.message = message;
        if (tags != null) {
            this.tags = tags;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
