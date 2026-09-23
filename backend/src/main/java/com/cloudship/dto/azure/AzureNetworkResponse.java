package com.cloudship.dto.azure;

import java.util.ArrayList;
import java.util.List;

public class AzureNetworkResponse {

    private String vnetName;
    private String subnetName;
    private String location;
    private String provisioningState;
    private List<String> addressSpaces = new ArrayList<>();
    private List<String> subnetAddressPrefixes = new ArrayList<>();
    private AzureResourceStatus status;
    private AzureResourceStatus subnetStatus;
    private String message;

    public AzureNetworkResponse() {
    }

    public AzureNetworkResponse(String vnetName, String subnetName, String location,
                                String provisioningState, List<String> addressSpaces,
                                List<String> subnetAddressPrefixes, AzureResourceStatus status,
                                AzureResourceStatus subnetStatus, String message) {
        this.vnetName = vnetName;
        this.subnetName = subnetName;
        this.location = location;
        this.provisioningState = provisioningState;
        if (addressSpaces != null) {
            this.addressSpaces = addressSpaces;
        }
        if (subnetAddressPrefixes != null) {
            this.subnetAddressPrefixes = subnetAddressPrefixes;
        }
        this.status = status;
        this.subnetStatus = subnetStatus;
        this.message = message;
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

    public List<String> getAddressSpaces() {
        return addressSpaces;
    }

    public void setAddressSpaces(List<String> addressSpaces) {
        this.addressSpaces = addressSpaces;
    }

    public List<String> getSubnetAddressPrefixes() {
        return subnetAddressPrefixes;
    }

    public void setSubnetAddressPrefixes(List<String> subnetAddressPrefixes) {
        this.subnetAddressPrefixes = subnetAddressPrefixes;
    }

    public AzureResourceStatus getStatus() {
        return status;
    }

    public void setStatus(AzureResourceStatus status) {
        this.status = status;
    }

    public AzureResourceStatus getSubnetStatus() {
        return subnetStatus;
    }

    public void setSubnetStatus(AzureResourceStatus subnetStatus) {
        this.subnetStatus = subnetStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
