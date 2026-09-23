package com.cloudship.dto.azure;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class AcrRepositoryResponse {

    private String registryName;
    private String loginServer;
    private List<String> repositories;
    private int repositoryCount;
    private AzureResourceStatus status;
    private String message;
    private OffsetDateTime queriedAt;

    public AcrRepositoryResponse() {
    }

    public AcrRepositoryResponse(String registryName, String loginServer, List<String> repositories,
                                 AzureResourceStatus status, String message) {
        this.registryName = registryName;
        this.loginServer = loginServer;
        this.repositories = repositories != null ? repositories : Collections.emptyList();
        this.repositoryCount = this.repositories.size();
        this.status = status;
        this.message = message;
        this.queriedAt = OffsetDateTime.now();
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

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories != null ? repositories : Collections.emptyList();
        this.repositoryCount = this.repositories.size();
    }

    public int getRepositoryCount() {
        return repositoryCount;
    }

    public void setRepositoryCount(int repositoryCount) {
        this.repositoryCount = repositoryCount;
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

    public OffsetDateTime getQueriedAt() {
        return queriedAt;
    }

    public void setQueriedAt(OffsetDateTime queriedAt) {
        this.queriedAt = queriedAt;
    }
}
