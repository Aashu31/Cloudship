package com.cloudship.dto;

import com.cloudship.entity.CIBuildStatus;

public class CIBuildStatusUpdateRequest {

    private CIBuildStatus status;
    private Integer jenkinsBuildNumber;
    private Long durationMs;
    private String dockerImageTag;
    private String errorMessage;

    public CIBuildStatusUpdateRequest() {
    }

    public CIBuildStatusUpdateRequest(CIBuildStatus status, Integer jenkinsBuildNumber, Long durationMs, String dockerImageTag, String errorMessage) {
        this.status = status;
        this.jenkinsBuildNumber = jenkinsBuildNumber;
        this.durationMs = durationMs;
        this.dockerImageTag = dockerImageTag;
        this.errorMessage = errorMessage;
    }

    public CIBuildStatus getStatus() {
        return status;
    }

    public void setStatus(CIBuildStatus status) {
        this.status = status;
    }

    public Integer getJenkinsBuildNumber() {
        return jenkinsBuildNumber;
    }

    public void setJenkinsBuildNumber(Integer jenkinsBuildNumber) {
        this.jenkinsBuildNumber = jenkinsBuildNumber;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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
}
