package com.ucd.urbanflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Cross-Origin Resource Sharing (CORS) configuration for the application.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // This configuration allows cross-origin requests from the frontend application.
        registry.addMapping("/api/**") // Apply CORS policy to all API endpoints under /api/
                .allowedOrigins("http://localhost:5173") // IMPORTANT: The origin of your Vue.js frontend app
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // Allow cookies and authentication headers
    }
}