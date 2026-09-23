package com.cloudship.dto.monitoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed deployment and workload operational health status for CloudShip Version 8.
 */
public class WorkloadHealthResponse {

    private String namespace;
    private String deploymentName;
    private int desiredReplicas;
    private int updatedReplicas;
    private int availableReplicas;
    private int readyReplicas;
    private String status; // HEALTHY, DEGRADED, PROGRESSING, FAILED, UNKNOWN
    private String image;
    private List<String> conditions = new ArrayList<>();
    private List<PodHealthSummary> pods = new ArrayList<>();

    public WorkloadHealthResponse() {
    }

    public WorkloadHealthResponse(String namespace, String deploymentName, int desiredReplicas,
                                  int updatedReplicas, int availableReplicas, int readyReplicas,
                                  String status, String image, List<String> conditions,
                                  List<PodHealthSummary> pods) {
        this.namespace = namespace;
        this.deploymentName = deploymentName;
        this.desiredReplicas = desiredReplicas;
        this.updatedReplicas = updatedReplicas;
        this.availableReplicas = availableReplicas;
        this.readyReplicas = readyReplicas;
        this.status = status;
        this.image = image;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.pods = pods != null ? pods : new ArrayList<>();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public int getDesiredReplicas() {
        return desiredReplicas;
    }

    public void setDesiredReplicas(int desiredReplicas) {
        this.desiredReplicas = desiredReplicas;
    }

    public int getUpdatedReplicas() {
        return updatedReplicas;
    }

    public void setUpdatedReplicas(int updatedReplicas) {
        this.updatedReplicas = updatedReplicas;
    }

    public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public int getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(int readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public List<PodHealthSummary> getPods() {
        return pods;
    }

    public void setPods(List<PodHealthSummary> pods) {
        this.pods = pods;
    }
}
