package com.cloudship.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS is configured in SecurityConfig.corsConfigurationSource() to avoid duplication
    // and ensure consistent CORS handling with Spring Security filter chain.
}
