package com.cloudship.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.cloudship.entity.User;
import com.cloudship.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CloudflareAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAuthenticationFilter.class);

    public static final String CF_ASSERTION_HEADER = "Cf-Access-Jwt-Assertion";
    public static final String CF_AUTHORIZATION_COOKIE = "CF_Authorization";
    public static final String DEV_USER_EMAIL_HEADER = "X-Dev-User-Email";

    private final CloudflareSecurityProperties properties;
    private final CloudflareJwtValidator jwtValidator;
    private final UserRepository userRepository;
    private final SecurityAuditLogger auditLogger;

    public CloudflareAuthenticationFilter(
            CloudflareSecurityProperties properties,
            CloudflareJwtValidator jwtValidator,
            UserRepository userRepository,
            SecurityAuditLogger auditLogger) {
        this.properties = properties;
        this.jwtValidator = jwtValidator;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        String requestPath = request.getRequestURI();

        if (token != null && !token.isBlank()) {
            try {
                CloudflareJwtValidator.VerifiedToken verified = jwtValidator.validateToken(token);
                User user = getOrCreateUser(verified.email(), verified.name());
                setAuthenticatedUser(user, request);
                auditLogger.logLoginSuccess(user, requestPath);
                log.debug("Authenticated user via Cloudflare Access JWT: {}", user.getEmail());
            } catch (JWTVerificationException | IllegalStateException e) {
                log.warn("Cloudflare Access JWT validation failed for request {}: {}", requestPath, e.getMessage());
                auditLogger.logInvalidJwt(requestPath, e.getMessage());
                // Invalidate security context on invalid token
                SecurityContextHolder.clearContext();
            }
        } else if (!properties.isEnabled()) {
            // Local development mode fallback: only active when cloudflare.enabled = false
            String devEmail = request.getHeader(DEV_USER_EMAIL_HEADER);
            if (devEmail == null || devEmail.isBlank()) {
                devEmail = properties.getDevUserEmail();
            } else {
                devEmail = devEmail.trim().toLowerCase();
            }

            User user = getOrCreateDevUser(devEmail, properties.getDevUserName());
            setAuthenticatedUser(user, request);
            auditLogger.logLoginSuccess(user, requestPath);
            log.trace("Local development mode: authenticated as dev user {}", user.getEmail());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Check Cf-Access-Jwt-Assertion header (standard Cloudflare Access injection)
        String headerToken = request.getHeader(CF_ASSERTION_HEADER);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }

        // 2. Check Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 3. Check CF_Authorization cookie (injected when browser visits protected domain)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (CF_AUTHORIZATION_COOKIE.equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void setAuthenticatedUser(User user, HttpServletRequest request) {
        CloudshipPrincipal principal = new CloudshipPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private synchronized User getOrCreateUser(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User(
                    name != null && !name.isBlank() ? name : email.split("@")[0],
                    email,
                    "USER"
            );
            return userRepository.save(newUser);
        });
    }

    private synchronized User getOrCreateDevUser(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User devUser = new User(name, email, "ADMIN");
            return userRepository.save(devUser);
        });
    }
}
