package com.cloudship.dto;

import com.cloudship.entity.GitConnectionStatus;
import com.cloudship.entity.GitProvider;
import com.cloudship.entity.GitRepository;

import java.time.OffsetDateTime;

public class GitRepositoryResponse {

    private Long id;
    private Long projectId;
    private GitProvider provider;
    private String repositoryUrl;
    private String owner;
    private String repositoryName;
    private String defaultBranch;
    private GitConnectionStatus connectionStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public GitRepositoryResponse() {
    }

    public static GitRepositoryResponse fromEntity(GitRepository entity) {
        GitRepositoryResponse response = new GitRepositoryResponse();
        response.setId(entity.getId());
        response.setProjectId(entity.getProject() != null ? entity.getProject().getId() : null);
        response.setProvider(entity.getProvider());
        response.setRepositoryUrl(entity.getRepositoryUrl());
        response.setOwner(entity.getOwner());
        response.setRepositoryName(entity.getRepositoryName());
        response.setDefaultBranch(entity.getDefaultBranch());
        response.setConnectionStatus(entity.getConnectionStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public GitProvider getProvider() {
        return provider;
    }

    public void setProvider(GitProvider provider) {
        this.provider = provider;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public GitConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(GitConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
