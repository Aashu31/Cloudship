package com.cloudship.dto.monitoring;

import java.time.OffsetDateTime;

/**
 * Concrete operational metrics foundation calculated from real database & cluster data.
 */
public class MonitoringMetricsResponse {

    private long totalPipelines;
    private long runningPipelines;
    private long successfulPipelines;
    private long failedPipelines;
    private double pipelineSuccessRatePercent;

    private long totalDeployments;
    private long activeDeployments;
    private long successfulDeployments;
    private long failedDeployments;
    private double deploymentSuccessRatePercent;

    private int totalPods;
    private int runningPods;
    private int pendingPods;
    private int failedPods;
    private int totalContainerRestarts;

    private Double avgPipelineDurationSeconds;
    private Double avgDeploymentDurationSeconds;
    private OffsetDateTime calculatedAt;

    public MonitoringMetricsResponse() {
    }

    public long getTotalPipelines() {
        return totalPipelines;
    }

    public void setTotalPipelines(long totalPipelines) {
        this.totalPipelines = totalPipelines;
    }

    public long getRunningPipelines() {
        return runningPipelines;
    }

    public void setRunningPipelines(long runningPipelines) {
        this.runningPipelines = runningPipelines;
    }

    public long getSuccessfulPipelines() {
        return successfulPipelines;
    }

    public void setSuccessfulPipelines(long successfulPipelines) {
        this.successfulPipelines = successfulPipelines;
    }

    public long getFailedPipelines() {
        return failedPipelines;
    }

    public void setFailedPipelines(long failedPipelines) {
        this.failedPipelines = failedPipelines;
    }

    public double getPipelineSuccessRatePercent() {
        return pipelineSuccessRatePercent;
    }

    public void setPipelineSuccessRatePercent(double pipelineSuccessRatePercent) {
        this.pipelineSuccessRatePercent = pipelineSuccessRatePercent;
    }

    public long getTotalDeployments() {
        return totalDeployments;
    }

    public void setTotalDeployments(long totalDeployments) {
        this.totalDeployments = totalDeployments;
    }

    public long getActiveDeployments() {
        return activeDeployments;
    }

    public void setActiveDeployments(long activeDeployments) {
        this.activeDeployments = activeDeployments;
    }

    public long getSuccessfulDeployments() {
        return successfulDeployments;
    }

    public void setSuccessfulDeployments(long successfulDeployments) {
        this.successfulDeployments = successfulDeployments;
    }

    public long getFailedDeployments() {
        return failedDeployments;
    }

    public void setFailedDeployments(long failedDeployments) {
        this.failedDeployments = failedDeployments;
    }

    public double getDeploymentSuccessRatePercent() {
        return deploymentSuccessRatePercent;
    }

    public void setDeploymentSuccessRatePercent(double deploymentSuccessRatePercent) {
        this.deploymentSuccessRatePercent = deploymentSuccessRatePercent;
    }

    public int getTotalPods() {
        return totalPods;
    }

    public void setTotalPods(int totalPods) {
        this.totalPods = totalPods;
    }

    public int getRunningPods() {
        return runningPods;
    }

    public void setRunningPods(int runningPods) {
        this.runningPods = runningPods;
    }

    public int getPendingPods() {
        return pendingPods;
    }

    public void setPendingPods(int pendingPods) {
        this.pendingPods = pendingPods;
    }

    public int getFailedPods() {
        return failedPods;
    }

    public void setFailedPods(int failedPods) {
        this.failedPods = failedPods;
    }

    public int getTotalContainerRestarts() {
        return totalContainerRestarts;
    }

    public void setTotalContainerRestarts(int totalContainerRestarts) {
        this.totalContainerRestarts = totalContainerRestarts;
    }

    public Double getAvgPipelineDurationSeconds() {
        return avgPipelineDurationSeconds;
    }

    public void setAvgPipelineDurationSeconds(Double avgPipelineDurationSeconds) {
        this.avgPipelineDurationSeconds = avgPipelineDurationSeconds;
    }

    public Double getAvgDeploymentDurationSeconds() {
        return avgDeploymentDurationSeconds;
    }

    public void setAvgDeploymentDurationSeconds(Double avgDeploymentDurationSeconds) {
        this.avgDeploymentDurationSeconds = avgDeploymentDurationSeconds;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(OffsetDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
