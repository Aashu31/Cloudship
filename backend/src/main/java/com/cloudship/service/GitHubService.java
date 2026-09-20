package com.cloudship.service;

import com.cloudship.entity.GitConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    // Regex supporting HTTPS, Git SSH, and ssh://git@ URLs for github.com
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
        "^(?:https:\\/\\/github\\.com\\/|git@github\\.com:|ssh:\\/\\/git@github\\.com\\/)([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+?)(?:\\.git)?$"
    );

    private final RestClient restClient;

    public GitHubService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "CloudShip-DevOps-Platform")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
    }

    public static class GitHubRepoInfo {
        private final String owner;
        private final String repositoryName;

        public GitHubRepoInfo(String owner, String repositoryName) {
            this.owner = owner;
            this.repositoryName = repositoryName;
        }

        public String getOwner() {
            return owner;
        }

        public String getRepositoryName() {
            return repositoryName;
        }
    }

    public static class GitHubMetadataResult {
        private final String owner;
        private final String repositoryName;
        private final String defaultBranch;
        private final GitConnectionStatus connectionStatus;
        private final String message;

        public GitHubMetadataResult(String owner, String repositoryName, String defaultBranch, GitConnectionStatus connectionStatus, String message) {
            this.owner = owner;
            this.repositoryName = repositoryName;
            this.defaultBranch = defaultBranch;
            this.connectionStatus = connectionStatus;
            this.message = message;
        }

        public String getOwner() {
            return owner;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public String getDefaultBranch() {
            return defaultBranch;
        }

        public GitConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Parses and extracts the owner and repository name from a GitHub URL.
     * Throws IllegalArgumentException if the URL format is invalid.
     */
    public GitHubRepoInfo parseGitHubUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository URL cannot be null or empty");
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid GitHub URL format: " + url);
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        if (owner == null || owner.isBlank() || repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("Could not extract owner and repository name from: " + url);
        }

        return new GitHubRepoInfo(owner, repo);
    }

    /**
     * Validates and verifies repository accessibility with GitHub's public API.
     */
    public GitHubMetadataResult verifyRepository(String url, String preferredBranch) {
        GitHubRepoInfo info = parseGitHubUrl(url);
        String owner = info.getOwner();
        String repo = info.getRepositoryName();

        log.info("Verifying GitHub repository metadata for {}/{}", owner, repo);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("GitHub API returned 4xx status {} for {}/{}", resp.getStatusCode(), owner, repo);
                    })
                    .body(Map.class);

            if (response != null && response.containsKey("name")) {
                String defaultBranch = (String) response.get("default_branch");
                if (preferredBranch != null && !preferredBranch.isBlank()) {
                    defaultBranch = preferredBranch;
                } else if (defaultBranch == null || defaultBranch.isBlank()) {
                    defaultBranch = "main";
                }

                log.info("Successfully verified GitHub repository {}/{} (branch: {})", owner, repo, defaultBranch);
                return new GitHubMetadataResult(
                        owner,
                        repo,
                        defaultBranch,
                        GitConnectionStatus.CONNECTED,
                        "Repository verified successfully via GitHub API"
                );
            } else {
                return new GitHubMetadataResult(
                        owner,
                        repo,
                        preferredBranch != null && !preferredBranch.isBlank() ? preferredBranch : "main",
                        GitConnectionStatus.NOT_CONNECTED,
                        "Repository not found or access requires private credentials"
                );
            }
        } catch (Exception e) {
            log.warn("Could not reach GitHub API for {}/{}: {}", owner, repo, e.getMessage());
            // Honest state when GitHub is offline, rate-limited, or unreachable
            String branch = (preferredBranch != null && !preferredBranch.isBlank()) ? preferredBranch : "main";
            return new GitHubMetadataResult(
                    owner,
                    repo,
                    branch,
                    GitConnectionStatus.ERROR,
                    "GitHub API unreachable or rate-limited: " + e.getMessage()
            );
        }
    }
}
