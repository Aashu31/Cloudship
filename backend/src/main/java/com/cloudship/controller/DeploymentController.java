package com.cloudship.controller;

import com.cloudship.dto.DeploymentRequest;
import com.cloudship.dto.DeploymentResponse;
import com.cloudship.service.DeploymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DeploymentController {

    private static final Logger log = LoggerFactory.getLogger(DeploymentController.class);

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping("/api/projects/{projectId}/deployments")
    public ResponseEntity<List<DeploymentResponse>> getDeploymentsByProjectId(@PathVariable Long projectId) {
        log.info("Received request to list deployments for project id: {}", projectId);
        List<DeploymentResponse> deployments = deploymentService.getDeploymentsByProjectId(projectId);
        return ResponseEntity.ok(deployments);
    }

    @PostMapping("/api/projects/{projectId}/deployments")
    public ResponseEntity<DeploymentResponse> triggerProjectDeployment(
            @PathVariable Long projectId,
            @RequestBody(required = false) DeploymentRequest request) {
        if (request == null) {
            request = new DeploymentRequest(projectId);
        } else {
            request.setProjectId(projectId);
        }
        log.info("Received request to trigger deployment for project id: {}", projectId);
        DeploymentResponse response = deploymentService.triggerDeployment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/deployments/{id}")
    public ResponseEntity<DeploymentResponse> getDeploymentById(@PathVariable Long id) {
        log.info("Received request to fetch deployment id: {}", id);
        DeploymentResponse deployment = deploymentService.getDeploymentById(id);
        return ResponseEntity.ok(deployment);
    }

    @GetMapping("/api/deployments/{id}/status")
    public ResponseEntity<DeploymentResponse> getDeploymentRolloutStatus(@PathVariable Long id) {
        log.info("Received request to poll rollout status for deployment id: {}", id);
        DeploymentResponse deployment = deploymentService.getDeploymentRolloutStatus(id);
        return ResponseEntity.ok(deployment);
    }

    @GetMapping("/api/deployments")
    public ResponseEntity<List<DeploymentResponse>> getAllDeployments() {
        log.info("Received request to list all deployments");
        List<DeploymentResponse> deployments = deploymentService.getAllDeployments();
        return ResponseEntity.ok(deployments);
    }

    @PostMapping("/api/deployments")
    public ResponseEntity<DeploymentResponse> triggerDeployment(@Valid @RequestBody DeploymentRequest request) {
        log.info("Received request to trigger deployment for project: {}", request.getProjectId());
        DeploymentResponse response = deploymentService.triggerDeployment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
