package com.cloudship.service.jenkins;

public class JenkinsTriggerResult {

    private final boolean success;
    private final Integer buildNumber;
    private final String queueItemUrl;
    private final String message;

    public JenkinsTriggerResult(boolean success, Integer buildNumber, String queueItemUrl, String message) {
        this.success = success;
        this.buildNumber = buildNumber;
        this.queueItemUrl = queueItemUrl;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public String getQueueItemUrl() {
        return queueItemUrl;
    }

    public String getMessage() {
        return message;
    }
}
