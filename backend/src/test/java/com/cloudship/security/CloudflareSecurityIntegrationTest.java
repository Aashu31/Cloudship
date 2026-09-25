package com.cloudship.security;

import com.cloudship.entity.Project;
import com.cloudship.entity.User;
import com.cloudship.repository.ProjectRepository;
import com.cloudship.repository.UserRepository;
import com.cloudship.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CloudflareSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CloudflareSecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Public endpoint /api/health is accessible without authentication")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Auth status endpoint /api/auth/me returns authenticated=true in dev mode")
    void testAuthMeInDevMode() throws Exception {
        securityProperties.setEnabled(false);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("dev@cloudship.local"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Dev mode supports X-Dev-User-Email header for identity simulation")
    void testDevUserHeaderSimulation() throws Exception {
        securityProperties.setEnabled(false);

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Dev-User-Email", "developer@cloudship.internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("developer@cloudship.internal"));
    }

    @Test
    @DisplayName("Enforced mode rejects unauthenticated requests to protected endpoints with 401")
    void testEnforcedModeRejectsUnauthenticated() throws Exception {
        securityProperties.setEnabled(true);

        try {
            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Authentication required. Please authenticate via Cloudflare Zero-Trust Access."));
        } finally {
            securityProperties.setEnabled(false);
        }
    }

    @Test
    @DisplayName("Enforced mode rejects invalid or forged JWT assertion with 401")
    void testEnforcedModeRejectsForgedJwt() throws Exception {
        securityProperties.setEnabled(true);

        try {
            mockMvc.perform(get("/api/projects")
                            .header("Cf-Access-Jwt-Assertion", "forged.header.signature"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        } finally {
            securityProperties.setEnabled(false);
        }
    }

    @Test
    @DisplayName("IDOR prevention: Non-admin User cannot access or delete another user's project")
    void testIdorAccessControl() {
        // Create User A and User B
        User userA = userRepository.save(new User("Alice", "alice@cloudship.internal", "USER"));
        User userB = userRepository.save(new User("Bob", "bob@cloudship.internal", "USER"));

        // Create Project owned by User A
        Project projectA = new Project("Alice App", "Private Project", "https://github.com/alice/app", userA);
        projectA = projectRepository.save(projectA);
        Long projectId = projectA.getId();

        // Simulate security context as User B
        CloudshipPrincipal principalB = new CloudshipPrincipal(userB.getId(), userB.getEmail(), userB.getName(), "USER");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principalB, null, principalB.getAuthorities()
                )
        );

        try {
            // User B attempts to access User A's project -> throws ForbiddenException (403)
            assertThatThrownBy(() -> projectService.getProjectById(projectId))
                    .isInstanceOf(com.cloudship.exception.ForbiddenException.class)
                    .hasMessageContaining("You do not have permission to access project with ID: " + projectId);

            // User B attempts to delete User A's project -> throws ForbiddenException (403)
            assertThatThrownBy(() -> projectService.deleteProject(projectId))
                    .isInstanceOf(com.cloudship.exception.ForbiddenException.class)
                    .hasMessageContaining("You do not have permission to access project with ID: " + projectId);

            // Verify project still exists
            assertThat(projectRepository.existsById(projectId)).isTrue();

        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Admin role can access and manage any project across users")
    void testAdminPrivilegeAccess() {
        // Create User A
        User userA = userRepository.save(new User("Alice", "alice@cloudship.internal", "USER"));
        User admin = userRepository.save(new User("Super Admin", "admin@cloudship.internal", "ADMIN"));

        // Create Project owned by User A
        Project projectA = new Project("Alice App 2", "Private Project", "https://github.com/alice/app2", userA);
        projectA = projectRepository.save(projectA);
        Long projectId = projectA.getId();

        // Simulate security context as Admin
        CloudshipPrincipal adminPrincipal = new CloudshipPrincipal(admin.getId(), admin.getEmail(), admin.getName(), "ADMIN");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        adminPrincipal, null, adminPrincipal.getAuthorities()
                )
        );

        try {
            // Admin can read User A's project
            var response = projectService.getProjectById(projectId);
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(projectId);

            // Admin can delete User A's project
            projectService.deleteProject(projectId);
            assertThat(projectRepository.existsById(projectId)).isFalse();

        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("First user in production mode does not automatically become admin (secure default)")
    void testFirstUserNotAutoAdminInProduction() throws Exception {
        securityProperties.setEnabled(true); // Production mode
        userRepository.deleteAll();

        // Verify no users exist before test
        assertThat(userRepository.count()).isZero();

        // In production mode without valid JWT, request should be rejected with 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk()) // /api/auth/me is public
                .andExpect(jsonPath("$.authenticated").value(false));

        // No user should be created
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("Dev mode creates admin user for local development convenience")
    void testDevModeCreatesAdminUser() throws Exception {
        securityProperties.setEnabled(false); // Dev mode
        userRepository.deleteAll();

        // Verify no users exist before test
        assertThat(userRepository.count()).isZero();

        // Simulate dev mode login
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        // Verify user was created with ADMIN role (dev mode convenience)
        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Logout config endpoint returns proper configuration")
    void testLogoutConfigEndpoint() throws Exception {
        securityProperties.setEnabled(false);

        mockMvc.perform(get("/api/auth/logout-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloudflareEnabled").value(false))
                .andExpect(jsonPath("$.logoutUrl").isEmpty());
    }

    @Test
    @DisplayName("Security headers present on responses")
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
