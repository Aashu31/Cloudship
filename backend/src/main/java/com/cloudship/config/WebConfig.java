package com.cloudship.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${cloudship.cors.allowed-origins:http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500,http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        log.info("Configuring CORS allowed origins: {}", Arrays.toString(origins));

        boolean hasWildcard = Arrays.stream(origins).anyMatch(o -> o.contains("*"));

        var registration = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "Accept", "X-Requested-With", "X-CloudShip-CI-Token", "X-Hub-Signature-256", "X-GitHub-Event", "Cf-Access-Jwt-Assertion", "CF-Access-Authenticated-User-Email", "X-Dev-User-Email")
                .allowCredentials(true)
                .maxAge(3600);

        if (hasWildcard) {
            registration.allowedOriginPatterns(origins);
        } else {
            registration.allowedOrigins(origins);
        }
    }
}
