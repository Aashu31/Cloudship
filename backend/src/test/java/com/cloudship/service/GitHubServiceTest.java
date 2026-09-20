package com.cloudship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService();
    }

    @Test
    @DisplayName("Should correctly parse valid GitHub HTTPS URL")
    void shouldParseValidHttpsUrl() {
        GitHubService.GitHubRepoInfo info = gitHubService.parseGitHubUrl("https://github.com/Aashu31/Cloudship");
        assertEquals("Aashu31", info.getOwner());
        assertEquals("Cloudship", info.getRepositoryName());
    }

    @Test
    @DisplayName("Should correctly parse valid GitHub HTTPS URL with .git extension")
    void shouldParseValidHttpsUrlWithGitExtension() {
        GitHubService.GitHubRepoInfo info = gitHubService.parseGitHubUrl("https://github.com/spring-projects/spring-boot.git");
        assertEquals("spring-projects", info.getOwner());
        assertEquals("spring-boot", info.getRepositoryName());
    }

    @Test
    @DisplayName("Should correctly parse valid GitHub SSH URL")
    void shouldParseValidSshUrl() {
        GitHubService.GitHubRepoInfo info = gitHubService.parseGitHubUrl("git@github.com:facebook/react.git");
        assertEquals("facebook", info.getOwner());
        assertEquals("react", info.getRepositoryName());
    }

    @Test
    @DisplayName("Should correctly parse valid ssh://git@github.com URL")
    void shouldParseValidSshProtocolUrl() {
        GitHubService.GitHubRepoInfo info = gitHubService.parseGitHubUrl("ssh://git@github.com/torvalds/linux.git");
        assertEquals("torvalds", info.getOwner());
        assertEquals("linux", info.getRepositoryName());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when URL is not GitHub")
    void shouldThrowWhenNotGitHubUrl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            gitHubService.parseGitHubUrl("https://gitlab.com/owner/repo")
        );
        assertTrue(ex.getMessage().contains("Invalid GitHub URL format"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when URL is malformed")
    void shouldThrowWhenUrlIsMalformed() {
        assertThrows(IllegalArgumentException.class, () ->
            gitHubService.parseGitHubUrl("just-some-random-string")
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when URL is null or empty")
    void shouldThrowWhenUrlIsNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> gitHubService.parseGitHubUrl(null));
        assertThrows(IllegalArgumentException.class, () -> gitHubService.parseGitHubUrl("   "));
    }
}
