package com.cloudship.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ci_builds")
public class CIBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_repository_id")
    private GitRepository gitRepository;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(nullable = false, length = 100)
    private String branch = "main";

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private CITriggerType triggerType = CITriggerType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CIBuildStatus status = CIBuildStatus.QUEUED;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "commit_message", length = 500)
    private String commitMessage;

    @Column(name = "commit_author", length = 100)
    private String commitAuthor;

    @Column(name = "jenkins_build_number")
    private Integer jenkinsBuildNumber;

    @Column(name = "jenkins_job_name", length = 100)
    private String jenkinsJobName;

    @Column(name = "docker_image_name", length = 200)
    private String dockerImageName;

    @Column(name = "docker_image_tag", length = 100)
    private String dockerImageTag;

    @Column(name = "registry_name", length = 100)
    private String registryName;

    @Column(name = "registry_login_server", length = 200)
    private String registryLoginServer;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 50)
    private CIPushStatus pushStatus = CIPushStatus.NOT_STARTED;

    @Column(name = "push_started_at")
    private OffsetDateTime pushStartedAt;

    @Column(name = "push_completed_at")
    private OffsetDateTime pushCompletedAt;

    @Column(name = "push_duration_ms")
    private Long pushDurationMs;

    @Column(name = "push_error_message", length = 1000)
    private String pushErrorMessage;

    @Column(name = "image_digest", length = 200)
    private String imageDigest;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CIBuild() {
    }

    public CIBuild(Project project, GitRepository gitRepository, String branch, String commitSha, CITriggerType triggerType) {
        this.project = project;
        this.gitRepository = gitRepository;
        this.branch = branch != null ? branch : "main";
        this.commitSha = commitSha;
        this.triggerType = triggerType != null ? triggerType : CITriggerType.MANUAL;
        this.status = CIBuildStatus.QUEUED;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.branch == null || this.branch.isBlank()) {
            this.branch = "main";
        }
        if (this.triggerType == null) {
            this.triggerType = CITriggerType.MANUAL;
        }
        if (this.status == null) {
            this.status = CIBuildStatus.QUEUED;
        }
        if (this.pushStatus == null) {
            this.pushStatus = CIPushStatus.NOT_STARTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
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

    public GitRepository getGitRepository() {
        return gitRepository;
    }

    public void setGitRepository(GitRepository gitRepository) {
        this.gitRepository = gitRepository;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public CITriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(CITriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public CIBuildStatus getStatus() {
        return status;
    }

    public void setStatus(CIBuildStatus status) {
        this.status = status;
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

    public Integer getJenkinsBuildNumber() {
        return jenkinsBuildNumber;
    }

    public void setJenkinsBuildNumber(Integer jenkinsBuildNumber) {
        this.jenkinsBuildNumber = jenkinsBuildNumber;
    }

    public String getJenkinsJobName() {
        return jenkinsJobName;
    }

    public void setJenkinsJobName(String jenkinsJobName) {
        this.jenkinsJobName = jenkinsJobName;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    public String getDockerImageTag() {
        return dockerImageTag;
    }

    public void setDockerImageTag(String dockerImageTag) {
        this.dockerImageTag = dockerImageTag;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getRegistryLoginServer() {
        return registryLoginServer;
    }

    public void setRegistryLoginServer(String registryLoginServer) {
        this.registryLoginServer = registryLoginServer;
    }

    public CIPushStatus getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(CIPushStatus pushStatus) {
        this.pushStatus = pushStatus;
    }

    public OffsetDateTime getPushStartedAt() {
        return pushStartedAt;
    }

    public void setPushStartedAt(OffsetDateTime pushStartedAt) {
        this.pushStartedAt = pushStartedAt;
    }

    public OffsetDateTime getPushCompletedAt() {
        return pushCompletedAt;
    }

    public void setPushCompletedAt(OffsetDateTime pushCompletedAt) {
        this.pushCompletedAt = pushCompletedAt;
    }

    public Long getPushDurationMs() {
        return pushDurationMs;
    }

    public void setPushDurationMs(Long pushDurationMs) {
        this.pushDurationMs = pushDurationMs;
    }

    public String getPushErrorMessage() {
        return pushErrorMessage;
    }

    public void setPushErrorMessage(String pushErrorMessage) {
        this.pushErrorMessage = pushErrorMessage;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
