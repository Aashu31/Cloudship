package com.cloudship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^(https?://.+|git@.+)$",
        message = "Repository URL must be a valid HTTP/HTTPS or Git SSH address"
    )
    private String repositoryUrl;

    public ProjectRequest() {
    }

    public ProjectRequest(String name, String description, String repositoryUrl) {
        this.name = name;
        this.description = description;
        this.repositoryUrl = repositoryUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl != null ? repositoryUrl.trim() : null;
    }
}
