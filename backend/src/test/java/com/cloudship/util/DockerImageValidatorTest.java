package com.cloudship.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DockerImageValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "cloudshipcr.azurecr.io",
            "myregistry.azurecr.cn",
            "localhost",
            "registry.internal.corp:5000",
            "test-registry-1.io"
    })
    @DisplayName("Accepts valid registry hosts")
    void validRegistryHosts(String host) {
        assertThat(DockerImageValidator.isValidRegistryHost(host)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "registry;rm -rf /",
            "reg`whoami`.io",
            "host with spaces",
            "registry$(id).azurecr.io",
            "reg|pipe.io",
            "reg&background.io"
    })
    @DisplayName("Rejects invalid or malicious registry hosts")
    void invalidRegistryHosts(String host) {
        assertThat(DockerImageValidator.isValidRegistryHost(host)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "cloudship/backend",
            "cloudship/frontend",
            "backend",
            "org/team/subteam/app",
            "app-service_v2.test"
    })
    @DisplayName("Accepts valid Docker repositories")
    void validRepositories(String repo) {
        assertThat(DockerImageValidator.isValidRepository(repo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "UPPERCASE_NOT_ALLOWED",
            "repo;reboot",
            "repo`cat /etc/passwd`",
            "repo $(reboot)",
            "repo/name with space",
            "/leading/slash",
            "trailing/slash/"
    })
    @DisplayName("Rejects invalid Docker repositories")
    void invalidRepositories(String repo) {
        assertThat(DockerImageValidator.isValidRepository(repo)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "latest",
            "v1.0.0",
            "a3f9c21",
            "main-b12",
            "build_42",
            "commit-sha-abcdef012345"
    })
    @DisplayName("Accepts valid image tags")
    void validTags(String tag) {
        assertThat(DockerImageValidator.isValidTag(tag)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "-leading-dash",
            ".leading-dot",
            "tag;echo hacked",
            "tag`reboot`",
            "tag$foo",
            "tag with spaces",
            "tag\ntag"
    })
    @DisplayName("Rejects invalid or injected image tags")
    void invalidTags(String tag) {
        assertThat(DockerImageValidator.isValidTag(tag)).isFalse();
    }

    @Test
    @DisplayName("Accepts valid image digests")
    void validDigests() {
        assertThat(DockerImageValidator.isValidDigest("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")).isTrue();
        assertThat(DockerImageValidator.isValidDigest("sha256;rm -rf")).isFalse();
        assertThat(DockerImageValidator.isValidDigest("")).isFalse();
    }

    @Test
    @DisplayName("Constructs deterministic immutable tag from commit SHA")
    void constructTagFromSha() {
        String tag = DockerImageValidator.constructImageTag("4f5a6b7c8d9e0f", "main", 1L);
        assertThat(tag).isEqualTo("4f5a6b7");
    }

    @Test
    @DisplayName("Constructs safe fallback tag when commit SHA is null")
    void constructTagFallback() {
        String tag = DockerImageValidator.constructImageTag(null, "feature/auth-service", 10L);
        assertThat(tag).isEqualTo("feature-auth-service-b10");

        String tagNoBranch = DockerImageValidator.constructImageTag(null, null, 42L);
        assertThat(tagNoBranch).isEqualTo("build-42");
    }

    @Test
    @DisplayName("Sanitizes credential leakage in error messages")
    void sanitizeErrorMessage() {
        String raw = "Login failed: password=supersecret123 and token=my-access-token; failed to push";
        String clean = DockerImageValidator.sanitizeErrorMessage(raw);
        assertThat(clean).doesNotContain("supersecret123");
        assertThat(clean).doesNotContain("my-access-token");
        assertThat(clean).contains("password=***");
        assertThat(clean).contains("token=***");
    }
}
