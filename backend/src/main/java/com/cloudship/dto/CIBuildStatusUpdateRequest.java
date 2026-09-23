package com.cloudship.dto;

import com.cloudship.entity.CIBuildStatus;

public class CIBuildStatusUpdateRequest {

    private CIBuildStatus status;
    private Integer jenkinsBuildNumber;
    private Long durationMs;
    private String dockerImageTag;
    private String errorMessage;
    private com.cloudship.entity.CIPushStatus pushStatus;
    private Long pushDurationMs;
    private String pushErrorMessage;
    private String imageDigest;
    private String registryName;
    private String registryLoginServer;

    public CIBuildStatusUpdateRequest() {
    }

    public CIBuildStatusUpdateRequest(CIBuildStatus status, Integer jenkinsBuildNumber, Long durationMs, String dockerImageTag, String errorMessage) {
        this.status = status;
        this.jenkinsBuildNumber = jenkinsBuildNumber;
        this.durationMs = durationMs;
        this.dockerImageTag = dockerImageTag;
        this.errorMessage = errorMessage;
    }

    public CIBuildStatusUpdateRequest(CIBuildStatus status, Integer jenkinsBuildNumber, Long durationMs,
                                      String dockerImageTag, String errorMessage,
                                      com.cloudship.entity.CIPushStatus pushStatus, Long pushDurationMs,
                                      String pushErrorMessage, String imageDigest,
                                      String registryName, String registryLoginServer) {
        this.status = status;
        this.jenkinsBuildNumber = jenkinsBuildNumber;
        this.durationMs = durationMs;
        this.dockerImageTag = dockerImageTag;
        this.errorMessage = errorMessage;
        this.pushStatus = pushStatus;
        this.pushDurationMs = pushDurationMs;
        this.pushErrorMessage = pushErrorMessage;
        this.imageDigest = imageDigest;
        this.registryName = registryName;
        this.registryLoginServer = registryLoginServer;
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

    public com.cloudship.entity.CIPushStatus getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(com.cloudship.entity.CIPushStatus pushStatus) {
        this.pushStatus = pushStatus;
    }

    public Long getPushDurationMs() {
        return pushDurationMs;
    }

    public void setPushDurationMs(Long pushDurationMs) {
        this.pushDurationMs = pushDurationMs;
    }

    public String getPushErrorMessage() {
        return pushErrorMessage;
    }

    public void setPushErrorMessage(String pushErrorMessage) {
        this.pushErrorMessage = pushErrorMessage;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getRegistryLoginServer() {
        return registryLoginServer;
    }

    public void setRegistryLoginServer(String registryLoginServer) {
        this.registryLoginServer = registryLoginServer;
    }
}
