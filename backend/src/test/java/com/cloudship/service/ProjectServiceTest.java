package com.cloudship.service;

import com.cloudship.dto.ProjectRequest;
import com.cloudship.dto.ProjectResponse;
import com.cloudship.entity.Project;
import com.cloudship.exception.DuplicateResourceException;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.DeploymentRepository;
import com.cloudship.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = new Project("Sample App", "A sample service", "https://github.com/org/sample-app");
        sampleProject.setId(1L);
    }

    @Test
    void shouldCreateProjectWhenNameIsUnique() {
        ProjectRequest request = new ProjectRequest("Sample App", "A sample service", "https://github.com/org/sample-app");

        when(projectRepository.existsByName("Sample App")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectResponse response = projectService.createProject(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Sample App");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenNameAlreadyExists() {
        ProjectRequest request = new ProjectRequest("Sample App", "Description", "https://github.com/org/sample-app");

        when(projectRepository.existsByName("Sample App")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Project already exists with name 'Sample App'");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenProjectDoesNotExist() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project with ID '99' was not found");
    }

    @Test
    void shouldDeleteProjectWhenExists() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        doNothing().when(projectRepository).deleteById(1L);

        projectService.deleteProject(1L);

        verify(projectRepository).deleteById(1L);
    }
}
