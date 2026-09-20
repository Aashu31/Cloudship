package com.cloudship.controller;

import com.cloudship.entity.GitConnectionStatus;
import com.cloudship.entity.Project;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.GitHubService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GitRepositoryRepository gitRepositoryRepository;

    @MockBean
    private GitHubService gitHubService;

    private Project testProject;

    @BeforeEach
    void setUp() {
        gitRepositoryRepository.deleteAll();
        projectRepository.deleteAll();

        testProject = new Project("repo-test-project", "Project for git testing", "https://github.com/org/repo.git");
        testProject = projectRepository.save(testProject);

        when(gitHubService.verifyRepository(anyString(), nullable(String.class)))
                .thenReturn(new GitHubService.GitHubMetadataResult(
                        "cloudship",
                        "demo-repo",
                        "main",
                        GitConnectionStatus.CONNECTED,
                        "Repository verified successfully"
                ));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/repository should connect repository successfully")
    void shouldConnectRepositorySuccessfully() throws Exception {
        String jsonPayload = """
            {
                "repositoryUrl": "https://github.com/cloudship/demo-repo",
                "defaultBranch": "main"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(testProject.getId()))
                .andExpect(jsonPath("$.provider").value("GITHUB"))
                .andExpect(jsonPath("$.owner").value("cloudship"))
                .andExpect(jsonPath("$.repositoryName").value("demo-repo"))
                .andExpect(jsonPath("$.defaultBranch").value("main"))
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/repository should return 400 for invalid GitHub URL")
    void shouldReturn400ForInvalidUrl() throws Exception {
        String jsonPayload = """
            {
                "repositoryUrl": "https://gitlab.com/invalid/repo",
                "defaultBranch": "main"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("GET /api/projects/{id}/repository should return 404 when project has no repository")
    void shouldReturn404WhenNoRepoConnected() throws Exception {
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/repository"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/repository should return repository when connected")
    void shouldReturnConnectedRepository() throws Exception {
        // First connect
        String jsonPayload = """
            {
                "repositoryUrl": "https://github.com/cloudship/demo-repo",
                "defaultBranch": "main"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // Then retrieve
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/repository"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("cloudship"))
                .andExpect(jsonPath("$.repositoryName").value("demo-repo"))
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id}/repository should disconnect repository")
    void shouldDisconnectRepository() throws Exception {
        // Connect first
        String jsonPayload = """
            {
                "repositoryUrl": "https://github.com/cloudship/demo-repo"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // Delete
        mockMvc.perform(delete("/api/projects/" + testProject.getId() + "/repository"))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/repository"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/projects/{id}/repository/status should return status check")
    void shouldReturnRepositoryStatus() throws Exception {
        // Connect first
        String jsonPayload = """
            {
                "repositoryUrl": "https://github.com/cloudship/demo-repo"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // Check status
        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/repository/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(testProject.getId()))
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.lastVerifiedAt").isNotEmpty());
    }
}
