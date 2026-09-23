package com.cloudship.dto.monitoring;

import java.time.OffsetDateTime;

/**
 * DTO for operational event records presented on the CloudShip dashboard.
 */
public class MonitoringEventResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String eventType;
    private String severity;
    private String source;
    private String message;
    private String detailsJson;
    private OffsetDateTime createdAt;

    public MonitoringEventResponse() {
    }

    public MonitoringEventResponse(Long id, Long projectId, String projectName, String eventType,
                                   String severity, String source, String message,
                                   String detailsJson, OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.eventType = eventType;
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
