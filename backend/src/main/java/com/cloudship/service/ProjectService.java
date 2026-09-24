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

import com.cloudship.exception.ForbiddenException;
import com.cloudship.repository.UserRepository;
import com.cloudship.security.CloudshipPrincipal;
import com.cloudship.security.SecurityUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                          DeploymentRepository deploymentRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.deploymentRepository = deploymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching CloudShip projects");
        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();

        List<Project> projects;
        if (principalOpt.isPresent() && !principalOpt.get().isAdmin()) {
            // Regular user: only list their own projects
            projects = projectRepository.findByOwnerId(principalOpt.get().getId());
        } else {
            // Admin or internal system task: list all projects
            projects = projectRepository.findAll();
        }

        return projects.stream()
                .map(project -> {
                    long deploymentCount = deploymentRepository.countByProjectId(project.getId());
                    return ProjectResponse.fromEntity(project, deploymentCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("Fetching project with id: {}", id);
        Project project = getProjectAndVerifyAccess(id);
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

        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
        if (principalOpt.isPresent()) {
            userRepository.findById(principalOpt.get().getId())
                    .ifPresent(project::setOwner);
        }

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {} and owner: {}",
                savedProject.getId(), savedProject.getOwner() != null ? savedProject.getOwner().getEmail() : "unassigned");
        return ProjectResponse.fromEntity(savedProject, 0);
    }

    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with ID: {}", id);
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
            if (principalOpt.isPresent()) {
                CloudshipPrincipal principal = principalOpt.get();
                if (!principal.isAdmin() && (project.getOwner() == null || !project.getOwner().getId().equals(principal.getId()))) {
                    log.warn("IDOR attempt blocked on delete: User {} tried to delete project {}", principal.getEmail(), id);
                    throw new ForbiddenException("You do not have permission to access project with ID: " + id);
                }
            }
            projectRepository.delete(project);
        } else if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Project", id);
        }
        log.info("Project with ID: {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public Project getProjectAndVerifyAccess(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
        if (principalOpt.isPresent()) {
            CloudshipPrincipal principal = principalOpt.get();
            if (!principal.isAdmin() && (project.getOwner() == null || !project.getOwner().getId().equals(principal.getId()))) {
                log.warn("IDOR attempt blocked: User {} tried to access project {} owned by {}",
                        principal.getEmail(), id, project.getOwner() != null ? project.getOwner().getId() : "none");
                throw new ForbiddenException("You do not have permission to access project with ID: " + id);
            }
        }

        return project;
    }

    @Transactional(readOnly = true)
    public long countProjects() {
        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
        if (principalOpt.isPresent() && !principalOpt.get().isAdmin()) {
            return projectRepository.findByOwnerId(principalOpt.get().getId()).size();
        }
        return projectRepository.count();
    }
}
