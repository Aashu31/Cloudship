package com.cloudship.controller;

import com.cloudship.dto.CIBuildResponse;
import com.cloudship.dto.CIBuildStatusUpdateRequest;
import com.cloudship.dto.CITriggerRequest;
import com.cloudship.entity.CITriggerType;
import com.cloudship.exception.UnauthorizedException;
import com.cloudship.service.CIService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CIBuildController {

    private static final Logger log = LoggerFactory.getLogger(CIBuildController.class);

    private final CIService ciService;
    private final String webhookSecret;

    public CIBuildController(
            CIService ciService,
            @Value("${cloudship.jenkins.webhook-secret:}") String webhookSecret) {
        this.ciService = ciService;
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
    }

    @GetMapping("/projects/{projectId}/ci-builds")
    public ResponseEntity<List<CIBuildResponse>> getProjectBuilds(@PathVariable Long projectId) {
        log.info("Fetching CI builds for project {}", projectId);
        return ResponseEntity.ok(ciService.getProjectBuilds(projectId));
    }

    @GetMapping("/projects/{projectId}/ci-builds/{buildId}")
    public ResponseEntity<CIBuildResponse> getProjectBuild(
            @PathVariable Long projectId,
            @PathVariable Long buildId) {
        log.info("Fetching CI build {} for project {}", buildId, projectId);
        return ResponseEntity.ok(ciService.getProjectBuild(projectId, buildId));
    }

    @GetMapping("/ci-builds/{id}")
    public ResponseEntity<CIBuildResponse> getBuild(@PathVariable Long id) {
        log.info("Fetching CI build {}", id);
        return ResponseEntity.ok(ciService.getBuild(id));
    }

    @PostMapping("/projects/{projectId}/ci-builds")
    public ResponseEntity<CIBuildResponse> triggerCIBuild(
            @PathVariable Long projectId,
            @Valid @RequestBody(required = false) CITriggerRequest request) {
        log.info("Triggering CI build for project {}", projectId);
        CIBuildResponse response = ciService.triggerBuild(projectId, request, CITriggerType.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/projects/{projectId}/ci/build")
    public ResponseEntity<CIBuildResponse> triggerCIBuildAlias(
            @PathVariable Long projectId,
            @Valid @RequestBody(required = false) CITriggerRequest request) {
        log.info("Triggering CI build (alias) for project {}", projectId);
        CIBuildResponse response = ciService.triggerBuild(projectId, request, CITriggerType.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/ci-builds/{id}")
    public ResponseEntity<CIBuildResponse> updateBuildStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-CloudShip-CI-Token", required = false) String ciToken,
            @RequestBody CIBuildStatusUpdateRequest request) {
        log.info("Updating status for CI build {}", id);

        if (!webhookSecret.isEmpty()) {
            byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
            byte[] actual = (ciToken != null ? ciToken.trim() : "").getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                log.warn("Unauthorized status update attempt on CI build {} - invalid or missing X-CloudShip-CI-Token", id);
                throw new UnauthorizedException("Invalid or missing CI authorization token");
            }
        }

        CIBuildResponse response = ciService.updateBuildStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jenkins/status")
    public ResponseEntity<Map<String, Object>> getJenkinsStatus() {
        log.info("Checking Jenkins CI status");
        return ResponseEntity.ok(ciService.getJenkinsStatus());
    }
}
