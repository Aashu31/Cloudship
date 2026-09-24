package com.cloudship.controller;

import com.cloudship.security.CloudshipPrincipal;
import com.cloudship.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
}
