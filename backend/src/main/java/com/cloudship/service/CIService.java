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
import com.cloudship.service.jenkins.JenkinsClient;
import com.cloudship.service.jenkins.JenkinsTriggerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CIService(
            CIBuildRepository ciBuildRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            JenkinsClient jenkinsClient,
            @Value("${cloudship.jenkins.default-job-name:cloudship-ci}") String defaultJobName) {
        this.ciBuildRepository = ciBuildRepository;
        this.projectRepository = projectRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.jenkinsClient = jenkinsClient;
        this.defaultJobName = defaultJobName;
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
                : generateSimulatedSha();

        String commitMessage = (request != null && request.getCommitMessage() != null && !request.getCommitMessage().isBlank())
                ? request.getCommitMessage().trim()
                : "Continuous Integration Build for " + branch;

        String commitAuthor = (request != null && request.getCommitAuthor() != null && !request.getCommitAuthor().isBlank())
                ? request.getCommitAuthor().trim()
                : "CloudShip Engineer";

        CITriggerType actualTriggerType = (triggerType != null) ? triggerType : CITriggerType.MANUAL;

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
        build.setDockerImageName("cloudship/backend");
        build.setDockerImageTag(commitSha.length() >= 7 ? commitSha.substring(0, 7) : commitSha);

        // Attempt triggering Jenkins
        Map<String, String> params = new HashMap<>();
        params.put("GIT_URL", repository.getRepositoryUrl());
        params.put("BRANCH_NAME", branch);
        params.put("GIT_COMMIT", commitSha);
        params.put("PROJECT_ID", String.valueOf(projectId));

        JenkinsTriggerResult triggerResult = jenkinsClient.triggerJob(defaultJobName, params);

        if (triggerResult.isSuccess()) {
            build.setStatus(CIBuildStatus.RUNNING);
            build.setJenkinsBuildNumber(triggerResult.getBuildNumber());
            log.info("CI Build queued in Jenkins for project '{}' [Build ID: {}]", project.getName(), build.getId());
        } else {
            build.setStatus(CIBuildStatus.FAILED);
            build.setCompletedAt(OffsetDateTime.now());
            build.setDurationMs(0L);
            build.setErrorMessage(triggerResult.getMessage());
            log.warn("CI Build for project '{}' failed to dispatch to Jenkins: {}", project.getName(), triggerResult.getMessage());
        }

        CIBuild saved = ciBuildRepository.save(build);
        return CIBuildResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CIBuildResponse> getProjectBuilds(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with ID '" + projectId + "' was not found");
        }
        return ciBuildRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(CIBuildResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CIBuildResponse getProjectBuild(Long projectId, Long buildId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with ID '" + projectId + "' was not found");
        }
        CIBuild build = ciBuildRepository.findByIdAndProjectId(buildId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CI Build with ID '" + buildId + "' not found for project '" + projectId + "'"
                ));
        return CIBuildResponse.fromEntity(build);
    }

    @Transactional(readOnly = true)
    public CIBuildResponse getBuild(Long id) {
        CIBuild build = ciBuildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CI Build with ID '" + id + "' was not found"));
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
        }
        if (request.getDockerImageTag() != null && !request.getDockerImageTag().isBlank()) {
            build.setDockerImageTag(request.getDockerImageTag());
        }
        if (request.getErrorMessage() != null) {
            build.setErrorMessage(request.getErrorMessage());
        }

        CIBuild saved = ciBuildRepository.save(build);
        log.info("Updated CI Build {} status to {}", id, saved.getStatus());
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

    private String generateSimulatedSha() {
        String part1 = UUID.randomUUID().toString().replace("-", "");
        String part2 = UUID.randomUUID().toString().replace("-", "");
        return (part1 + part2).substring(0, 40);
    }
}
