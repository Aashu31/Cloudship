package com.cloudship.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Entity representing an end-to-end Version 7 CI/CD Pipeline Execution.
 * Orchestrates Git checkout -> Jenkins CI -> Docker packaging -> ACR push
 * -> Image Verification -> AKS Deployment -> Rollout Verification.
 */
@Entity
@Table(name = "pipeline_executions", indexes = {
        @Index(name = "idx_pipeline_project_id", columnList = "project_id"),
        @Index(name = "idx_pipeline_status", columnList = "status"),
        @Index(name = "idx_pipeline_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_pipeline_ci_build_id", columnList = "ci_build_id"),
        @Index(name = "idx_pipeline_deployment_id", columnList = "deployment_id")
})
public class PipelineExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ci_build_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private CIBuild ciBuild;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deployment_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private Deployment deployment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PipelineStatus status = PipelineStatus.QUEUED;

    @Column
    private String branch;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "commit_author")
    private String commitAuthor;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private CITriggerType triggerType = CITriggerType.MANUAL;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "image_tag", length = 100)
    private String imageTag;

    @Column(name = "image_digest")
    private String imageDigest;

    @Column(name = "cluster_name", length = 100)
    private String clusterName;

    @Column(length = 100)
    private String namespace = "default";

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public PipelineExecution() {
    }

    public PipelineExecution(Project project, String branch, String commitSha, CITriggerType triggerType) {
        this.project = project;
        this.branch = branch;
        this.commitSha = commitSha;
        this.triggerType = triggerType != null ? triggerType : CITriggerType.MANUAL;
        this.status = PipelineStatus.QUEUED;
        this.startedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.startedAt == null) {
            this.startedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Safely transitions the pipeline to a new state, enforcing the lifecycle state machine.
     * Prevents invalid state transitions and ensures terminal states are immutable.
     */
    public boolean transitionTo(PipelineStatus targetStatus, String error) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }
        if (this.status == targetStatus) {
            return false;
        }
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid pipeline state transition from '%s' to '%s' (Pipeline ID: %d)",
                    this.status, targetStatus, this.id
            ));
        }

        this.status = targetStatus;
        this.updatedAt = OffsetDateTime.now();

        if (error != null && !error.isBlank()) {
            this.errorMessage = (error.length() > 1950) ? error.substring(0, 1950) + "..." : error;
        }

        if (targetStatus.isTerminal()) {
            if (this.completedAt == null) {
                this.completedAt = OffsetDateTime.now();
            }
            if (this.startedAt != null && this.durationMs == null) {
                this.durationMs = Duration.between(this.startedAt, this.completedAt).toMillis();
            }
        }

        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public CIBuild getCiBuild() {
        return ciBuild;
    }

    public void setCiBuild(CIBuild ciBuild) {
        this.ciBuild = ciBuild;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public void setCommitAuthor(String commitAuthor) {
        this.commitAuthor = commitAuthor;
    }

    public CITriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(CITriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Long getDurationSeconds() {
        return durationMs != null ? durationMs / 1000 : null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
