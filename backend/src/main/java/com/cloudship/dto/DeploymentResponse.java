package com.cloudship.dto;

import com.cloudship.entity.Deployment;
import com.cloudship.entity.DeploymentStatus;
import java.time.OffsetDateTime;

public class DeploymentResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long ciBuildId;
    private String version;
    private DeploymentStatus status;
    private String clusterName;
    private String namespace;
    private String deploymentName;
    private String serviceName;
    private String imageName;
    private String imageTag;
    private String imageDigest;
    private Integer replicas;
    private Integer readyReplicas;
    private Integer updatedReplicas;
    private Integer availableReplicas;
    private String rolloutStatus;
    private String errorMessage;
    private OffsetDateTime startedAt;
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
        DeploymentResponse response = new DeploymentResponse();
        response.setId(deployment.getId());
        response.setProjectId(deployment.getProject() != null ? deployment.getProject().getId() : null);
        response.setProjectName(deployment.getProject() != null ? deployment.getProject().getName() : null);
        response.setCiBuildId(deployment.getCiBuild() != null ? deployment.getCiBuild().getId() : null);
        response.setVersion(deployment.getVersion());
        response.setStatus(deployment.getStatus());
        response.setClusterName(deployment.getClusterName());
        response.setNamespace(deployment.getNamespace());
        response.setDeploymentName(deployment.getDeploymentName());
        response.setServiceName(deployment.getServiceName());
        response.setImageName(deployment.getImageName());
        response.setImageTag(deployment.getImageTag());
        response.setImageDigest(deployment.getImageDigest());
        response.setReplicas(deployment.getReplicas());
        response.setReadyReplicas(deployment.getReadyReplicas());
        response.setUpdatedReplicas(deployment.getUpdatedReplicas());
        response.setAvailableReplicas(deployment.getAvailableReplicas());
        response.setRolloutStatus(deployment.getRolloutStatus());
        response.setErrorMessage(deployment.getErrorMessage());
        response.setStartedAt(deployment.getStartedAt());
        response.setCreatedAt(deployment.getCreatedAt());
        response.setCompletedAt(deployment.getCompletedAt());
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getCiBuildId() {
        return ciBuildId;
    }

    public void setCiBuildId(Long ciBuildId) {
        this.ciBuildId = ciBuildId;
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

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Integer getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(Integer readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public Integer getUpdatedReplicas() {
        return updatedReplicas;
    }

    public void setUpdatedReplicas(Integer updatedReplicas) {
        this.updatedReplicas = updatedReplicas;
    }

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(Integer availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public String getRolloutStatus() {
        return rolloutStatus;
    }

    public void setRolloutStatus(String rolloutStatus) {
        this.rolloutStatus = rolloutStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
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
