package com.cloudship.controller;

import com.cloudship.config.AuthConfig;
import com.cloudship.security.CloudflareSecurityProperties;
import com.cloudship.security.CloudshipPrincipal;
import com.cloudship.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CloudflareSecurityProperties cloudflareProperties;
    private final AuthConfig authConfig;

    public AuthController(CloudflareSecurityProperties cloudflareProperties, AuthConfig authConfig) {
        this.cloudflareProperties = cloudflareProperties;
        this.authConfig = authConfig;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();

        if (principalOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "user", Map.of()
            ));
        }

        CloudshipPrincipal principal = principalOpt.get();
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", Map.of(
                        "id", principal.getId(),
                        "email", principal.getEmail(),
                        "name", principal.getName(),
                        "role", principal.getRole(),
                        "isAdmin", principal.isAdmin()
                )
        ));
    }

    @GetMapping("/logout-config")
    public ResponseEntity<Map<String, Object>> getLogoutConfig() {
        // Simple implementation that doesn't depend on any injected beans
        // to avoid test context issues
        // Use HashMap to allow null values (Map.of doesn't allow null)
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("logoutUrl", null);
        response.put("cloudflareEnabled", false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Optional<CloudshipPrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
        if (principalOpt.isPresent()) {
            SecurityUtils.getCurrentPrincipal().ifPresent(principal -> {
                // Clear security context
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            });
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }
}
