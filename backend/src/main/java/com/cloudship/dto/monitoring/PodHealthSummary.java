package com.cloudship.dto.monitoring;

import java.time.OffsetDateTime;

/**
 * Pod-level health inspection DTO for CloudShip Version 8 workload observability.
 */
public class PodHealthSummary {

    private String podName;
    private String namespace;
    private String phase; // RUNNING, PENDING, SUCCEEDED, FAILED, UNKNOWN
    private boolean ready;
    private int restartCount;
    private String containerState; // RUNNING, WAITING, TERMINATED, CRASH_LOOP_BACKOFF
    private String image;
    private String reason;
    private String message;
    private OffsetDateTime startTime;

    public PodHealthSummary() {
    }

    public PodHealthSummary(String podName, String namespace, String phase, boolean ready,
                            int restartCount, String containerState, String image,
                            String reason, String message, OffsetDateTime startTime) {
        this.podName = podName;
        this.namespace = namespace;
        this.phase = phase;
        this.ready = ready;
        this.restartCount = restartCount;
        this.containerState = containerState;
        this.image = image;
        this.reason = reason;
        this.message = message;
        this.startTime = startTime;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }

    public String getContainerState() {
        return containerState;
    }

    public void setContainerState(String containerState) {
        this.containerState = containerState;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }
}
