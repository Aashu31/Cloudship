package com.cloudship.util;

import java.util.regex.Pattern;

/**
 * Validates and sanitizes Docker image components (registry login server, repository,
 * tag, and digest) according to Docker/OCI specifications to protect against
 * shell command injection, path traversal, and malformed inputs.
 */
public final class DockerImageValidator {

    // RFC 1123 compliant hostname with optional port (e.g. cloudshipcr.azurecr.io, localhost:5000)
    private static final Pattern REGISTRY_HOST_PATTERN = Pattern.compile(
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?::\\d{1,5})?$"
    );

    // Docker repository specification: lowercase alphanumeric with '.', '_', '-', '/' separators
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
            "^[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*$"
    );

    // Docker tag specification: ASCII letters, numbers, underscores, periods, hyphens. Max 128 chars.
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_][a-zA-Z0-9._-]{0,127}$"
    );

    // OCI / Docker content digest: algorithm (sha256) followed by hex string
    private static final Pattern DIGEST_PATTERN = Pattern.compile(
            "^[a-z0-9]+:[a-fA-F0-9]{32,128}$"
    );

    // Dangerous shell metacharacters that must never appear in any parameter
    private static final Pattern SHELL_METACHAR_PATTERN = Pattern.compile(
            "[;&|`$><*?!\\s\\r\\n\\\"'\\\\()]"
    );

    private DockerImageValidator() {
    }

    /**
     * Validates a registry login server hostname.
     */
    public static boolean isValidRegistryHost(String host) {
        if (host == null || host.isBlank() || host.length() > 255) {
            return false;
        }
        if (SHELL_METACHAR_PATTERN.matcher(host).find()) {
            return false;
        }
        return REGISTRY_HOST_PATTERN.matcher(host.trim()).matches();
    }

    /**
     * Validates a Docker/OCI repository name.
     */
    public static boolean isValidRepository(String repository) {
        if (repository == null || repository.isBlank() || repository.length() > 255) {
            return false;
        }
        if (SHELL_METACHAR_PATTERN.matcher(repository).find()) {
            return false;
        }
        return REPOSITORY_PATTERN.matcher(repository.trim()).matches();
    }

    /**
     * Validates a Docker image tag.
     */
    public static boolean isValidTag(String tag) {
        if (tag == null || tag.isBlank() || tag.length() > 128) {
            return false;
        }
        if (SHELL_METACHAR_PATTERN.matcher(tag).find()) {
            return false;
        }
        return TAG_PATTERN.matcher(tag.trim()).matches();
    }

    /**
     * Validates an image content digest (e.g. sha256:4f5a...).
     */
    public static boolean isValidDigest(String digest) {
        if (digest == null || digest.isBlank() || digest.length() > 200) {
            return false;
        }
        if (SHELL_METACHAR_PATTERN.matcher(digest).find()) {
            return false;
        }
        return DIGEST_PATTERN.matcher(digest.trim()).matches();
    }

    /**
     * Validates a full Docker image reference: [loginServer/]repository:tag
     */
    public static boolean isValidImageReference(String imageRef) {
        if (imageRef == null || imageRef.isBlank() || imageRef.length() > 500) {
            return false;
        }
        if (SHELL_METACHAR_PATTERN.matcher(imageRef).find()) {
            return false;
        }

        // Split tag
        int colonIdx = imageRef.lastIndexOf(':');
        if (colonIdx <= 0 || colonIdx == imageRef.length() - 1) {
            return false;
        }
        String namePart = imageRef.substring(0, colonIdx);
        String tagPart = imageRef.substring(colonIdx + 1);

        if (!isValidTag(tagPart)) {
            return false;
        }

        // Check if registry host is present (contains a dot before the first slash)
        int firstSlashIdx = namePart.indexOf('/');
        if (firstSlashIdx > 0 && namePart.substring(0, firstSlashIdx).contains(".")) {
            String host = namePart.substring(0, firstSlashIdx);
            String repo = namePart.substring(firstSlashIdx + 1);
            return isValidRegistryHost(host) && isValidRepository(repo);
        }

        return isValidRepository(namePart);
    }

    /**
     * Constructs a standard immutable or branch image tag.
     * Prefers short commit SHA (7 chars) if available; otherwise uses normalized branch name or fallback.
     */
    public static String constructImageTag(String commitSha, String branch, Long buildId) {
        if (commitSha != null && !commitSha.isBlank()) {
            String cleanSha = commitSha.trim().toLowerCase();
            // Validate hex
            if (cleanSha.matches("^[a-f0-9]{7,40}$")) {
                return cleanSha.substring(0, Math.min(cleanSha.length(), 7));
            }
        }

        if (branch != null && !branch.isBlank()) {
            String cleanBranch = branch.trim().toLowerCase()
                    .replaceAll("[^a-z0-9._-]", "-")
                    .replaceAll("^-+|-+$", "");
            if (!cleanBranch.isEmpty()) {
                String candidate = cleanBranch + (buildId != null ? "-b" + buildId : "-latest");
                return candidate.length() > 128 ? candidate.substring(0, 128) : candidate;
            }
        }

        return buildId != null ? "build-" + buildId : "latest";
    }

    /**
     * Builds a full deterministic image reference: <loginServer>/<repository>:<tag>
     */
    public static String buildFullImageReference(String loginServer, String repository, String tag) {
        String cleanRepo = (repository != null && !repository.isBlank()) ? repository.trim().toLowerCase() : "cloudship/backend";
        String cleanTag = (tag != null && !tag.isBlank()) ? tag.trim() : "latest";

        if (loginServer != null && !loginServer.isBlank()) {
            return loginServer.trim().toLowerCase() + "/" + cleanRepo + ":" + cleanTag;
        }
        return cleanRepo + ":" + cleanTag;
    }

    /**
     * Sanitizes error messages to prevent sensitive credential exposure or excessive log injection.
     */
    public static String sanitizeErrorMessage(String error) {
        if (error == null) return null;
        String sanitized = error.replaceAll("(?i)(password|secret|token|key|authorization|bearer)[=:\\s]+[^\\s;,]+", "$1=***");
        if (sanitized.length() > 990) {
            sanitized = sanitized.substring(0, 990) + "...";
        }
        return sanitized.trim();
    }
}
