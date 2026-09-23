package com.cloudship.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states for the CloudShip Version 7 unified CI/CD pipeline orchestration engine.
 */
public enum PipelineStatus {

    // Forward pipeline lifecycle states
    QUEUED,
    CI_RUNNING,
    CI_SUCCESS,
    IMAGE_PUSHING,
    IMAGE_PUSHED,
    DEPLOYMENT_STARTING,
    DEPLOYING,
    ROLLOUT_VERIFYING,
    SUCCESS,

    // Failure / terminal states
    CI_FAILED,
    IMAGE_PUSH_FAILED,
    DEPLOYMENT_FAILED,
    ROLLOUT_FAILED,
    PIPELINE_FAILED,
    CANCELLED;

    private static final Set<PipelineStatus> TERMINAL_STATES = EnumSet.of(
            SUCCESS,
            CI_FAILED,
            IMAGE_PUSH_FAILED,
            DEPLOYMENT_FAILED,
            ROLLOUT_FAILED,
            PIPELINE_FAILED,
            CANCELLED
    );

    private static final Set<PipelineStatus> FAILURE_STATES = EnumSet.of(
            CI_FAILED,
            IMAGE_PUSH_FAILED,
            DEPLOYMENT_FAILED,
            ROLLOUT_FAILED,
            PIPELINE_FAILED,
            CANCELLED
    );

    /**
     * Determines whether the pipeline has reached an immutable terminal state.
     */
    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    /**
     * Determines whether the status represents a failure or cancellation.
     */
    public boolean isFailure() {
        return FAILURE_STATES.contains(this);
    }

    /**
     * Determines whether the pipeline is currently in an active, non-terminal execution phase.
     */
    public boolean isRunning() {
        return !isTerminal();
    }

    /**
     * Validates whether a state transition from this status to the target status is allowed.
     */
    public boolean canTransitionTo(PipelineStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true; // Idempotent self-transition
        }
        if (this.isTerminal()) {
            return false; // Terminal states cannot transition to any other state
        }
        if (target == CANCELLED || target == PIPELINE_FAILED) {
            return true; // Can always be cancelled or marked failed from any running state
        }

        return switch (this) {
            case QUEUED -> target == CI_RUNNING || target == CI_FAILED;
            case CI_RUNNING -> target == CI_SUCCESS || target == IMAGE_PUSHING || target == CI_FAILED;
            case CI_SUCCESS -> target == IMAGE_PUSHING || target == IMAGE_PUSHED || target == IMAGE_PUSH_FAILED || target == CI_FAILED;
            case IMAGE_PUSHING -> target == IMAGE_PUSHED || target == IMAGE_PUSH_FAILED;
            case IMAGE_PUSHED -> target == DEPLOYMENT_STARTING || target == IMAGE_PUSH_FAILED || target == DEPLOYMENT_FAILED;
            case DEPLOYMENT_STARTING -> target == DEPLOYING || target == DEPLOYMENT_FAILED;
            case DEPLOYING -> target == ROLLOUT_VERIFYING || target == DEPLOYMENT_FAILED;
            case ROLLOUT_VERIFYING -> target == SUCCESS || target == ROLLOUT_FAILED;
            default -> false;
        };
    }
}
