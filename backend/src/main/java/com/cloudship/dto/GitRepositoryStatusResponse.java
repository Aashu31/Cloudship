package com.cloudship.dto;

import com.cloudship.entity.GitConnectionStatus;
import java.time.OffsetDateTime;

public class GitRepositoryStatusResponse {

    private Long projectId;
    private String repositoryUrl;
    private String owner;
    private String repositoryName;
    private String defaultBranch;
    private GitConnectionStatus connectionStatus;
    private String message;
    private OffsetDateTime lastVerifiedAt;

    public GitRepositoryStatusResponse() {
    }

    public GitRepositoryStatusResponse(Long projectId, String repositoryUrl, String owner, String repositoryName, String defaultBranch, GitConnectionStatus connectionStatus, String message) {
        this.projectId = projectId;
        this.repositoryUrl = repositoryUrl;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.defaultBranch = defaultBranch;
        this.connectionStatus = connectionStatus;
        this.message = message;
        this.lastVerifiedAt = OffsetDateTime.now();
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(OffsetDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }
}
