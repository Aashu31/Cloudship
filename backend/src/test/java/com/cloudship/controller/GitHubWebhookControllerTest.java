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
}
