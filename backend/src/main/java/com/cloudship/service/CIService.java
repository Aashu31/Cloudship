package com.cloudship.service;

import com.cloudship.dto.CIBuildResponse;
import com.cloudship.dto.CIBuildStatusUpdateRequest;
import com.cloudship.dto.CITriggerRequest;
import com.cloudship.dto.GitHubWebhookResponse;
import com.cloudship.entity.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.jenkins.JenkinsBuildDetails;
import com.cloudship.service.jenkins.JenkinsClient;
import com.cloudship.service.jenkins.JenkinsTriggerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CIService {

    private static final Logger log = LoggerFactory.getLogger(CIService.class);

    private final CIBuildRepository ciBuildRepository;
    private final ProjectRepository projectRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final JenkinsClient jenkinsClient;
    private final String defaultJobName;
    private final com.cloudship.config.AzureProperties azureProperties;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public CIService(
            CIBuildRepository ciBuildRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            JenkinsClient jenkinsClient,
            @Value("${cloudship.jenkins.default-job-name:cloudship-ci}") String defaultJobName) {
        this(ciBuildRepository, projectRepository, gitRepositoryRepository, jenkinsClient, defaultJobName, new com.cloudship.config.AzureProperties(), null);
    }

    public CIService(
            CIBuildRepository ciBuildRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            JenkinsClient jenkinsClient,
            @Value("${cloudship.jenkins.default-job-name:cloudship-ci}") String defaultJobName,
            com.cloudship.config.AzureProperties azureProperties) {
        this(ciBuildRepository, projectRepository, gitRepositoryRepository, jenkinsClient, defaultJobName, azureProperties, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CIService(
            CIBuildRepository ciBuildRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            JenkinsClient jenkinsClient,
            @Value("${cloudship.jenkins.default-job-name:cloudship-ci}") String defaultJobName,
            com.cloudship.config.AzureProperties azureProperties,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.ciBuildRepository = ciBuildRepository;
        this.projectRepository = projectRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.jenkinsClient = jenkinsClient;
        this.defaultJobName = defaultJobName;
        this.azureProperties = azureProperties != null ? azureProperties : new com.cloudship.config.AzureProperties();
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CIBuildResponse triggerBuild(Long projectId, CITriggerRequest request, CITriggerType triggerType) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID '" + projectId + "' was not found"));

        GitRepository repository = gitRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Project '" + project.getName() + "' does not have a connected Git repository. Connect a repository before triggering CI."
                ));

        String branch = (request != null && request.getBranch() != null && !request.getBranch().isBlank())
                ? request.getBranch().trim()
                : repository.getDefaultBranch();

        String commitSha = (request != null && request.getCommitSha() != null && !request.getCommitSha().isBlank())
                ? request.getCommitSha().trim()
                : null;

        String commitMessage = (request != null && request.getCommitMessage() != null && !request.getCommitMessage().isBlank())
                ? request.getCommitMessage().trim()
                : "Continuous Integration Build for " + branch;

        String commitAuthor = (request != null && request.getCommitAuthor() != null && !request.getCommitAuthor().isBlank())
                ? request.getCommitAuthor().trim()
                : "CloudShip Engineer";

        CITriggerType actualTriggerType = (triggerType != null) ? triggerType : CITriggerType.MANUAL;

        String dockerImageTag = com.cloudship.util.DockerImageValidator.constructImageTag(commitSha, branch, null);
        String dockerImageName = "cloudship/backend";

        CIBuild build = new CIBuild();
        build.setProject(project);
        build.setGitRepository(repository);
        build.setBranch(branch);
        build.setCommitSha(commitSha);
        build.setTriggerType(actualTriggerType);
        build.setStatus(CIBuildStatus.QUEUED);
        build.setStartedAt(OffsetDateTime.now());
        build.setCommitMessage(commitMessage);
        build.setCommitAuthor(commitAuthor);
        build.setJenkinsJobName(defaultJobName);
        build.setDockerImageName(dockerImageName);
        build.setDockerImageTag(dockerImageTag);
        build.setRegistryName(azureProperties.getAcrName());
        build.setRegistryLoginServer(azureProperties.resolveAcrLoginServer());
        build.setPushStatus(com.cloudship.entity.CIPushStatus.NOT_STARTED);

        // Pre-save build so an ID is generated before dispatching to Jenkins
        CIBuild saved = ciBuildRepository.save(build);

        // Attempt triggering Jenkins
        Map<String, String> params = new HashMap<>();
        params.put("GIT_URL", repository.getRepositoryUrl());
        params.put("BRANCH_NAME", branch);
        params.put("GIT_COMMIT", commitSha != null ? commitSha : "HEAD");
        params.put("PROJECT_ID", String.valueOf(projectId));
        params.put("CLOUDSHIP_BUILD_ID", String.valueOf(saved.getId()));
        params.put("DOCKER_IMAGE_NAME", dockerImageName);
        params.put("DOCKER_IMAGE_TAG", dockerImageTag);
        params.put("ACR_NAME", azureProperties.getAcrName() != null ? azureProperties.getAcrName() : "");
        params.put("ACR_LOGIN_SERVER", azureProperties.resolveAcrLoginServer() != null ? azureProperties.resolveAcrLoginServer() : "");

        JenkinsTriggerResult triggerResult = jenkinsClient.triggerJob(defaultJobName, params);

        if (triggerResult.isSuccess()) {
            saved.setStatus(CIBuildStatus.RUNNING);
            saved.setJenkinsBuildNumber(triggerResult.getBuildNumber());
            log.info("CI Build queued in Jenkins for project '{}' [Build ID: {}]", project.getName(), saved.getId());
        } else {
            saved.setStatus(CIBuildStatus.FAILED);
            saved.setCompletedAt(OffsetDateTime.now());
            saved.setDurationMs(0L);
            saved.setErrorMessage(triggerResult.getMessage());
            log.warn("CI Build for project '{}' failed to dispatch to Jenkins: {}", project.getName(), triggerResult.getMessage());
        }

        saved = ciBuildRepository.save(saved);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.cloudship.event.CIBuildCreatedEvent(saved));
        }
        return CIBuildResponse.fromEntity(saved);
    }

    @Transactional
    public List<CIBuildResponse> getProjectBuilds(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with ID '" + projectId + "' was not found");
        }
        return ciBuildRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::reconcileBuildStatusIfRunning)
                .map(CIBuildResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CIBuildResponse getProjectBuild(Long projectId, Long buildId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with ID '" + projectId + "' was not found");
        }
        CIBuild build = ciBuildRepository.findByIdAndProjectId(buildId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CI Build with ID '" + buildId + "' not found for project '" + projectId + "'"
                ));
        build = reconcileBuildStatusIfRunning(build);
        return CIBuildResponse.fromEntity(build);
    }

    @Transactional
    public CIBuildResponse getBuild(Long id) {
        CIBuild build = ciBuildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CI Build with ID '" + id + "' was not found"));
        build = reconcileBuildStatusIfRunning(build);
        return CIBuildResponse.fromEntity(build);
    }

    @Transactional
    public CIBuildResponse updateBuildStatus(Long id, CIBuildStatusUpdateRequest request) {
        CIBuild build = ciBuildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CI Build with ID '" + id + "' was not found"));

        if (request.getStatus() != null) {
            build.setStatus(request.getStatus());
            if (request.getStatus() == CIBuildStatus.SUCCESS
                    || request.getStatus() == CIBuildStatus.FAILED
                    || request.getStatus() == CIBuildStatus.ABORTED) {
                if (build.getCompletedAt() == null) {
                    build.setCompletedAt(OffsetDateTime.now());
                }
            }
        }
        if (request.getJenkinsBuildNumber() != null) {
            build.setJenkinsBuildNumber(request.getJenkinsBuildNumber());
        }
        if (request.getDurationMs() != null) {
            build.setDurationMs(request.getDurationMs());
        } else if (build.getDurationMs() == null && build.getStartedAt() != null && build.getCompletedAt() != null) {
            build.setDurationMs(Duration.between(build.getStartedAt(), build.getCompletedAt()).toMillis());
        }
        if (request.getDockerImageTag() != null && !request.getDockerImageTag().isBlank()) {
            build.setDockerImageTag(request.getDockerImageTag());
        }
        if (request.getErrorMessage() != null) {
            build.setErrorMessage(request.getErrorMessage());
        }
        if (request.getPushStatus() != null) {
            build.setPushStatus(request.getPushStatus());
            if (request.getPushStatus() == com.cloudship.entity.CIPushStatus.RUNNING && build.getPushStartedAt() == null) {
                build.setPushStartedAt(OffsetDateTime.now());
            }
            if ((request.getPushStatus() == com.cloudship.entity.CIPushStatus.SUCCESS
                    || request.getPushStatus() == com.cloudship.entity.CIPushStatus.FAILED
                    || request.getPushStatus() == com.cloudship.entity.CIPushStatus.SKIPPED)
                    && build.getPushCompletedAt() == null) {
                build.setPushCompletedAt(OffsetDateTime.now());
            }
        }
        if (request.getPushDurationMs() != null) {
            build.setPushDurationMs(request.getPushDurationMs());
        }
        if (request.getPushErrorMessage() != null) {
            build.setPushErrorMessage(com.cloudship.util.DockerImageValidator.sanitizeErrorMessage(request.getPushErrorMessage()));
        }
        if (request.getImageDigest() != null && !request.getImageDigest().isBlank()) {
            build.setImageDigest(request.getImageDigest().trim());
        }
        if (request.getRegistryName() != null && !request.getRegistryName().isBlank()) {
            build.setRegistryName(request.getRegistryName().trim());
        }
        if (request.getRegistryLoginServer() != null && !request.getRegistryLoginServer().isBlank()) {
            build.setRegistryLoginServer(request.getRegistryLoginServer().trim());
        }

        CIBuild saved = ciBuildRepository.save(build);
        log.info("Updated CI Build {} status to {} (pushStatus: {})", id, saved.getStatus(), saved.getPushStatus());
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.cloudship.event.CIBuildStatusChangedEvent(saved));
        }
        return CIBuildResponse.fromEntity(saved);
    }

    @Transactional
    public GitHubWebhookResponse handleGitHubWebhook(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return new GitHubWebhookResponse("FAILED", "Empty webhook payload", null, null);
        }

        // Extract repository URL
        String repoUrl = extractRepoUrlFromWebhook(payload);
        if (repoUrl == null) {
            return new GitHubWebhookResponse("FAILED", "Could not determine repository URL from webhook payload", null, null);
        }

        // Find connected repository
        Optional<GitRepository> repoOpt = findMatchingRepository(repoUrl);
        if (repoOpt.isEmpty()) {
            log.info("Webhook received for unregistered repository URL: {}", repoUrl);
            return new GitHubWebhookResponse("IGNORED", "No project configured for repository: " + repoUrl, null, null);
        }

        GitRepository repository = repoOpt.get();
        Project project = repository.getProject();

        // Extract branch
        String ref = (String) payload.get("ref");
        String branch = (ref != null && ref.startsWith("refs/heads/"))
                ? ref.substring("refs/heads/".length())
                : repository.getDefaultBranch();

        // Extract head commit info
        String commitSha = null;
        String commitMessage = null;
        String commitAuthor = null;

        @SuppressWarnings("unchecked")
        Map<String, Object> headCommit = (Map<String, Object>) payload.get("head_commit");
        if (headCommit != null) {
            commitSha = (String) headCommit.get("id");
            commitMessage = (String) headCommit.get("message");
            @SuppressWarnings("unchecked")
            Map<String, Object> author = (Map<String, Object>) headCommit.get("author");
            if (author != null) {
                commitAuthor = (String) author.get("name");
            }
        }

        CITriggerRequest triggerRequest = new CITriggerRequest(branch, commitSha, commitMessage, commitAuthor);
        CIBuildResponse buildResponse = triggerBuild(project.getId(), triggerRequest, CITriggerType.WEBHOOK);

        log.info("Dispatched webhook CI build {} for project '{}'", buildResponse.getId(), project.getName());
        return new GitHubWebhookResponse("PROCESSED", "CI build triggered successfully", buildResponse.getId(), project.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getJenkinsStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean available = jenkinsClient.isAvailable();
        status.put("available", available);
        status.put("baseUrl", jenkinsClient.getBaseUrl());
        status.put("defaultJobName", defaultJobName);
        status.put("connectionStatus", available ? "CONNECTED" : "UNAVAILABLE");
        return status;
    }

    private CIBuild reconcileBuildStatusIfRunning(CIBuild build) {
        if (build.getStatus() == CIBuildStatus.RUNNING && build.getJenkinsBuildNumber() != null) {
            try {
                String jobName = build.getJenkinsJobName() != null ? build.getJenkinsJobName() : defaultJobName;
                JenkinsBuildDetails details = jenkinsClient.getBuildDetails(jobName, build.getJenkinsBuildNumber());
                if (details != null && details.getStatus() != null && details.getStatus() != CIBuildStatus.RUNNING) {
                    build.setStatus(details.getStatus());
                    if (build.getCompletedAt() == null) {
                        build.setCompletedAt(OffsetDateTime.now());
                    }
                    if (details.getDurationMs() > 0) {
                        build.setDurationMs(details.getDurationMs());
                    } else if (build.getDurationMs() == null && build.getStartedAt() != null && build.getCompletedAt() != null) {
                        build.setDurationMs(Duration.between(build.getStartedAt(), build.getCompletedAt()).toMillis());
                    }
                    build = ciBuildRepository.save(build);
                    log.info("Reconciled running CI Build {} with Jenkins status: {}", build.getId(), build.getStatus());
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(new com.cloudship.event.CIBuildStatusChangedEvent(build));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to reconcile CI build {} status from Jenkins: {}", build.getId(), e.getMessage());
            }
        }
        return build;
    }

    private Optional<GitRepository> findMatchingRepository(String targetUrl) {
        String normalizedTarget = normalizeRepoUrl(targetUrl);
        return gitRepositoryRepository.findAll().stream()
                .filter(r -> normalizeRepoUrl(r.getRepositoryUrl()).equalsIgnoreCase(normalizedTarget))
                .findFirst();
    }

    private String normalizeRepoUrl(String url) {
        if (url == null) return "";
        return url.trim()
                .replaceAll("^https?://", "")
                .replaceAll("^git@github\\.com:", "github.com/")
                .replaceAll("\\.git$", "")
                .toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private String extractRepoUrlFromWebhook(Map<String, Object> payload) {
        Map<String, Object> repoObj = (Map<String, Object>) payload.get("repository");
        if (repoObj != null) {
            if (repoObj.get("clone_url") != null) {
                return (String) repoObj.get("clone_url");
            }
            if (repoObj.get("html_url") != null) {
                return (String) repoObj.get("html_url");
            }
            if (repoObj.get("url") != null) {
                return (String) repoObj.get("url");
            }
        }
        return null;
    }
}
