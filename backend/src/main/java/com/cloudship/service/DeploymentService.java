package com.cloudship.service;

import com.cloudship.dto.DeploymentRequest;
import com.cloudship.dto.DeploymentResponse;
import com.cloudship.entity.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.azure.AzureClientProvider;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.KubernetesDeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final CIBuildRepository ciBuildRepository;
    private final AzureClientProvider azureClientProvider;
    private final AzureContainerRegistryService acrService;
    private final KubernetesDeploymentService kubernetesDeploymentService;

    public DeploymentService(
            DeploymentRepository deploymentRepository,
            ProjectRepository projectRepository,
            CIBuildRepository ciBuildRepository,
            AzureClientProvider azureClientProvider,
            AzureContainerRegistryService acrService,
            KubernetesDeploymentService kubernetesDeploymentService) {
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
        this.ciBuildRepository = ciBuildRepository;
        this.azureClientProvider = azureClientProvider;
        this.acrService = acrService;
        this.kubernetesDeploymentService = kubernetesDeploymentService;
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> getDeploymentsByProjectId(Long projectId) {
        log.debug("Fetching deployments for project ID: {}", projectId);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(DeploymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeploymentResponse getDeploymentById(Long id) {
        log.debug("Fetching deployment with ID: {}", id);
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));
        return DeploymentResponse.fromEntity(deployment);
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> getAllDeployments() {
        log.debug("Fetching all deployments");
        return deploymentRepository.findAll().stream()
                .map(DeploymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countDeployments() {
        return deploymentRepository.count();
    }

    @Transactional
    public DeploymentResponse triggerDeployment(DeploymentRequest request) {
        if (request == null || request.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required to trigger a deployment.");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        log.info("Triggering rolling Kubernetes deployment for project '{}' (ID: {})", project.getName(), project.getId());

        // 1. Resolve verified container image from CIBuild or request
        CIBuild targetBuild = null;
        String imageName = null;
        String imageTag = null;
        String imageDigest = null;

        if (request.getCiBuildId() != null) {
            targetBuild = ciBuildRepository.findByIdAndProjectId(request.getCiBuildId(), project.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("CIBuild", request.getCiBuildId()));

            if (targetBuild.getPushStatus() != CIPushStatus.SUCCESS) {
                throw new IllegalStateException(String.format(
                        "Cannot deploy build #%d: Container image push status is '%s'. Only images verified and pushed to the registry can be deployed.",
                        targetBuild.getId(), targetBuild.getPushStatus()
                ));
            }
            imageName = targetBuild.getDockerImageName();
            imageTag = targetBuild.getDockerImageTag();
            imageDigest = targetBuild.getImageDigest();
        } else {
            // Find the latest successful push for this project
            Optional<CIBuild> latestPushed = ciBuildRepository.findFirstByProjectIdAndPushStatusOrderByCreatedAtDesc(
                    project.getId(), CIPushStatus.SUCCESS);

            if (latestPushed.isPresent()) {
                targetBuild = latestPushed.get();
                imageName = targetBuild.getDockerImageName();
                imageTag = targetBuild.getDockerImageTag();
                imageDigest = targetBuild.getImageDigest();
                log.info("Auto-selected latest successfully pushed build #{} (image: {}:{})", targetBuild.getId(), imageName, imageTag);
            } else if (request.getImageName() != null && !request.getImageName().isBlank()) {
                // User supplied image: must verify against ACR
                imageName = request.getImageName().trim();
                imageTag = (request.getImageTag() != null && !request.getImageTag().isBlank()) ? request.getImageTag().trim() : "latest";
                imageDigest = request.getImageDigest();
                log.info("Verifying user-specified image '{}:{}' against ACR", imageName, imageTag);
                var verifyResp = acrService.verifyImage(imageName, imageTag, imageDigest);
                if (!verifyResp.isVerified()) {
                    throw new IllegalStateException("Cannot deploy: " + verifyResp.getMessage());
                }
            } else {
                throw new IllegalStateException(String.format(
                        "Cannot deploy project '%s': No verified container image found in registry. Trigger a CI build to build and push an image first.",
                        project.getName()
                ));
            }
        }

        // 2. Resolve deployment parameters
        String clusterName = (request.getClusterName() != null && !request.getClusterName().isBlank())
                ? request.getClusterName().trim()
                : azureClientProvider.getAksClusterName();

        String namespace = (request.getNamespace() != null && !request.getNamespace().isBlank())
                ? request.getNamespace().trim()
                : azureClientProvider.getK8sNamespace();
        if (namespace == null || namespace.isBlank()) {
            namespace = "default";
        }

        String sanitizedProject = project.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String depName = (request.getDeploymentName() != null && !request.getDeploymentName().isBlank())
                ? request.getDeploymentName().trim()
                : sanitizedProject;

        String svcName = (request.getServiceName() != null && !request.getServiceName().isBlank())
                ? request.getServiceName().trim()
                : depName + "-service";

        int replicas = (request.getReplicas() != null && request.getReplicas() >= 1) ? request.getReplicas() : 1;
        String version = (imageTag != null && !imageTag.isBlank()) ? imageTag : "v" + (deploymentRepository.count() + 1);

        // 3. Create and persist Deployment entity
        Deployment deployment = new Deployment();
        deployment.setProject(project);
        deployment.setCiBuild(targetBuild);
        deployment.setVersion(version);
        deployment.setStatus(DeploymentStatus.RUNNING);
        deployment.setRolloutStatus("RUNNING");
        deployment.setClusterName(clusterName);
        deployment.setNamespace(namespace);
        deployment.setDeploymentName(depName);
        deployment.setServiceName(svcName);
        deployment.setImageName(imageName);
        deployment.setImageTag(imageTag);
        deployment.setImageDigest(imageDigest);
        deployment.setReplicas(replicas);
        deployment.setReadyReplicas(0);
        deployment.setUpdatedReplicas(0);
        deployment.setAvailableReplicas(0);
        deployment.setStartedAt(OffsetDateTime.now());

        Deployment saved = deploymentRepository.save(deployment);

        // 4. Apply Kubernetes Deployment & Service resources
        try {
            kubernetesDeploymentService.applyDeployment(saved);
        } catch (Exception e) {
            log.error("Failed to execute Kubernetes deployment for project '{}': {}", project.getName(), e.getMessage(), e);
            saved.setStatus(DeploymentStatus.FAILED);
            saved.setRolloutStatus("FAILED");
            saved.setErrorMessage("Deployment error: " + e.getMessage());
            saved.setCompletedAt(OffsetDateTime.now());
        }

        Deployment finalDeployment = deploymentRepository.save(saved);
        return DeploymentResponse.fromEntity(finalDeployment);
    }

    @Transactional
    public DeploymentResponse getDeploymentRolloutStatus(Long id) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));

        if (deployment.getStatus() == DeploymentStatus.RUNNING || "RUNNING".equalsIgnoreCase(deployment.getRolloutStatus())) {
            kubernetesDeploymentService.checkRolloutStatus(deployment);
            deployment = deploymentRepository.save(deployment);
        }

        return DeploymentResponse.fromEntity(deployment);
    }
}
