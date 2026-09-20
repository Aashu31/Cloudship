package com.cloudship.service.jenkins;

import com.cloudship.entity.CIBuildStatus;

public class JenkinsBuildDetails {

    private final int buildNumber;
    private final CIBuildStatus status;
    private final long durationMs;
    private final String result;

    public JenkinsBuildDetails(int buildNumber, CIBuildStatus status, long durationMs, String result) {
        this.buildNumber = buildNumber;
        this.status = status;
        this.durationMs = durationMs;
        this.result = result;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public CIBuildStatus getStatus() {
        return status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getResult() {
        return result;
    }
}
