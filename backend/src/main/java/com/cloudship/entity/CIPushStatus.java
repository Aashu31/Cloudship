package com.cloudship.entity;

/**
 * Represents the lifecycle status of pushing a built container image
 * to the container registry (e.g., Azure Container Registry).
 */
public enum CIPushStatus {
    NOT_STARTED,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}
