package com.cloudship.controller;

import com.cloudship.dto.GitHubWebhookResponse;
import com.cloudship.service.CIService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final CIService ciService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public GitHubWebhookController(
            CIService ciService,
            ObjectMapper objectMapper,
            @Value("${cloudship.jenkins.webhook-secret:}") String webhookSecret) {
        this.ciService = ciService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
    }

    @PostMapping("/github")
    public ResponseEntity<GitHubWebhookResponse> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "push") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody(required = false) byte[] rawBody) {
        log.info("Received GitHub webhook event: {}", event);

        // Verify HMAC-SHA256 signature when webhook secret is configured
        if (!verifySignature(rawBody, signatureHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GitHubWebhookResponse("UNAUTHORIZED", "Invalid or missing webhook signature", null, null));
        }

        if ("ping".equalsIgnoreCase(event)) {
            return ResponseEntity.ok(new GitHubWebhookResponse("PONG", "CloudShip GitHub webhook listener is active", null, null));
        }

        if (rawBody == null || rawBody.length == 0) {
            return ResponseEntity.badRequest().body(new GitHubWebhookResponse("FAILED", "Empty webhook payload received", null, null));
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse GitHub webhook JSON payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new GitHubWebhookResponse("FAILED", "Malformed JSON payload: " + e.getMessage(), null, null));
        }

        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(new GitHubWebhookResponse("FAILED", "Empty webhook payload received", null, null));
        }

        GitHubWebhookResponse response = ciService.handleGitHubWebhook(payload);
        return ResponseEntity.ok(response);
    }

    private boolean verifySignature(byte[] rawBody, String signatureHeader) {
        if (webhookSecret.isEmpty()) {
            log.warn("GitHub webhook received but webhook secret is not configured; processing request without signature verification. Set JENKINS_WEBHOOK_SECRET for production environments.");
            return true;
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Rejecting GitHub webhook: missing X-Hub-Signature-256 header when webhook secret is configured.");
            return false;
        }

        if (!signatureHeader.startsWith("sha256=")) {
            log.warn("Rejecting GitHub webhook: malformed X-Hub-Signature-256 header format.");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(rawBody != null ? rawBody : new byte[0]);

            StringBuilder hexString = new StringBuilder("sha256=");
            for (byte b : hmacBytes) {
                hexString.append(String.format("%02x", b));
            }

            String expectedSignature = hexString.toString();

            // Constant-time comparison to prevent timing attacks
            boolean matches = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                log.warn("Rejecting GitHub webhook: signature mismatch for X-Hub-Signature-256.");
            }

            return matches;
        } catch (Exception e) {
            log.error("Error computing HMAC SHA-256 signature for GitHub webhook", e);
            return false;
        }
    }
}
