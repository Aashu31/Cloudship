package com.cloudship.controller;

import com.cloudship.dto.PipelineExecutionResponse;
import com.cloudship.dto.PipelineTriggerRequest;
import com.cloudship.entity.CITriggerType;
import com.cloudship.entity.PipelineStatus;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PipelineController.class)
@ActiveProfiles("test")
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PipelineService pipelineService;

    @Test
    @DisplayName("POST /api/projects/{projectId}/pipelines triggers CI/CD pipeline and returns 201 CREATED")
    void testTriggerPipeline_Returns201() throws Exception {
        PipelineExecutionResponse resp = new PipelineExecutionResponse();
        resp.setId(10L);
        resp.setProjectId(1L);
        resp.setProjectName("cloudship-service");
        resp.setStatus(PipelineStatus.CI_RUNNING);
        resp.setStage("BUILD");
        resp.setBranch("main");
        resp.setCommitSha("abc1234");
        resp.setStartedAt(OffsetDateTime.now());

        when(pipelineService.triggerPipeline(eq(1L), any(PipelineTriggerRequest.class), eq(CITriggerType.MANUAL)))
                .thenReturn(resp);

        PipelineTriggerRequest req = new PipelineTriggerRequest("main", "abc1234", "CI trigger", "Aashu");

        mockMvc.perform(post("/api/projects/1/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.status").value("CI_RUNNING"))
                .andExpect(jsonPath("$.stage").value("BUILD"));
    }

    @Test
    @DisplayName("POST /api/projects/{projectId}/pipelines returns 404 when project not found")
    void testTriggerPipeline_ProjectNotFound_Returns404() throws Exception {
        when(pipelineService.triggerPipeline(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Project with ID '999' was not found"));

        mockMvc.perform(post("/api/projects/999/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/projects/{projectId}/pipelines returns 409 CONFLICT on concurrent run")
    void testTriggerPipeline_ConcurrentRun_Returns409() throws Exception {
        when(pipelineService.triggerPipeline(eq(1L), any(), any()))
                .thenThrow(new IllegalStateException("A pipeline execution is already in progress for project 'auth-service'"));

        mockMvc.perform(post("/api/projects/1/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in progress")));
    }

    @Test
    @DisplayName("GET /api/projects/{projectId}/pipelines returns pipeline history")
    void testGetProjectPipelines() throws Exception {
        PipelineExecutionResponse resp = new PipelineExecutionResponse();
        resp.setId(10L);
        resp.setProjectId(1L);
        resp.setStatus(PipelineStatus.SUCCESS);
        resp.setStage("LIVE");

        when(pipelineService.getProjectPipelines(1L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/projects/1/pipelines")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].stage").value("LIVE"));
    }

    @Test
    @DisplayName("GET /api/pipelines/{id}/status returns live pipeline rollout status")
    void testGetPipelineStatus() throws Exception {
        PipelineExecutionResponse resp = new PipelineExecutionResponse();
        resp.setId(10L);
        resp.setStatus(PipelineStatus.ROLLOUT_VERIFYING);
        resp.setStage("HEALTH_CHECK");

        when(pipelineService.getPipelineStatus(10L)).thenReturn(resp);

        mockMvc.perform(get("/api/pipelines/10/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("ROLLOUT_VERIFYING"))
                .andExpect(jsonPath("$.stage").value("HEALTH_CHECK"));
    }

    @Test
    @DisplayName("POST /api/pipelines/{id}/cancel cancels active pipeline")
    void testCancelPipeline() throws Exception {
        PipelineExecutionResponse resp = new PipelineExecutionResponse();
        resp.setId(10L);
        resp.setStatus(PipelineStatus.CANCELLED);
        resp.setStage("CANCELLED");

        when(pipelineService.cancelPipeline(10L)).thenReturn(resp);

        mockMvc.perform(post("/api/pipelines/10/cancel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
