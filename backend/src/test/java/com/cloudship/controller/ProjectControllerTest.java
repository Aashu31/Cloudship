package com.cloudship.controller;

import com.cloudship.dto.ProjectRequest;
import com.cloudship.dto.ProjectResponse;
import com.cloudship.exception.GlobalExceptionHandler;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @Test
    void shouldReturnAllProjects() throws Exception {
        ProjectResponse p1 = new ProjectResponse(1L, "App One", "First app", "https://github.com/org/app1", 0, OffsetDateTime.now(), OffsetDateTime.now());
        when(projectService.getAllProjects()).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("App One"));
    }

    @Test
    void shouldReturnProjectByIdWhenFound() throws Exception {
        ProjectResponse p1 = new ProjectResponse(1L, "App One", "First app", "https://github.com/org/app1", 0, OffsetDateTime.now(), OffsetDateTime.now());
        when(projectService.getProjectById(1L)).thenReturn(p1);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("App One"));
    }

    @Test
    void shouldReturn404WhenProjectNotFound() throws Exception {
        when(projectService.getProjectById(99L)).thenThrow(new ResourceNotFoundException("Project", 99L));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project with ID '99' was not found"));
    }

    @Test
    void shouldCreateProjectSuccessfully() throws Exception {
        ProjectRequest request = new ProjectRequest("New Project", "New project description", "https://github.com/org/new-proj");
        ProjectResponse created = new ProjectResponse(10L, "New Project", "New project description", "https://github.com/org/new-proj", 0, OffsetDateTime.now(), OffsetDateTime.now());

        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("New Project"));
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        // Missing name and invalid URL
        ProjectRequest invalidRequest = new ProjectRequest("", "Description", "invalid-url");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldDeleteProjectSuccessfully() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentProject() throws Exception {
        doThrow(new ResourceNotFoundException("Project", 99L)).when(projectService).deleteProject(99L);

        mockMvc.perform(delete("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
