package com.cloudship.controller;

import com.cloudship.dto.DeploymentResponse;
import com.cloudship.entity.DeploymentStatus;
import com.cloudship.exception.GlobalExceptionHandler;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DeploymentController.class)
@Import(GlobalExceptionHandler.class)
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeploymentService deploymentService;

    @Test
    void shouldReturnDeploymentsForProject() throws Exception {
        DeploymentResponse dep = new DeploymentResponse(
                100L, 1L, "App One", "v1.0.0", DeploymentStatus.SUCCESS, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(deploymentService.getDeploymentsByProjectId(1L)).thenReturn(List.of(dep));

        mockMvc.perform(get("/api/projects/1/deployments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].version").value("v1.0.0"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void shouldReturnDeploymentById() throws Exception {
        DeploymentResponse dep = new DeploymentResponse(
                100L, 1L, "App One", "v1.0.0", DeploymentStatus.SUCCESS, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(deploymentService.getDeploymentById(100L)).thenReturn(dep);

        mockMvc.perform(get("/api/deployments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.version").value("v1.0.0"));
    }

    @Test
    void shouldReturn404WhenDeploymentNotFound() throws Exception {
        when(deploymentService.getDeploymentById(999L)).thenThrow(new ResourceNotFoundException("Deployment", 999L));

        mockMvc.perform(get("/api/deployments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
