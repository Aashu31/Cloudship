package com.cloudship.dto;

import jakarta.validation.constraints.Size;

public class CITriggerRequest {

    @Size(max = 100, message = "Branch name must not exceed 100 characters")
    private String branch;

    @Size(max = 100, message = "Commit SHA must not exceed 100 characters")
    private String commitSha;

    @Size(max = 500, message = "Commit message must not exceed 500 characters")
    private String commitMessage;

    @Size(max = 100, message = "Commit author must not exceed 100 characters")
    private String commitAuthor;

    public CITriggerRequest() {
    }

    public CITriggerRequest(String branch, String commitSha, String commitMessage, String commitAuthor) {
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
}
