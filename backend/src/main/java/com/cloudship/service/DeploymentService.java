package com.cloudship.service;

import com.cloudship.dto.DeploymentResponse;
import com.cloudship.entity.Deployment;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;

    public DeploymentService(DeploymentRepository deploymentRepository, ProjectRepository projectRepository) {
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
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
}
