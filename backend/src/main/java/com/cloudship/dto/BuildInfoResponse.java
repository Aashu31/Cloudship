package com.cloudship.dto;

import java.time.OffsetDateTime;

public class BuildInfoResponse {

    private String application;
    private String version;
    private String environment;
    private String dockerImage;
    private String javaVersion;
    private String containerStatus;
    private OffsetDateTime timestamp;

    public BuildInfoResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public BuildInfoResponse(String application, String version, String environment, String dockerImage, String javaVersion, String containerStatus) {
        this.application = application;
        this.version = version;
        this.environment = environment;
        this.dockerImage = dockerImage;
        this.javaVersion = javaVersion;
        this.containerStatus = containerStatus;
        this.timestamp = OffsetDateTime.now();
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getContainerStatus() {
        return containerStatus;
    }

    public void setContainerStatus(String containerStatus) {
        this.containerStatus = containerStatus;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
