package com.cloudship.dto;

import com.cloudship.entity.Deployment;
import com.cloudship.entity.DeploymentStatus;
import java.time.OffsetDateTime;

public class DeploymentResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String version;
    private DeploymentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;

    public DeploymentResponse() {
    }

    public DeploymentResponse(Long id, Long projectId, String projectName, String version,
                              DeploymentStatus status, OffsetDateTime createdAt, OffsetDateTime completedAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.version = version;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static DeploymentResponse fromEntity(Deployment deployment) {
        if (deployment == null) {
            return null;
        }
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getProject() != null ? deployment.getProject().getId() : null,
                deployment.getProject() != null ? deployment.getProject().getName() : null,
                deployment.getVersion(),
                deployment.getStatus(),
                deployment.getCreatedAt(),
                deployment.getCompletedAt()
        );
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
