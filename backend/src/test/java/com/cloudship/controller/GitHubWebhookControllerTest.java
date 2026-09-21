package com.cloudship.controller;

import com.cloudship.entity.GitConnectionStatus;
import com.cloudship.entity.GitRepository;
import com.cloudship.entity.Project;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.jenkins.JenkinsClient;
import com.cloudship.service.jenkins.JenkinsTriggerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GitRepositoryRepository gitRepositoryRepository;

    @Autowired
    private CIBuildRepository ciBuildRepository;

    @MockBean
    private JenkinsClient jenkinsClient;

    private Project testProject;

    @BeforeEach
    void setUp() {
        ciBuildRepository.deleteAll();
        gitRepositoryRepository.deleteAll();
        projectRepository.deleteAll();

        testProject = new Project("webhook-proj", "Project for webhook tests", "https://github.com/cloudship/webhook-demo.git");
        testProject = projectRepository.save(testProject);

        GitRepository repo = new GitRepository(testProject, "https://github.com/cloudship/webhook-demo", "cloudship", "webhook-demo", "main", GitConnectionStatus.CONNECTED);
        gitRepositoryRepository.save(repo);

        when(jenkinsClient.triggerJob(anyString(), any())).thenReturn(
                new JenkinsTriggerResult(true, 10, "http://localhost:8080/queue/item/10/", "Job triggered")
        );
    }

    @Test
    @DisplayName("POST /api/webhooks/github with ping event should return PONG")
    void shouldHandlePingEvent() throws Exception {
        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PONG"));
    }

    @Test
    @DisplayName("POST /api/webhooks/github with push for known repository should trigger CI build")
    void shouldTriggerCIOnPushEvent() throws Exception {
        String webhookPayload = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "clone_url": "https://github.com/cloudship/webhook-demo.git",
                    "html_url": "https://github.com/cloudship/webhook-demo"
                },
                "head_commit": {
                    "id": "e4f5a6b7c8d90123456789012345678901234567",
                    "message": "fix: resolve critical edge case",
                    "author": {
                        "name": "Octocat"
                    }
                }
            }
            """;

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.projectId").value(testProject.getId()))
                .andExpect(jsonPath("$.buildId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/webhooks/github with push for unknown repository should return IGNORED")
    void shouldIgnorePushForUnknownRepository() throws Exception {
        String webhookPayload = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "clone_url": "https://github.com/unknown/arbitrary-repo.git",
                    "html_url": "https://github.com/unknown/arbitrary-repo"
                }
            }
            """;

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));
    }

    @Test
    @DisplayName("POST /api/webhooks/github with empty payload should return 400")
    void shouldRejectEmptyWebhookPayload() throws Exception {
        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    // =========================================================================
    // HMAC-SHA256 Signature Verification Tests (Phase 3 Hardening)
    // =========================================================================

    private static final String TEST_WEBHOOK_SECRET = "super-secure-webhook-secret-xyz-987";

    private MockMvc securedMockMvc;

    @Autowired
    private com.cloudship.service.CIService ciService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void initSecuredController() {
        GitHubWebhookController securedController = new GitHubWebhookController(ciService, objectMapper, TEST_WEBHOOK_SECRET);
        this.securedMockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(securedController)
                .build();
    }

    private static String computeSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("sha256=");
            for (byte b : hmacBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Valid signature -> accepted with 200 OK")
    void shouldAcceptValidSignature() throws Exception {
        String payload = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "clone_url": "https://github.com/cloudship/webhook-demo.git",
                    "html_url": "https://github.com/cloudship/webhook-demo"
                }
            }
            """;
        String validSig = computeSignature(payload, TEST_WEBHOOK_SECRET);

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    @DisplayName("2. Invalid signature -> rejected with 401 Unauthorized")
    void shouldRejectInvalidSignature() throws Exception {
        String payload = "{\"ref\": \"refs/heads/main\"}";
        String invalidSig = "sha256=0000000000000000000000000000000000000000000000000000000000000000";

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", invalidSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid or missing webhook signature"));
    }

    @Test
    @DisplayName("3. Missing signature when secret is configured -> rejected with 401 Unauthorized")
    void shouldRejectMissingSignatureWhenSecretConfigured() throws Exception {
        String payload = "{\"ref\": \"refs/heads/main\"}";

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("4. Malformed signature header format -> rejected with 401 Unauthorized")
    void shouldRejectMalformedSignature() throws Exception {
        String payload = "{\"ref\": \"refs/heads/main\"}";
        // Missing "sha256=" prefix
        String malformedSig = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", malformedSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("5. Valid signature + unmapped repository -> returns IGNORED")
    void shouldReturnIgnoredForValidSignatureWithUnmappedRepository() throws Exception {
        String payload = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "clone_url": "https://github.com/unknown/nonexistent-project.git",
                    "html_url": "https://github.com/unknown/nonexistent-project"
                }
            }
            """;
        String validSig = computeSignature(payload, TEST_WEBHOOK_SECRET);

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No project configured")));
    }

    @Test
    @DisplayName("6. Valid signature + mapped repository -> triggers CI build")
    void shouldTriggerBuildForValidSignatureWithMappedRepository() throws Exception {
        String payload = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "clone_url": "https://github.com/cloudship/webhook-demo.git",
                    "html_url": "https://github.com/cloudship/webhook-demo"
                },
                "head_commit": {
                    "id": "11223344556677889900aabbccddeeff11223344",
                    "message": "feat: secured webhook trigger",
                    "author": { "name": "Security Bot" }
                }
            }
            """;
        String validSig = computeSignature(payload, TEST_WEBHOOK_SECRET);

        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.buildId").isNotEmpty())
                .andExpect(jsonPath("$.projectId").value(testProject.getId()));
    }

    @Test
    @DisplayName("7. Ping webhook with valid signature -> returns PONG (and ping without signature rejected)")
    void shouldHandlePingWithValidSignatureAndRejectWithout() throws Exception {
        String pingPayload = "{\"zen\": \"Security is paramount\"}";
        String validSig = computeSignature(pingPayload, TEST_WEBHOOK_SECRET);

        // Valid signature on ping -> 200 PONG
        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PONG"));

        // Missing signature on ping when secret is configured -> 401 Unauthorized
        securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("8. Webhook secret never appears in response body")
    void shouldNeverExposeWebhookSecretInResponse() throws Exception {
        String payload = "{\"ref\": \"refs/heads/main\"}";
        String invalidSig = "sha256=badbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbad1";

        org.springframework.test.web.servlet.MvcResult result = securedMockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", invalidSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(
                responseBody.contains(TEST_WEBHOOK_SECRET),
                "Webhook response must NEVER expose the configured webhook secret"
        );
    }
}
