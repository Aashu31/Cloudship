package com.cloudship.controller;

import com.cloudship.dto.GitRepositoryRequest;
import com.cloudship.dto.GitRepositoryResponse;
import com.cloudship.dto.GitRepositoryStatusResponse;
import com.cloudship.service.GitRepositoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/repository")
public class GitRepositoryController {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryController.class);

    private final GitRepositoryService gitRepositoryService;

    public GitRepositoryController(GitRepositoryService gitRepositoryService) {
        this.gitRepositoryService = gitRepositoryService;
    }

    @GetMapping
    public ResponseEntity<GitRepositoryResponse> getRepository(@PathVariable Long projectId) {
        log.info("Fetching Git repository for project {}", projectId);
        GitRepositoryResponse response = gitRepositoryService.getRepository(projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<GitRepositoryResponse> connectRepository(
            @PathVariable Long projectId,
            @Valid @RequestBody GitRepositoryRequest request) {
        log.info("Connecting Git repository for project {}: {}", projectId, request.getRepositoryUrl());
        GitRepositoryResponse response = gitRepositoryService.connectRepository(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<GitRepositoryResponse> updateRepository(
            @PathVariable Long projectId,
            @Valid @RequestBody GitRepositoryRequest request) {
        log.info("Updating Git repository for project {}: {}", projectId, request.getRepositoryUrl());
        GitRepositoryResponse response = gitRepositoryService.updateRepository(projectId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> disconnectRepository(@PathVariable Long projectId) {
        log.info("Disconnecting Git repository from project {}", projectId);
        gitRepositoryService.disconnectRepository(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<GitRepositoryStatusResponse> getRepositoryStatus(@PathVariable Long projectId) {
        log.info("Verifying Git repository status for project {}", projectId);
        GitRepositoryStatusResponse response = gitRepositoryService.verifyRepositoryStatus(projectId);
        return ResponseEntity.ok(response);
    }
}
