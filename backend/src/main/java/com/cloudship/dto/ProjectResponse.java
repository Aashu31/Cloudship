package com.cloudship.dto;

import com.cloudship.entity.Project;
import java.time.OffsetDateTime;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String repositoryUrl;
    private long deploymentsCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String name, String description, String repositoryUrl,
                           long deploymentsCount, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.repositoryUrl = repositoryUrl;
        this.deploymentsCount = deploymentsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProjectResponse fromEntity(Project project, long deploymentsCount) {
        if (project == null) {
            return null;
        }
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getRepositoryUrl(),
                deploymentsCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public long getDeploymentsCount() {
        return deploymentsCount;
    }

    public void setDeploymentsCount(long deploymentsCount) {
        this.deploymentsCount = deploymentsCount;
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
