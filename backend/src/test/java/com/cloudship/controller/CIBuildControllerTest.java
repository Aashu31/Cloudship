package com.cloudship.controller;

import com.cloudship.entity.*;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CIBuildControllerTest {

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
    private GitRepository testRepo;

    @BeforeEach
    void setUp() {
        ciBuildRepository.deleteAll();
        gitRepositoryRepository.deleteAll();
        projectRepository.deleteAll();

        testProject = new Project("ci-test-project", "Project for CI testing", "https://github.com/cloudship/ci-demo.git");
        testProject = projectRepository.save(testProject);

        testRepo = new GitRepository(testProject, "https://github.com/cloudship/ci-demo", "cloudship", "ci-demo", "main", GitConnectionStatus.CONNECTED);
        testRepo = gitRepositoryRepository.save(testRepo);

        when(jenkinsClient.isAvailable()).thenReturn(true);
        when(jenkinsClient.getBaseUrl()).thenReturn("http://localhost:8080");
        when(jenkinsClient.triggerJob(anyString(), any())).thenReturn(
                new JenkinsTriggerResult(true, 101, "http://localhost:8080/queue/item/1/", "Job triggered successfully")
        );
    }

    @Test
    @DisplayName("POST /api/projects/{id}/ci-builds should trigger build successfully when Jenkins is available")
    void shouldTriggerBuildSuccessfullyWhenJenkinsAvailable() throws Exception {
        String payload = """
            {
                "branch": "main",
                "commitSha": "a1b2c3d4e5f6789012345678901234567890abcd",
                "commitMessage": "feat: test ci build",
                "commitAuthor": "Developer"
            }
            """;

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(testProject.getId()))
                .andExpect(jsonPath("$.branch").value("main"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.dockerImageName").value("cloudship/backend"))
                .andExpect(jsonPath("$.dockerImageTag").value("a1b2c3d"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/ci-builds should record failed build when Jenkins is unavailable")
    void shouldRecordFailedBuildWhenJenkinsUnavailable() throws Exception {
        when(jenkinsClient.triggerJob(anyString(), any())).thenReturn(
                new JenkinsTriggerResult(false, null, null, "Jenkins unavailable at http://localhost:8080")
        );

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Jenkins unavailable at http://localhost:8080"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/projects/{id}/ci-builds should return 400 when project has no repository")
    void shouldReturn400WhenNoRepositoryConnected() throws Exception {
        gitRepositoryRepository.deleteAll();

        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/ci-builds should return 404 when project does not exist")
    void shouldReturn404WhenProjectNotFound() throws Exception {
        mockMvc.perform(post("/api/projects/9999/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/ci-builds should list builds for project")
    void shouldListProjectBuilds() throws Exception {
        // Trigger one build first
        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/ci-builds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectId").value(testProject.getId()));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/ci-builds/{buildId} should get build by id")
    void shouldGetProjectBuildById() throws Exception {
        var createResult = mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        Long buildId = com.jayway.jsonpath.JsonPath.parse(responseJson).read("$.id", Long.class);

        mockMvc.perform(get("/api/projects/" + testProject.getId() + "/ci-builds/" + buildId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buildId))
                .andExpect(jsonPath("$.projectId").value(testProject.getId()));
    }

    @Test
    @DisplayName("PATCH /api/ci-builds/{id} should update build status and docker tag")
    void shouldUpdateBuildStatus() throws Exception {
        var createResult = mockMvc.perform(post("/api/projects/" + testProject.getId() + "/ci-builds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long buildId = com.jayway.jsonpath.JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

        String updatePayload = """
            {
                "status": "SUCCESS",
                "jenkinsBuildNumber": 42,
                "durationMs": 35000,
                "dockerImageTag": "a1b2c3d"
            }
            """;

        mockMvc.perform(patch("/api/ci-builds/" + buildId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.jenkinsBuildNumber").value(42))
                .andExpect(jsonPath("$.durationMs").value(35000))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/jenkins/status should return Jenkins connectivity status")
    void shouldReturnJenkinsStatus() throws Exception {
        mockMvc.perform(get("/api/jenkins/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.baseUrl").value("http://localhost:8080"));
    }
}
