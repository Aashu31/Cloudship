package com.cloudship.service;

import com.cloudship.dto.CIBuildResponse;
import com.cloudship.dto.CIBuildStatusUpdateRequest;
import com.cloudship.dto.CITriggerRequest;
import com.cloudship.dto.GitHubWebhookResponse;
import com.cloudship.entity.*;
import com.cloudship.exception.ResourceNotFoundException;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.repository.GitRepositoryRepository;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.service.jenkins.JenkinsClient;
import com.cloudship.service.jenkins.JenkinsTriggerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CIServiceTest {

    @Mock
    private CIBuildRepository ciBuildRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GitRepositoryRepository gitRepositoryRepository;

    @Mock
    private JenkinsClient jenkinsClient;

    private CIService ciService;

    private Project testProject;
    private GitRepository testRepo;

    @BeforeEach
    void setUp() {
        ciService = new CIService(ciBuildRepository, projectRepository, gitRepositoryRepository, jenkinsClient, "cloudship-ci");

        testProject = new Project("demo-proj", "A demo project", "https://github.com/cloudship/demo.git");
        testProject.setId(1L);

        testRepo = new GitRepository(testProject, "https://github.com/cloudship/demo", "cloudship", "demo", "main", GitConnectionStatus.CONNECTED);
        testRepo.setId(10L);
    }

    @Test
    @DisplayName("triggerBuild should dispatch job to Jenkins and record running build")
    void shouldTriggerBuildSuccessfully() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(gitRepositoryRepository.findByProjectId(1L)).thenReturn(Optional.of(testRepo));
        when(jenkinsClient.triggerJob(anyString(), any())).thenReturn(
                new JenkinsTriggerResult(true, 55, "http://jenkins:8080/queue/item/55/", "Dispatched")
        );
        when(ciBuildRepository.save(any(CIBuild.class))).thenAnswer(invocation -> {
            CIBuild b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        CITriggerRequest req = new CITriggerRequest("main", "1234567890abcdef1234567890abcdef12345678", "test commit", "Dev");
        CIBuildResponse resp = ciService.triggerBuild(1L, req, CITriggerType.MANUAL);

        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        assertEquals(CIBuildStatus.RUNNING, resp.getStatus());
        assertEquals("cloudship/backend", resp.getDockerImageName());
        assertEquals("1234567", resp.getDockerImageTag());
        verify(jenkinsClient, times(1)).triggerJob(eq("cloudship-ci"), any());
    }

    @Test
    @DisplayName("triggerBuild should throw IllegalArgumentException when repository not connected")
    void shouldThrowWhenNoRepositoryConnected() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(gitRepositoryRepository.findByProjectId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ciService.triggerBuild(1L, null, CITriggerType.MANUAL));
        verify(jenkinsClient, never()).triggerJob(anyString(), any());
    }

    @Test
    @DisplayName("triggerBuild should throw ResourceNotFoundException when project does not exist")
    void shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ciService.triggerBuild(99L, null, CITriggerType.MANUAL));
    }

    @Test
    @DisplayName("updateBuildStatus should update status and completion time")
    void shouldUpdateBuildStatus() {
        CIBuild existing = new CIBuild(testProject, testRepo, "main", "abc", CITriggerType.MANUAL);
        existing.setId(200L);

        when(ciBuildRepository.findById(200L)).thenReturn(Optional.of(existing));
        when(ciBuildRepository.save(any(CIBuild.class))).thenAnswer(inv -> inv.getArgument(0));

        CIBuildStatusUpdateRequest req = new CIBuildStatusUpdateRequest(CIBuildStatus.SUCCESS, 60, 45000L, "tag-abc", null);
        CIBuildResponse resp = ciService.updateBuildStatus(200L, req);

        assertEquals(CIBuildStatus.SUCCESS, resp.getStatus());
        assertEquals(60, resp.getJenkinsBuildNumber());
        assertEquals(45000L, resp.getDurationMs());
        assertEquals("tag-abc", resp.getDockerImageTag());
        assertNotNull(resp.getCompletedAt());
    }

    @Test
    @DisplayName("handleGitHubWebhook should trigger CI for matching repository URL")
    void shouldHandleWebhookForMatchingRepository() {
        when(gitRepositoryRepository.findAll()).thenReturn(List.of(testRepo));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(gitRepositoryRepository.findByProjectId(1L)).thenReturn(Optional.of(testRepo));
        when(jenkinsClient.triggerJob(anyString(), any())).thenReturn(
                new JenkinsTriggerResult(true, 1, "http://jenkins/queue/1/", "Success")
        );
        when(ciBuildRepository.save(any(CIBuild.class))).thenAnswer(inv -> {
            CIBuild b = inv.getArgument(0);
            b.setId(300L);
            return b;
        });

        Map<String, Object> payload = new HashMap<>();
        payload.put("ref", "refs/heads/main");
        Map<String, Object> repoMap = new HashMap<>();
        repoMap.put("clone_url", "https://github.com/cloudship/demo.git");
        payload.put("repository", repoMap);
        Map<String, Object> commitMap = new HashMap<>();
        commitMap.put("id", "abcdef1234567890abcdef1234567890abcdef12");
        commitMap.put("message", "push test");
        Map<String, Object> authorMap = new HashMap<>();
        authorMap.put("name", "Alice");
        commitMap.put("author", authorMap);
        payload.put("head_commit", commitMap);

        GitHubWebhookResponse resp = ciService.handleGitHubWebhook(payload);

        assertEquals("PROCESSED", resp.getStatus());
        assertEquals(300L, resp.getBuildId());
        assertEquals(1L, resp.getProjectId());
    }

    @Test
    @DisplayName("getJenkinsStatus should report current connection state")
    void shouldReportJenkinsStatus() {
        when(jenkinsClient.isAvailable()).thenReturn(true);
        when(jenkinsClient.getBaseUrl()).thenReturn("http://localhost:8080");

        Map<String, Object> status = ciService.getJenkinsStatus();
        assertEquals(true, status.get("available"));
        assertEquals("CONNECTED", status.get("connectionStatus"));
        assertEquals("http://localhost:8080", status.get("baseUrl"));
        assertEquals("cloudship-ci", status.get("defaultJobName"));
    }
}
