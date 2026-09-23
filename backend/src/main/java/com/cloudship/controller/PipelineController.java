package com.cloudship.controller;

import com.cloudship.dto.PipelineExecutionResponse;
import com.cloudship.dto.PipelineTriggerRequest;
import com.cloudship.entity.CITriggerType;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.service.PipelineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping("/projects/{projectId}/pipelines")
    public ResponseEntity<PipelineExecutionResponse> triggerPipeline(
            @PathVariable Long projectId,
            @Valid @RequestBody(required = false) PipelineTriggerRequest request) {
        log.info("Triggering Version 7 CI/CD Pipeline for project {}", projectId);
        PipelineExecutionResponse response = pipelineService.triggerPipeline(projectId, request, CITriggerType.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/pipelines")
    public ResponseEntity<List<PipelineExecutionResponse>> getProjectPipelines(@PathVariable Long projectId) {
        log.debug("Fetching pipeline history for project {}", projectId);
        return ResponseEntity.ok(pipelineService.getProjectPipelines(projectId));
    }

    @GetMapping("/projects/{projectId}/pipelines/{pipelineId}")
    public ResponseEntity<PipelineExecutionResponse> getProjectPipeline(
            @PathVariable Long projectId,
            @PathVariable Long pipelineId) {
        log.debug("Fetching pipeline #{} for project {}", pipelineId, projectId);
        PipelineExecutionResponse pipeline = pipelineService.getPipelineById(pipelineId);
        if (!pipeline.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Pipeline #" + pipelineId + " does not belong to project #" + projectId);
        }
        return ResponseEntity.ok(pipeline);
    }

    @GetMapping("/pipelines")
    public ResponseEntity<List<PipelineExecutionResponse>> getAllPipelines() {
        log.debug("Fetching all pipeline executions");
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    @GetMapping("/pipelines/{id}")
    public ResponseEntity<PipelineExecutionResponse> getPipelineById(@PathVariable Long id) {
        log.debug("Fetching pipeline execution #{}", id);
        return ResponseEntity.ok(pipelineService.getPipelineById(id));
    }

    @GetMapping("/pipelines/{id}/status")
    public ResponseEntity<PipelineExecutionResponse> getPipelineStatus(@PathVariable Long id) {
        log.debug("Checking live rollout and execution status for pipeline #{}", id);
        return ResponseEntity.ok(pipelineService.getPipelineStatus(id));
    }

    @PostMapping("/pipelines/{id}/cancel")
    public ResponseEntity<PipelineExecutionResponse> cancelPipeline(@PathVariable Long id) {
        log.info("Cancelling pipeline execution #{}", id);
        return ResponseEntity.ok(pipelineService.cancelPipeline(id));
    }
}
