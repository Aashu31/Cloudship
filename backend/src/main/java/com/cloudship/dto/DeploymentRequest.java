package com.cloudship.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DeploymentRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long ciBuildId;

    private String clusterName;

    private String namespace;

    private String deploymentName;

    private String serviceName;

    @Min(value = 1, message = "Replicas must be at least 1")
    @Max(value = 10, message = "Replicas must not exceed 10")
    private Integer replicas = 1;

    private String imageName;

    private String imageTag;

    private String imageDigest;

    public DeploymentRequest() {
    }

    public DeploymentRequest(Long projectId) {
        this.projectId = projectId;
    }

    public DeploymentRequest(Long projectId, Long ciBuildId, Integer replicas) {
        this.projectId = projectId;
        this.ciBuildId = ciBuildId;
        this.replicas = replicas;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCiBuildId() {
        return ciBuildId;
    }

    public void setCiBuildId(Long ciBuildId) {
        this.ciBuildId = ciBuildId;
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

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
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
}
