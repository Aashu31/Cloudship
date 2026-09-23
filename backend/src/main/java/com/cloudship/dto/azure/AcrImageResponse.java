package com.cloudship.dto.azure;

import java.time.OffsetDateTime;

public class AcrImageResponse {

    private String repository;
    private String tag;
    private String digest;
    private String imageReference;
    private String registryName;
    private String loginServer;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdatedAt;
    private AzureResourceStatus status;
    private String message;

    public AcrImageResponse() {
    }

    public AcrImageResponse(String repository, String tag, String digest, String imageReference,
                            String registryName, String loginServer, OffsetDateTime createdAt,
                            OffsetDateTime lastUpdatedAt, AzureResourceStatus status, String message) {
        this.repository = repository;
        this.tag = tag;
        this.digest = digest;
        this.imageReference = imageReference;
        this.registryName = registryName;
        this.loginServer = loginServer;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.status = status;
        this.message = message;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getImageReference() {
        return imageReference;
    }

    public void setImageReference(String imageReference) {
        this.imageReference = imageReference;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getLoginServer() {
        return loginServer;
    }

    public void setLoginServer(String loginServer) {
        this.loginServer = loginServer;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(OffsetDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
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
