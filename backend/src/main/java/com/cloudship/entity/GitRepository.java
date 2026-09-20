package com.cloudship.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "git_repositories")
public class GitRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GitProvider provider = GitProvider.GITHUB;

    @Column(name = "repository_url", nullable = false, length = 255)
    private String repositoryUrl;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(name = "repository_name", nullable = false, length = 100)
    private String repositoryName;

    @Column(name = "default_branch", nullable = false, length = 100)
    private String defaultBranch = "main";

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 50)
    private GitConnectionStatus connectionStatus = GitConnectionStatus.NOT_CONNECTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public GitRepository() {
    }

    public GitRepository(Project project, String repositoryUrl, String owner, String repositoryName, String defaultBranch, GitConnectionStatus connectionStatus) {
        this.project = project;
        this.repositoryUrl = repositoryUrl;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.connectionStatus = connectionStatus != null ? connectionStatus : GitConnectionStatus.NOT_CONNECTED;
        this.provider = GitProvider.GITHUB;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.defaultBranch == null || this.defaultBranch.isBlank()) {
            this.defaultBranch = "main";
        }
        if (this.provider == null) {
            this.provider = GitProvider.GITHUB;
        }
        if (this.connectionStatus == null) {
            this.connectionStatus = GitConnectionStatus.NOT_CONNECTED;
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

    public GitProvider getProvider() {
        return provider;
    }

    public void setProvider(GitProvider provider) {
        this.provider = provider;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public GitConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(GitConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
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
