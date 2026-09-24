package com.cloudship.security;

import com.cloudship.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CloudshipPrincipal> getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CloudshipPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static CloudshipPrincipal getAuthenticatedPrincipalOrThrow() {
        return getCurrentPrincipal()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }
}
