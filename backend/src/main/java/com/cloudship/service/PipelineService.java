package com.cloudship.service;

import com.cloudship.dto.*;
import com.cloudship.entity.*;
import com.cloudship.event.CIBuildStatusChangedEvent;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.PipelineExecutionRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.util.DockerImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private static final List<PipelineStatus> ACTIVE_STATUSES = List.of(
            PipelineStatus.QUEUED,
            PipelineStatus.CI_RUNNING,
            PipelineStatus.CI_SUCCESS,
            PipelineStatus.IMAGE_PUSHING,
            PipelineStatus.IMAGE_PUSHED,
            PipelineStatus.DEPLOYMENT_STARTING,
            PipelineStatus.DEPLOYING,
            PipelineStatus.ROLLOUT_VERIFYING
    );

    private final PipelineExecutionRepository pipelineExecutionRepository;
    private final ProjectRepository projectRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final CIBuildRepository ciBuildRepository;
    private final DeploymentRepository deploymentRepository;
    private final CIService ciService;
    private final AzureContainerRegistryService acrService;
    private final DeploymentService deploymentService;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public PipelineService(
            PipelineExecutionRepository pipelineExecutionRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            CIBuildRepository ciBuildRepository,
            DeploymentRepository deploymentRepository,
            CIService ciService,
            AzureContainerRegistryService acrService,
            DeploymentService deploymentService,
            TransactionTemplate transactionTemplate,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.pipelineExecutionRepository = pipelineExecutionRepository;
        this.projectRepository = projectRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.ciBuildRepository = ciBuildRepository;
        this.deploymentRepository = deploymentRepository;
        this.ciService = ciService;
        this.acrService = acrService;
        this.deploymentService = deploymentService;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    public PipelineService(
            PipelineExecutionRepository pipelineExecutionRepository,
            ProjectRepository projectRepository,
            GitRepositoryRepository gitRepositoryRepository,
            CIBuildRepository ciBuildRepository,
            DeploymentRepository deploymentRepository,
            CIService ciService,
            AzureContainerRegistryService acrService,
            DeploymentService deploymentService,
            TransactionTemplate transactionTemplate) {
        this(pipelineExecutionRepository, projectRepository, gitRepositoryRepository, ciBuildRepository,
                deploymentRepository, ciService, acrService, deploymentService, transactionTemplate, null);
    }

    private PipelineExecution saveAndPublish(PipelineExecution pipeline) {
        PipelineExecution saved = pipelineExecutionRepository.save(pipeline);
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new com.cloudship.event.PipelineStatusChangedEvent(saved));
            } catch (Exception e) {
                log.warn("Failed to publish PipelineStatusChangedEvent: {}", e.getMessage());
            }
        }
        return saved;
    }

    /**
     * Triggers an end-to-end Version 7 CI/CD Pipeline execution for a project.
     * Enforces concurrency guards to prevent duplicate overlapping runs.
     */
    public PipelineExecutionResponse triggerPipeline(Long projectId, PipelineTriggerRequest request, CITriggerType triggerType) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required to trigger a pipeline.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID '" + projectId + "' was not found"));
        assertProjectAccess(project);

        GitRepository repository = gitRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Project '" + project.getName() + "' does not have a connected Git repository. Connect a repository before triggering a pipeline."
                ));

        // 1. Concurrency guard: Check for active non-terminal pipeline
        Optional<PipelineExecution> activePipeline = pipelineExecutionRepository
                .findFirstByProjectIdAndStatusInOrderByCreatedAtDesc(projectId, ACTIVE_STATUSES);

        if (activePipeline.isPresent()) {
            PipelineExecution existing = activePipeline.get();
            throw new IllegalStateException(String.format(
                    "A pipeline execution is already in progress for project '%s' (Execution #%d, Status: %s). Wait for completion or cancel it before triggering another pipeline.",
                    project.getName(), existing.getId(), existing.getStatus()
            ));
        }

        String branch = (request != null && request.getBranch() != null && !request.getBranch().isBlank())
                ? request.getBranch().trim()
                : repository.getDefaultBranch();

        String commitSha = (request != null && request.getCommitSha() != null && !request.getCommitSha().isBlank())
                ? request.getCommitSha().trim()
                : null;

        String commitMessage = (request != null && request.getCommitMessage() != null && !request.getCommitMessage().isBlank())
                ? request.getCommitMessage().trim()
                : "Full CI/CD Pipeline Execution for " + branch;

        String commitAuthor = (request != null && request.getCommitAuthor() != null && !request.getCommitAuthor().isBlank())
                ? request.getCommitAuthor().trim()
                : "CloudShip Engineer";

        CITriggerType actualTriggerType = (triggerType != null) ? triggerType : CITriggerType.MANUAL;

        // 2. Initialize and persist QUEUED Pipeline Execution
        PipelineExecution pipeline = new PipelineExecution(project, branch, commitSha, actualTriggerType);
        pipeline.setCommitMessage(commitMessage);
        pipeline.setCommitAuthor(commitAuthor);
        if (request != null) {
            pipeline.setClusterName(request.getClusterName());
            pipeline.setNamespace(request.getNamespace() != null ? request.getNamespace() : "default");
        }

        PipelineExecution saved = transactionTemplate.execute(tx -> pipelineExecutionRepository.save(pipeline));
        if (saved == null) {
            throw new IllegalStateException("Failed to persist pipeline execution for project: " + project.getName());
        }

        log.info("Triggered Version 7 CI/CD Pipeline #{} for project '{}' [{}]", saved.getId(), project.getName(), actualTriggerType);

        // 3. Dispatch CI Stage (Jenkins build)
        try {
            CITriggerRequest ciRequest = new CITriggerRequest(branch, commitSha, commitMessage, commitAuthor);
            CIBuildResponse ciResponse = ciService.triggerBuild(projectId, ciRequest, actualTriggerType);

            CIBuild ciBuild = ciBuildRepository.findById(ciResponse.getId()).orElse(null);
            saved.setCiBuild(ciBuild);
            if (ciBuild != null) {
                saved.setImageName(ciBuild.getDockerImageName());
                saved.setImageTag(ciBuild.getDockerImageTag());
            }

            if (ciResponse.getStatus() == CIBuildStatus.FAILED) {
                saved.transitionTo(PipelineStatus.CI_FAILED, ciResponse.getErrorMessage());
                log.warn("Pipeline #{} CI stage failed to dispatch: {}", saved.getId(), ciResponse.getErrorMessage());
            } else {
                saved.transitionTo(PipelineStatus.CI_RUNNING, null);
                log.info("Pipeline #{} transitioned to CI_RUNNING with CI Build #{}", saved.getId(), ciResponse.getId());
            }
        } catch (Exception e) {
            log.error("Error dispatching CI build for Pipeline #{}: {}", saved.getId(), e.getMessage(), e);
            saved.transitionTo(PipelineStatus.CI_FAILED, "Failed to dispatch CI build: " + e.getMessage());
        }

        PipelineExecution finalPipeline = transactionTemplate.execute(tx -> saveAndPublish(saved));
        return PipelineExecutionResponse.fromEntity(finalPipeline != null ? finalPipeline : saved);
    }

    /**
     * Listens to CI build status transitions and orchestrates the downstream pipeline stages:
     * ACR Push -> Image Verification -> AKS Deployment -> Rollout Verification.
     */
    @EventListener
    public void onCIBuildStatusChanged(CIBuildStatusChangedEvent event) {
        if (event == null || event.getBuild() == null) {
            return;
        }
        handleCIBuildUpdate(event.getBuild());
    }

    public void handleCIBuildUpdate(CIBuild build) {
        if (build == null || build.getId() == null) {
            return;
        }

        Optional<PipelineExecution> pipelineOpt = pipelineExecutionRepository.findByCiBuildId(build.getId());
        if (pipelineOpt.isEmpty()) {
            return;
        }

        PipelineExecution pipeline = pipelineOpt.get();
        if (pipeline.getStatus().isTerminal()) {
            log.debug("Pipeline #{} is already terminal ({}); ignoring CI build update.", pipeline.getId(), pipeline.getStatus());
            return;
        }

        // Keep image info synchronized
        if (build.getDockerImageName() != null) pipeline.setImageName(build.getDockerImageName());
        if (build.getDockerImageTag() != null) pipeline.setImageTag(build.getDockerImageTag());
        if (build.getImageDigest() != null) pipeline.setImageDigest(build.getImageDigest());

        // A. Handle Image Push failure FIRST
        if (build.getPushStatus() == CIPushStatus.FAILED || (pipeline.getStatus() == PipelineStatus.IMAGE_PUSHING && (build.getStatus() == CIBuildStatus.FAILED || build.getStatus() == CIBuildStatus.ABORTED))) {
            String err = (build.getPushErrorMessage() != null && !build.getPushErrorMessage().isBlank())
                    ? build.getPushErrorMessage()
                    : ((build.getErrorMessage() != null && !build.getErrorMessage().isBlank()) ? build.getErrorMessage() : "Container image push to ACR failed");
            pipeline.transitionTo(PipelineStatus.IMAGE_PUSH_FAILED, err);
            saveAndPublish(pipeline);
            log.warn("Pipeline #{} marked IMAGE_PUSH_FAILED: {}", pipeline.getId(), err);
            return;
        }

        // B. Handle CI failure
        if (build.getStatus() == CIBuildStatus.FAILED || build.getStatus() == CIBuildStatus.ABORTED) {
            String err = (build.getErrorMessage() != null && !build.getErrorMessage().isBlank())
                    ? build.getErrorMessage()
                    : "Jenkins CI build #" + build.getJenkinsBuildNumber() + " failed";
            pipeline.transitionTo(PipelineStatus.CI_FAILED, err);
            saveAndPublish(pipeline);
            log.info("Pipeline #{} marked CI_FAILED", pipeline.getId());
            return;
        }

        // C. Handle Image Pushing status
        if (build.getPushStatus() == CIPushStatus.RUNNING) {
            if (pipeline.getStatus() == PipelineStatus.CI_RUNNING || pipeline.getStatus() == PipelineStatus.CI_SUCCESS) {
                pipeline.transitionTo(PipelineStatus.IMAGE_PUSHING, null);
                saveAndPublish(pipeline);
                log.info("Pipeline #{} transitioned to IMAGE_PUSHING", pipeline.getId());
            }
            return;
        }

        // D. Handle CI SUCCESS + ACR PUSH SUCCESS: Orchestrate Deployment
        if (build.getStatus() == CIBuildStatus.SUCCESS && build.getPushStatus() == CIPushStatus.SUCCESS) {
            if (pipeline.getStatus() == PipelineStatus.CI_RUNNING) {
                pipeline.transitionTo(PipelineStatus.CI_SUCCESS, null);
            }
            if (pipeline.getStatus() == PipelineStatus.CI_SUCCESS || pipeline.getStatus() == PipelineStatus.IMAGE_PUSHING) {
                pipeline.transitionTo(PipelineStatus.IMAGE_PUSHED, null);
            }
            saveAndPublish(pipeline);
            log.info("Pipeline #{} CI and ACR push succeeded. Initiating image verification and deployment.", pipeline.getId());

            // 1. Stage 4: Strict Image Verification & Digest Confirmation
            String imageName = build.getDockerImageName();
            String imageTag = build.getDockerImageTag();
            String imageDigest = build.getImageDigest();

            var verifyResp = acrService.verifyImage(imageName, imageTag, imageDigest);
            if (!verifyResp.isVerified()) {
                String errMsg = "Container image verification failed in ACR: " + verifyResp.getMessage();
                log.error("Pipeline #{} aborted before deployment: {}", pipeline.getId(), errMsg);
                pipeline.transitionTo(PipelineStatus.IMAGE_PUSH_FAILED, errMsg);
                saveAndPublish(pipeline);
                return;
            }

            // 2. Stage 5: AKS Deployment
            pipeline.transitionTo(PipelineStatus.DEPLOYMENT_STARTING, null);
            saveAndPublish(pipeline);

            try {
                DeploymentRequest depRequest = new DeploymentRequest();
                depRequest.setProjectId(pipeline.getProject().getId());
                depRequest.setCiBuildId(build.getId());
                depRequest.setImageName(imageName);
                depRequest.setImageTag(imageTag);
                depRequest.setImageDigest(imageDigest);
                depRequest.setClusterName(pipeline.getClusterName());
                depRequest.setNamespace(pipeline.getNamespace());

                DeploymentResponse depResponse = deploymentService.triggerDeployment(depRequest);

                Deployment deploymentEntity = deploymentRepository.findById(depResponse.getId()).orElse(null);
                pipeline.setDeployment(deploymentEntity);

                if (depResponse.getStatus() == DeploymentStatus.FAILED) {
                    pipeline.transitionTo(PipelineStatus.DEPLOYMENT_FAILED, depResponse.getErrorMessage());
                    log.warn("Pipeline #{} Kubernetes deployment failed: {}", pipeline.getId(), depResponse.getErrorMessage());
                } else {
                    pipeline.transitionTo(PipelineStatus.DEPLOYING, null);
                    pipeline.transitionTo(PipelineStatus.ROLLOUT_VERIFYING, null);

                    // 3. Stage 6: Immediate Rollout Status Inspection
                    if ("SUCCESS".equalsIgnoreCase(depResponse.getRolloutStatus()) || depResponse.getStatus() == DeploymentStatus.SUCCESS) {
                        pipeline.transitionTo(PipelineStatus.SUCCESS, null);
                        log.info("Pipeline #{} completed with full end-to-end SUCCESS!", pipeline.getId());
                    } else {
                        log.info("Pipeline #{} in ROLLOUT_VERIFYING waiting for pods to become ready", pipeline.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Pipeline #{} exception during Kubernetes deployment: {}", pipeline.getId(), e.getMessage(), e);
                pipeline.transitionTo(PipelineStatus.DEPLOYMENT_FAILED, "Kubernetes deployment error: " + e.getMessage());
            }

            saveAndPublish(pipeline);
        }
    }

    /**
     * Reconciles and returns the latest live status for a pipeline execution.
     * Evaluates rollout verification and timeout limits (180 seconds).
     */
    public PipelineExecutionResponse getPipelineStatus(Long id) {
        PipelineExecution pipeline = pipelineExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineExecution", id));

        if (pipeline.getStatus() == PipelineStatus.ROLLOUT_VERIFYING && pipeline.getDeployment() != null) {
            // Check timeout first (180s max for rollout)
            OffsetDateTime refTime = pipeline.getStartedAt() != null ? pipeline.getStartedAt() : pipeline.getCreatedAt();
            long elapsedSeconds = Duration.between(refTime, OffsetDateTime.now()).getSeconds();

            if (elapsedSeconds > 180) {
                log.warn("Pipeline #{} rollout timed out after {} seconds", id, elapsedSeconds);
                pipeline.transitionTo(PipelineStatus.ROLLOUT_FAILED, "Rollout verification timed out after " + elapsedSeconds + " seconds");
                pipeline = saveAndPublish(pipeline);
            } else {
                try {
                    DeploymentResponse rolloutStatus = deploymentService.getDeploymentRolloutStatus(pipeline.getDeployment().getId());
                    if (rolloutStatus.getStatus() == DeploymentStatus.SUCCESS || "SUCCESS".equalsIgnoreCase(rolloutStatus.getRolloutStatus())) {
                        pipeline.transitionTo(PipelineStatus.SUCCESS, null);
                        pipeline = saveAndPublish(pipeline);
                        log.info("Pipeline #{} rollout verified successfully -> SUCCESS", id);
                    } else if (rolloutStatus.getStatus() == DeploymentStatus.FAILED || "FAILED".equalsIgnoreCase(rolloutStatus.getRolloutStatus())) {
                        pipeline.transitionTo(PipelineStatus.ROLLOUT_FAILED, rolloutStatus.getErrorMessage());
                        pipeline = saveAndPublish(pipeline);
                        log.warn("Pipeline #{} rollout failed: {}", id, rolloutStatus.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.debug("Error checking rollout status for pipeline #{}: {}", id, e.getMessage());
                }
            }
        } else if (pipeline.getStatus() == PipelineStatus.CI_RUNNING && pipeline.getCiBuild() != null) {
            try {
                ciService.getBuild(pipeline.getCiBuild().getId());
                CIBuild refreshed = ciBuildRepository.findById(pipeline.getCiBuild().getId()).orElse(null);
                if (refreshed != null) {
                    handleCIBuildUpdate(refreshed);
                    pipeline = pipelineExecutionRepository.findById(id).orElse(pipeline);
                }
            } catch (Exception e) {
                log.debug("Error checking CI build status for pipeline #{}: {}", id, e.getMessage());
            }
        }

        return PipelineExecutionResponse.fromEntity(pipeline);
    }

    /**
     * Cancels an active pipeline execution.
     */
    public PipelineExecutionResponse cancelPipeline(Long id) {
        PipelineExecution pipeline = pipelineExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineExecution", id));

        if (pipeline.getProject() != null) {
            assertProjectAccess(pipeline.getProject());
        }

        if (pipeline.getStatus().isTerminal()) {
            throw new IllegalStateException(String.format(
                    "Cannot cancel pipeline #%d: It has already completed with terminal status '%s'.",
                    id, pipeline.getStatus()
            ));
        }

        pipeline.transitionTo(PipelineStatus.CANCELLED, "Pipeline cancelled by user");
        PipelineExecution saved = saveAndPublish(pipeline);
        log.info("Pipeline #{} was CANCELLED", id);
        return PipelineExecutionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PipelineExecutionResponse> getProjectPipelines(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with ID '" + projectId + "' was not found"));
        assertProjectAccess(project);

        return pipelineExecutionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(PipelineExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PipelineExecutionResponse> getAllPipelines() {
        java.util.Optional<com.cloudship.security.CloudshipPrincipal> principalOpt = com.cloudship.security.SecurityUtils.getCurrentPrincipal();
        List<PipelineExecution> pipelines = pipelineExecutionRepository.findAllByOrderByCreatedAtDesc();
        if (principalOpt.isPresent() && !principalOpt.get().isAdmin()) {
            Long userId = principalOpt.get().getId();
            pipelines = pipelines.stream()
                    .filter(p -> p.getProject() != null && p.getProject().getOwner() != null && userId.equals(p.getProject().getOwner().getId()))
                    .collect(Collectors.toList());
        }
        return pipelines.stream()
                .map(PipelineExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PipelineExecutionResponse getPipelineById(Long id) {
        PipelineExecution pipeline = pipelineExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineExecution", id));
        if (pipeline.getProject() != null) {
            assertProjectAccess(pipeline.getProject());
        }
        return PipelineExecutionResponse.fromEntity(pipeline);
    }

    private void assertProjectAccess(Project project) {
        com.cloudship.security.SecurityUtils.getCurrentPrincipal().ifPresent(principal -> {
            if (!principal.isAdmin() && (project.getOwner() == null || !project.getOwner().getId().equals(principal.getId()))) {
                throw new com.cloudship.exception.ForbiddenException("You do not have permission to access project with ID: " + project.getId());
            }
        });
    }
}
