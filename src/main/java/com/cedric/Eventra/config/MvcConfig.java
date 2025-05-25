package com.cedric.Eventra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j // Added for logging
public class MvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir.base}")
    private String baseUploadDir;

    // Injecting specific subdirectory names from properties
    @Value("${file.upload-dir.service-images}")
    private String serviceImagesSubDir; // e.g., "service-images"

    @Value("${file.upload-dir.profile-pictures}")
    private String profilePicturesSubDir; // e.g., "profile-pictures"

    @Value("${file.upload-dir.cover-photos}")
    private String coverPhotosSubDir; // e.g., "cover-photos"

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Base file system path
        Path baseUploadPath = Paths.get(baseUploadDir).toAbsolutePath();

        // Handler for Service Images
        String serviceImagesLocation = "file:" + baseUploadPath.resolve(serviceImagesSubDir).toString() + "/";
        registry.addResourceHandler("/uploads/service-images/**") // Public URL path
                .addResourceLocations(serviceImagesLocation);       // File system location
        log.info("Serving service images from: {} mapped to URL /uploads/service-images/**", serviceImagesLocation);

        // Handler for Profile Pictures
        String profilePicturesLocation = "file:" + baseUploadPath.resolve(profilePicturesSubDir).toString() + "/";
        registry.addResourceHandler("/uploads/profile-pictures/**") // Public URL path
                .addResourceLocations(profilePicturesLocation);      // File system location
        log.info("Serving profile pictures from: {} mapped to URL /uploads/profile-pictures/**", profilePicturesLocation);

        // Handler for Cover Photos
        String coverPhotosLocation = "file:" + baseUploadPath.resolve(coverPhotosSubDir).toString() + "/";
        registry.addResourceHandler("/uploads/cover-photos/**")   // Public URL path
                .addResourceLocations(coverPhotosLocation);        // File system location
        log.info("Serving cover photos from: {} mapped to URL /uploads/cover-photos/**", coverPhotosLocation);
    }

    // Your existing global CORS configurer bean
    @Bean
    public WebMvcConfigurer globalCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // API Calls
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:63342") // Your frontend origins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);

                // WebSocket Handshake specific if different (already in your other CorsConfig, this one is more general)
                // Or if you have one single CorsConfig, ensure /ws/** is handled correctly
                registry.addMapping("/ws/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:63342")
                        .allowedMethods("GET", "POST", "OPTIONS") // SockJS handshake might need these
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}