package com.cloudship.event;

import com.cloudship.entity.CIBuild;

/**
 * Event fired when a new CI build is initiated/created in CloudShip.
 */
public class CIBuildCreatedEvent {

    private final CIBuild build;

    public CIBuildCreatedEvent(CIBuild build) {
        this.build = build;
    }

    public CIBuild getBuild() {
        return build;
    }
}
