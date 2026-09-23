package com.cloudship.controller;

import com.cloudship.dto.DeploymentRequest;
import com.cloudship.dto.DeploymentResponse;
import com.cloudship.entity.DeploymentStatus;
import com.cloudship.service.DeploymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentControllerV6Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeploymentService deploymentService;

    @Test
    @DisplayName("POST /api/deployments triggers rolling deployment and returns 201 Created")
    void testTriggerDeployment() throws Exception {
        DeploymentRequest request = new DeploymentRequest(1L, 10L, 2);
        request.setClusterName("aks-cloudship-dev");
        request.setNamespace("default");

        DeploymentResponse response = new DeploymentResponse(
                50L, 1L, "auth-service", "v1", DeploymentStatus.RUNNING,
                OffsetDateTime.now(), null
        );
        response.setCiBuildId(10L);
        response.setClusterName("aks-cloudship-dev");
        response.setNamespace("default");
        response.setDeploymentName("auth-service");
        response.setServiceName("auth-service-service");
        response.setImageName("cloudshipcr.azurecr.io/cloudship/auth-service");
        response.setImageTag("build-10");
        response.setReplicas(2);
        response.setRolloutStatus("RUNNING");

        when(deploymentService.triggerDeployment(any(DeploymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.ciBuildId").value(10))
                .andExpect(jsonPath("$.clusterName").value("aks-cloudship-dev"))
                .andExpect(jsonPath("$.deploymentName").value("auth-service"))
                .andExpect(jsonPath("$.replicas").value(2))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.rolloutStatus").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/projects/{projectId}/deployments triggers deployment for specific project")
    void testTriggerProjectDeployment() throws Exception {
        DeploymentResponse response = new DeploymentResponse(
                51L, 2L, "payment-service", "v2", DeploymentStatus.RUNNING,
                OffsetDateTime.now(), null
        );
        response.setRolloutStatus("RUNNING");

        when(deploymentService.triggerDeployment(any(DeploymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/projects/2/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.projectId").value(2));
    }

    @Test
    @DisplayName("POST /api/projects/{projectId}/deployments with unverified image returns 409 CONFLICT")
    void testTriggerDeploymentInvalidStateReturns409() throws Exception {
        when(deploymentService.triggerDeployment(any(DeploymentRequest.class)))
                .thenThrow(new IllegalStateException(
                        "Cannot deploy build #10: Container image push status is 'FAILED'. Only images verified and pushed to the registry can be deployed."));

        mockMvc.perform(post("/api/projects/2/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only images verified and pushed")));
    }

    @Test
    @DisplayName("GET /api/deployments/{id}/status returns rollout status")
    void testGetDeploymentRolloutStatus() throws Exception {
        DeploymentResponse response = new DeploymentResponse(
                50L, 1L, "auth-service", "v1", DeploymentStatus.SUCCESS,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        response.setRolloutStatus("SUCCESS");
        response.setReadyReplicas(2);
        response.setAvailableReplicas(2);

        when(deploymentService.getDeploymentRolloutStatus(50L)).thenReturn(response);

        mockMvc.perform(get("/api/deployments/50/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.rolloutStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.readyReplicas").value(2));
    }
}
