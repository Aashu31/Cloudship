package com.cloudship.controller;

import com.cloudship.dto.DeploymentResponse;
import com.cloudship.service.DeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping("/api/deployments/{id}")
    public ResponseEntity<DeploymentResponse> getDeploymentById(@PathVariable Long id) {
        log.info("Received request to fetch deployment id: {}", id);
        DeploymentResponse deployment = deploymentService.getDeploymentById(id);
        return ResponseEntity.ok(deployment);
    }

    @GetMapping("/api/deployments")
    public ResponseEntity<List<DeploymentResponse>> getAllDeployments() {
        log.info("Received request to list all deployments");
        List<DeploymentResponse> deployments = deploymentService.getAllDeployments();
        return ResponseEntity.ok(deployments);
    }
}
