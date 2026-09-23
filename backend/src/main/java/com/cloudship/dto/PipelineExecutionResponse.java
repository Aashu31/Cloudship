package com.cloudship.dto;

import com.cloudship.entity.PipelineExecution;
import com.cloudship.entity.PipelineStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelineExecutionResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long ciBuildId;
    private Long deploymentId;
    private PipelineStatus status;
    private String stage;
    private String branch;
    private String commitSha;
    private String commitMessage;
    private String commitAuthor;
    private String triggerType;
    private String imageName;
    private String imageTag;
    private String imageDigest;
    private String clusterName;
    private String namespace;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Long durationMs;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private CIBuildResponse ciBuild;
    private DeploymentResponse deployment;

    public PipelineExecutionResponse() {
    }

    public static PipelineExecutionResponse fromEntity(PipelineExecution entity) {
        if (entity == null) {
            return null;
        }

        PipelineExecutionResponse resp = new PipelineExecutionResponse();
        resp.setId(entity.getId());
        if (entity.getProject() != null) {
            resp.setProjectId(entity.getProject().getId());
            resp.setProjectName(entity.getProject().getName());
        }
        if (entity.getCiBuild() != null) {
            resp.setCiBuildId(entity.getCiBuild().getId());
            resp.setCiBuild(CIBuildResponse.fromEntity(entity.getCiBuild()));
        }
        if (entity.getDeployment() != null) {
            resp.setDeploymentId(entity.getDeployment().getId());
            resp.setDeployment(DeploymentResponse.fromEntity(entity.getDeployment()));
        }
        resp.setStatus(entity.getStatus());
        resp.setStage(resolveStage(entity.getStatus()));
        resp.setBranch(entity.getBranch());
        resp.setCommitSha(entity.getCommitSha());
        resp.setCommitMessage(entity.getCommitMessage());
        resp.setCommitAuthor(entity.getCommitAuthor());
        resp.setTriggerType(entity.getTriggerType() != null ? entity.getTriggerType().name() : "MANUAL");
        resp.setImageName(entity.getImageName());
        resp.setImageTag(entity.getImageTag());
        resp.setImageDigest(entity.getImageDigest());
        resp.setClusterName(entity.getClusterName());
        resp.setNamespace(entity.getNamespace());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setStartedAt(entity.getStartedAt());
        resp.setCompletedAt(entity.getCompletedAt());
        resp.setDurationMs(entity.getDurationMs());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        return resp;
    }

    public static String resolveStage(PipelineStatus status) {
        if (status == null) return "STANDBY";
        return switch (status) {
            case QUEUED -> "SOURCE";
            case CI_RUNNING -> "BUILD";
            case CI_SUCCESS -> "CONTAINER";
            case IMAGE_PUSHING, IMAGE_PUSHED -> "REGISTRY";
            case DEPLOYMENT_STARTING, DEPLOYING -> "DEPLOY";
            case ROLLOUT_VERIFYING -> "HEALTH_CHECK";
            case SUCCESS -> "LIVE";
            case CI_FAILED -> "BUILD_FAILED";
            case IMAGE_PUSH_FAILED -> "REGISTRY_FAILED";
            case DEPLOYMENT_FAILED -> "DEPLOY_FAILED";
            case ROLLOUT_FAILED -> "ROLLOUT_FAILED";
            case PIPELINE_FAILED -> "PIPELINE_FAILED";
            case CANCELLED -> "CANCELLED";
        };
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

    public Long getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(Long deploymentId) {
        this.deploymentId = deploymentId;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public void setCommitAuthor(String commitAuthor) {
        this.commitAuthor = commitAuthor;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
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

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    public CIBuildResponse getCiBuild() {
        return ciBuild;
    }

    public void setCiBuild(CIBuildResponse ciBuild) {
        this.ciBuild = ciBuild;
    }

    public DeploymentResponse getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentResponse deployment) {
        this.deployment = deployment;
    }
}
