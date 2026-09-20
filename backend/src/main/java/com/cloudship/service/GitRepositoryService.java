package com.cloudship.service;

import com.cloudship.dto.GitRepositoryRequest;
import com.cloudship.dto.GitRepositoryResponse;
import com.cloudship.dto.GitRepositoryStatusResponse;
import com.cloudship.entity.GitConnectionStatus;
import com.cloudship.entity.GitProvider;
import com.cloudship.entity.GitRepository;
import com.cloudship.entity.Project;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryService.class);

    private final GitRepositoryRepository gitRepositoryRepository;
    private final ProjectRepository projectRepository;
    private final GitHubService gitHubService;

    public GitRepositoryService(GitRepositoryRepository gitRepositoryRepository,
                                ProjectRepository projectRepository,
                                GitHubService gitHubService) {
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.projectRepository = projectRepository;
        this.gitHubService = gitHubService;
    }

    @Transactional(readOnly = true)
    public GitRepositoryResponse getRepository(Long projectId) {
        getProjectOrThrow(projectId);
        GitRepository repository = gitRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No repository connected for project with ID: " + projectId));
        return GitRepositoryResponse.fromEntity(repository);
    }

    @Transactional
    public GitRepositoryResponse connectRepository(Long projectId, GitRepositoryRequest request) {
        Project project = getProjectOrThrow(projectId);

        GitHubService.GitHubMetadataResult verification = gitHubService.verifyRepository(
                request.getRepositoryUrl(),
                request.getDefaultBranch()
        );

        GitRepository repository = gitRepositoryRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    GitRepository newRepo = new GitRepository();
                    newRepo.setProject(project);
                    return newRepo;
                });

        repository.setProvider(GitProvider.GITHUB);
        repository.setRepositoryUrl(request.getRepositoryUrl().trim());
        repository.setOwner(verification.getOwner());
        repository.setRepositoryName(verification.getRepositoryName());
        repository.setDefaultBranch(verification.getDefaultBranch());
        repository.setConnectionStatus(verification.getConnectionStatus());

        // Keep project repository URL synchronized
        project.setRepositoryUrl(request.getRepositoryUrl().trim());
        projectRepository.save(project);

        GitRepository saved = gitRepositoryRepository.save(repository);
        log.info("Connected GitHub repository {}/{} to project {} with status {}",
                saved.getOwner(), saved.getRepositoryName(), projectId, saved.getConnectionStatus());

        return GitRepositoryResponse.fromEntity(saved);
    }

    @Transactional
    public GitRepositoryResponse updateRepository(Long projectId, GitRepositoryRequest request) {
        return connectRepository(projectId, request);
    }

    @Transactional
    public void disconnectRepository(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        gitRepositoryRepository.findByProjectId(projectId).ifPresent(repo -> {
            project.setGitRepository(null);
            gitRepositoryRepository.delete(repo);
            gitRepositoryRepository.flush();
            log.info("Disconnected Git repository from project {}", projectId);
        });
    }

    @Transactional
    public GitRepositoryStatusResponse verifyRepositoryStatus(Long projectId) {
        getProjectOrThrow(projectId);
        GitRepository repository = gitRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No repository connected for project with ID: " + projectId));

        GitHubService.GitHubMetadataResult verification = gitHubService.verifyRepository(
                repository.getRepositoryUrl(),
                repository.getDefaultBranch()
        );

        repository.setConnectionStatus(verification.getConnectionStatus());
        if (verification.getConnectionStatus() == GitConnectionStatus.CONNECTED) {
            repository.setDefaultBranch(verification.getDefaultBranch());
        }
        gitRepositoryRepository.save(repository);

        return new GitRepositoryStatusResponse(
                projectId,
                repository.getRepositoryUrl(),
                repository.getOwner(),
                repository.getRepositoryName(),
                repository.getDefaultBranch(),
                repository.getConnectionStatus(),
                verification.getMessage()
        );
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID '" + projectId + "' was not found"));
    }
}
