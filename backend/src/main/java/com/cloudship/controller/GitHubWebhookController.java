package com.cloudship.controller;

import com.cloudship.dto.GitHubWebhookResponse;
import com.cloudship.service.CIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final CIService ciService;

    public GitHubWebhookController(CIService ciService) {
        this.ciService = ciService;
    }

    @PostMapping("/github")
    public ResponseEntity<GitHubWebhookResponse> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "push") String event,
            @RequestBody(required = false) Map<String, Object> payload) {
        log.info("Received GitHub webhook event: {}", event);

        if ("ping".equalsIgnoreCase(event)) {
            return ResponseEntity.ok(new GitHubWebhookResponse("PONG", "CloudShip GitHub webhook listener is active", null, null));
        }

        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(new GitHubWebhookResponse("FAILED", "Empty webhook payload received", null, null));
        }

        GitHubWebhookResponse response = ciService.handleGitHubWebhook(payload);
        return ResponseEntity.ok(response);
    }
}
