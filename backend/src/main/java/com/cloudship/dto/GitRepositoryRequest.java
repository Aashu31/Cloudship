package com.cloudship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GitRepositoryRequest {

    @NotBlank(message = "Repository URL is required")
    @Size(max = 255, message = "Repository URL must not exceed 255 characters")
    @Pattern(
        regexp = "^(https:\\/\\/github\\.com\\/[A-Za-z0-9_.-]+\\/[A-Za-z0-9_.-]+(\\.[A-Za-z0-9_.-]+)?|git@github\\.com:[A-Za-z0-9_.-]+\\/[A-Za-z0-9_.-]+(\\.[A-Za-z0-9_.-]+)?|ssh:\\/\\/git@github\\.com\\/[A-Za-z0-9_.-]+\\/[A-Za-z0-9_.-]+(\\.[A-Za-z0-9_.-]+)?)$",
        message = "Repository URL must be a valid GitHub repository HTTPS or Git SSH address"
    )
    private String repositoryUrl;

    @Size(max = 100, message = "Default branch name must not exceed 100 characters")
    private String defaultBranch;

    public GitRepositoryRequest() {
    }

    public GitRepositoryRequest(String repositoryUrl, String defaultBranch) {
        this.repositoryUrl = repositoryUrl;
        this.defaultBranch = defaultBranch;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
}
