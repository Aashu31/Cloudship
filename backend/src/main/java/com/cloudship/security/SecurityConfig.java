package com.cloudship.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cloudship.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CloudflareSecurityProperties.class)
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Value("${cloudship.cors.allowed-origins:http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500,http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public CloudflareJwtValidator cloudflareJwtValidator(CloudflareSecurityProperties properties) {
        return new CloudflareJwtValidator(properties);
    }

    @Bean
    public CloudflareAuthenticationFilter cloudflareAuthenticationFilter(
            CloudflareSecurityProperties properties,
            CloudflareJwtValidator jwtValidator,
            UserRepository userRepository) {
        return new CloudflareAuthenticationFilter(properties, jwtValidator, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CloudflareAuthenticationFilter cloudflareAuthFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = Map.of(
                                    "status", HttpServletResponse.SC_UNAUTHORIZED,
                                    "error", "UNAUTHORIZED",
                                    "message", "Authentication required. Please authenticate via Cloudflare Zero-Trust Access.",
                                    "path", request.getRequestURI()
                            );
                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = Map.of(
                                    "status", HttpServletResponse.SC_FORBIDDEN,
                                    "error", "FORBIDDEN",
                                    "message", "Access denied. Insufficient privileges.",
                                    "path", request.getRequestURI()
                            );
                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight CORS OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Health and probes
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                        // Auth status endpoint (returns authenticated: false when unauthenticated)
                        .requestMatchers("/api/auth/me").permitAll()

                        // Webhook endpoints (validated via HMAC signatures or CI tokens)
                        .requestMatchers("/api/webhooks/**").permitAll()

                        // Error handler
                        .requestMatchers("/error").permitAll()

                        // All protected API endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Any other request
                        .anyRequest().authenticated()
                )
                .addFilterBefore(cloudflareAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        boolean hasWildcard = origins.stream().anyMatch(o -> o.contains("*"));
        if (hasWildcard) {
            configuration.setAllowedOriginPatterns(origins);
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Accept",
                "X-Requested-With",
                "X-CloudShip-CI-Token",
                "X-Hub-Signature-256",
                "X-GitHub-Event",
                "Cf-Access-Jwt-Assertion",
                "CF-Access-Authenticated-User-Email",
                "X-Dev-User-Email"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
