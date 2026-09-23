package com.cloudship.dto;

import jakarta.validation.constraints.Size;

public class PipelineTriggerRequest {

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branch;

    @Size(max = 100, message = "Commit SHA must not exceed 100 characters")
    private String commitSha;

    @Size(max = 1000, message = "Commit message must not exceed 1000 characters")
    private String commitMessage;

    @Size(max = 255, message = "Commit author must not exceed 255 characters")
    private String commitAuthor;

    private String clusterName;
    private String namespace;
    private Integer replicas;

    public PipelineTriggerRequest() {
    }

    public PipelineTriggerRequest(String branch, String commitSha, String commitMessage, String commitAuthor) {
        this.branch = branch;
        this.commitSha = commitSha;
        this.commitMessage = commitMessage;
        this.commitAuthor = commitAuthor;
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

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
