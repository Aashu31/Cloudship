package com.cloudship.event;

import com.cloudship.entity.Deployment;

/**
 * Event fired when a Kubernetes Deployment status transitions.
 * Consumed by MonitoringService to log operational events in Version 8.
 */
public class DeploymentStatusChangedEvent {

    private final Deployment deployment;

    public DeploymentStatusChangedEvent(Deployment deployment) {
        this.deployment = deployment;
    }

    public Deployment getDeployment() {
        return deployment;
    }
}
