package com.cloudship.dto;

import com.cloudship.entity.CIBuild;
import com.cloudship.entity.CIBuildStatus;
import com.cloudship.entity.CITriggerType;

import java.time.OffsetDateTime;

public class CIBuildResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long gitRepositoryId;
    private String repositoryUrl;
    private String commitSha;
    private String branch;
    private CITriggerType triggerType;
    private CIBuildStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Long durationMs;
    private String commitMessage;
    private String commitAuthor;
    private Integer jenkinsBuildNumber;
    private String jenkinsJobName;
    private String dockerImageName;
    private String dockerImageTag;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CIBuildResponse() {
    }

    public static CIBuildResponse fromEntity(CIBuild entity) {
        if (entity == null) {
            return null;
        }

        CIBuildResponse response = new CIBuildResponse();
        response.setId(entity.getId());
        if (entity.getProject() != null) {
            response.setProjectId(entity.getProject().getId());
            response.setProjectName(entity.getProject().getName());
        }
        if (entity.getGitRepository() != null) {
            response.setGitRepositoryId(entity.getGitRepository().getId());
            response.setRepositoryUrl(entity.getGitRepository().getRepositoryUrl());
        } else if (entity.getProject() != null) {
            response.setRepositoryUrl(entity.getProject().getRepositoryUrl());
        }
        response.setCommitSha(entity.getCommitSha());
        response.setBranch(entity.getBranch());
        response.setTriggerType(entity.getTriggerType());
        response.setStatus(entity.getStatus());
        response.setStartedAt(entity.getStartedAt());
        response.setCompletedAt(entity.getCompletedAt());
        response.setDurationMs(entity.getDurationMs());
        response.setCommitMessage(entity.getCommitMessage());
        response.setCommitAuthor(entity.getCommitAuthor());
        response.setJenkinsBuildNumber(entity.getJenkinsBuildNumber());
        response.setJenkinsJobName(entity.getJenkinsJobName());
        response.setDockerImageName(entity.getDockerImageName());
        response.setDockerImageTag(entity.getDockerImageTag());
        response.setErrorMessage(entity.getErrorMessage());
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getGitRepositoryId() {
        return gitRepositoryId;
    }

    public void setGitRepositoryId(Long gitRepositoryId) {
        this.gitRepositoryId = gitRepositoryId;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public CITriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(CITriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public CIBuildStatus getStatus() {
        return status;
    }

    public void setStatus(CIBuildStatus status) {
        this.status = status;
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

    public Integer getJenkinsBuildNumber() {
        return jenkinsBuildNumber;
    }

    public void setJenkinsBuildNumber(Integer jenkinsBuildNumber) {
        this.jenkinsBuildNumber = jenkinsBuildNumber;
    }

    public String getJenkinsJobName() {
        return jenkinsJobName;
    }

    public void setJenkinsJobName(String jenkinsJobName) {
        this.jenkinsJobName = jenkinsJobName;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public void setDockerImageTag(String dockerImageTag) {
        this.dockerImageTag = dockerImageTag;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
