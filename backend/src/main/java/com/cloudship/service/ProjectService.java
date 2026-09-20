package com.cloudship.service;

import com.cloudship.dto.ProjectRequest;
import com.cloudship.dto.ProjectResponse;
import com.cloudship.entity.Project;
import com.cloudship.exception.DuplicateResourceException;
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
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;

    public ProjectService(ProjectRepository projectRepository, DeploymentRepository deploymentRepository) {
        this.projectRepository = projectRepository;
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching all CloudShip projects");
        return projectRepository.findAll().stream()
                .map(project -> {
                    long deploymentCount = deploymentRepository.countByProjectId(project.getId());
                    return ProjectResponse.fromEntity(project, deploymentCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("Fetching project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        long deploymentCount = deploymentRepository.countByProjectId(project.getId());
        return ProjectResponse.fromEntity(project, deploymentCount);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        log.info("Creating new project with name: {}", request.getName());
        if (projectRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Project", "name", request.getName());
        }

        Project project = new Project(
                request.getName(),
                request.getDescription(),
                request.getRepositoryUrl()
        );

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());
        return ProjectResponse.fromEntity(savedProject, 0);
    }

    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with ID: {}", id);
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        projectRepository.deleteById(id);
        log.info("Project with ID: {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public long countProjects() {
        return projectRepository.count();
    }
}
