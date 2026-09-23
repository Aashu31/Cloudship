package com.cloudship.event;

import com.cloudship.entity.PipelineExecution;

/**
 * Event fired when a CI/CD Pipeline execution status transitions.
 * Consumed by MonitoringService to log operational events in Version 8.
 */
public class PipelineStatusChangedEvent {

    private final PipelineExecution pipelineExecution;

    public PipelineStatusChangedEvent(PipelineExecution pipelineExecution) {
        this.pipelineExecution = pipelineExecution;
    }

    public PipelineExecution getPipelineExecution() {
        return pipelineExecution;
    }
}
