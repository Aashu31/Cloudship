package com.cloudship.dto.monitoring;

import com.cloudship.dto.azure.AksClusterResponse;
import com.cloudship.dto.azure.AzureRegistryResponse;
import com.cloudship.dto.azure.AzureStatusResponse;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level consolidated monitoring snapshot for the CloudShip Version 8 control center.
 */
public class MonitoringOverviewResponse {

    private Map<String, Object> application = new HashMap<>();
    private Map<String, Object> database = new HashMap<>();
    private AzureStatusResponse azure;
    private AzureRegistryResponse acr;
    private AksClusterResponse aks;
    private Map<String, Object> kubernetes = new HashMap<>();
    private Map<String, Object> pipelines = new HashMap<>();
    private List<MonitoringEventResponse> recentEvents = new ArrayList<>();
    private Map<String, Object> selfHealth = new HashMap<>();
    private OffsetDateTime timestamp;

    public MonitoringOverviewResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public Map<String, Object> getApplication() {
        return application;
    }

    public void setApplication(Map<String, Object> application) {
        this.application = application;
    }

    public Map<String, Object> getDatabase() {
        return database;
    }

    public void setDatabase(Map<String, Object> database) {
        this.database = database;
    }

    public AzureStatusResponse getAzure() {
        return azure;
    }

    public void setAzure(AzureStatusResponse azure) {
        this.azure = azure;
    }

    public AzureRegistryResponse getAcr() {
        return acr;
    }

    public void setAcr(AzureRegistryResponse acr) {
        this.acr = acr;
    }

    public AksClusterResponse getAks() {
        return aks;
    }

    public void setAks(AksClusterResponse aks) {
        this.aks = aks;
    }

    public Map<String, Object> getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(Map<String, Object> kubernetes) {
        this.kubernetes = kubernetes;
    }

    public Map<String, Object> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Map<String, Object> pipelines) {
        this.pipelines = pipelines;
    }

    public List<MonitoringEventResponse> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<MonitoringEventResponse> recentEvents) {
        this.recentEvents = recentEvents;
    }

    public Map<String, Object> getSelfHealth() {
        return selfHealth;
    }

    public void setSelfHealth(Map<String, Object> selfHealth) {
        this.selfHealth = selfHealth;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
