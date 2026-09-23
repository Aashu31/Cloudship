package com.cloudship.event;

import com.cloudship.entity.CIBuild;

/**
 * Event fired when a CI build status or push status transitions.
 * Used by the Version 7 Full CI/CD Orchestration engine to progress pipeline executions.
 */
public class CIBuildStatusChangedEvent {

    private final CIBuild build;

    public CIBuildStatusChangedEvent(CIBuild build) {
        this.build = build;
    }

    public CIBuild getBuild() {
        return build;
    }
}
