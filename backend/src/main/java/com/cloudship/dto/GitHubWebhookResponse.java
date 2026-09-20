package com.cloudship.dto;

public class GitHubWebhookResponse {

    private String status;
    private String message;
    private Long buildId;
    private Long projectId;

    public GitHubWebhookResponse() {
    }

    public GitHubWebhookResponse(String status, String message, Long buildId, Long projectId) {
        this.status = status;
        this.message = message;
        this.buildId = buildId;
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getBuildId() {
        return buildId;
    }

    public void setBuildId(Long buildId) {
        this.buildId = buildId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
