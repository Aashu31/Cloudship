package com.cloudship.dto.azure;

import java.time.OffsetDateTime;

public class KubernetesWorkloadResponse {

    private String name;
    private String namespace;
    private Integer desiredReplicas;
    private Integer readyReplicas;
    private Integer updatedReplicas;
    private Integer availableReplicas;
    private String image;
    private String rolloutStatus;
    private OffsetDateTime creationTimestamp;

    public KubernetesWorkloadResponse() {
    }

    public KubernetesWorkloadResponse(
            String name,
            String namespace,
            Integer desiredReplicas,
            Integer readyReplicas,
            Integer updatedReplicas,
            Integer availableReplicas,
            String image,
            String rolloutStatus,
            OffsetDateTime creationTimestamp) {
        this.name = name;
        this.namespace = namespace;
        this.desiredReplicas = desiredReplicas;
        this.readyReplicas = readyReplicas;
        this.updatedReplicas = updatedReplicas;
        this.availableReplicas = availableReplicas;
        this.image = image;
        this.rolloutStatus = rolloutStatus;
        this.creationTimestamp = creationTimestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Integer getDesiredReplicas() {
        return desiredReplicas;
    }

    public void setDesiredReplicas(Integer desiredReplicas) {
        this.desiredReplicas = desiredReplicas;
    }

    public Integer getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(Integer readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public Integer getUpdatedReplicas() {
        return updatedReplicas;
    }

    public void setUpdatedReplicas(Integer updatedReplicas) {
        this.updatedReplicas = updatedReplicas;
    }

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(Integer availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRolloutStatus() {
        return rolloutStatus;
    }

    public void setRolloutStatus(String rolloutStatus) {
        this.rolloutStatus = rolloutStatus;
    }

    public OffsetDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(OffsetDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
}
