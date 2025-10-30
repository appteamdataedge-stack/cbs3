package com.example.moneymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * CORS configuration for allowing frontend to communicate with backend
 * Supports dynamic origins from environment variables for EC2 deployment
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed.origins:}")
    private String additionalAllowedOrigins;

    /**
     * Get all allowed origins including environment-specific ones
     */
    private List<String> getAllowedOrigins() {
        List<String> origins = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4173",
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:5177",
            "http://localhost:5178",
            "https://cbs3.vercel.app",
            "https://moneymarket.duckdns.org"
        ));
        
        // Add additional origins from environment variable (comma-separated)
        if (additionalAllowedOrigins != null && !additionalAllowedOrigins.trim().isEmpty()) {
            String[] additionalOrigins = additionalAllowedOrigins.split(",");
            for (String origin : additionalOrigins) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty() && !origins.contains(trimmedOrigin)) {
                    origins.add(trimmedOrigin);
                }
            }
        }
        
        System.out.println("CORS Allowed Origins: " + origins);
        return origins;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        
        // Use dynamic allowed origins
        List<String> allowedOrigins = getAllowedOrigins();
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        
        // Allow all headers and methods
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setMaxAge(3600L);
        
        // Expose headers
        corsConfiguration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", 
            "Access-Control-Allow-Methods", 
            "Access-Control-Allow-Headers", 
            "Access-Control-Max-Age", 
            "Access-Control-Allow-Credentials",
            "Content-Type",
            "Authorization"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}

