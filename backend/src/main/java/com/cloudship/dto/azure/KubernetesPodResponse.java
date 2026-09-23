package com.cloudship.dto.azure;

import java.time.OffsetDateTime;

public class KubernetesPodResponse {

    private String name;
    private String namespace;
    private String nodeName;
    private String phase;
    private String ready;
    private Integer readyContainers;
    private Integer totalContainers;
    private Integer restartCount;
    private OffsetDateTime startTime;
    private String age;
    private String statusMessage;

    public KubernetesPodResponse() {
    }

    public KubernetesPodResponse(
            String name,
            String namespace,
            String nodeName,
            String phase,
            String ready,
            Integer readyContainers,
            Integer totalContainers,
            Integer restartCount,
            OffsetDateTime startTime,
            String age,
            String statusMessage) {
        this.name = name;
        this.namespace = namespace;
        this.nodeName = nodeName;
        this.phase = phase;
        this.ready = ready;
        this.readyContainers = readyContainers;
        this.totalContainers = totalContainers;
        this.restartCount = restartCount;
        this.startTime = startTime;
        this.age = age;
        this.statusMessage = statusMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getReady() {
        return ready;
    }

    public void setReady(String ready) {
        this.ready = ready;
    }

    public Integer getReadyContainers() {
        return readyContainers;
    }

    public void setReadyContainers(Integer readyContainers) {
        this.readyContainers = readyContainers;
    }

    public Integer getTotalContainers() {
        return totalContainers;
    }

    public void setTotalContainers(Integer totalContainers) {
        this.totalContainers = totalContainers;
    }

    public Integer getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(Integer restartCount) {
        this.restartCount = restartCount;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
