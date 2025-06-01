package com.cedric.Eventra.security; // Or your designated config package

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Moved to WebMvcConfig
@Configuration
public class CorsConfig {
/*
    @Bean
    public WebMvcConfigurer globalCorsConfigurer() { // Renamed method for clarity
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Configuration for your main REST APIs
                registry.addMapping("/api/**") // Apply to all paths under /api/
                        .allowedOrigins(
                                "http://localhost:3000",  // Example: Common React dev port
                                "http://localhost:5173",  // Example: Common Vite/Vue dev port
                                "http://localhost:63342"  // The port your test-chat.html was served from
                                // Add your deployed frontend production URL here later
                                // e.g., "https://yourfrontend.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Crucial: Add OPTIONS
                        .allowedHeaders("*") // Allows all headers including Authorization and Content-Type
                        .allowCredentials(true) // Important for sending tokens/cookies
                        .maxAge(3600); // Optional: How long pre-flight response can be cached (in seconds)

                // Specific configuration for WebSocket/SockJS HTTP handshake paths
                // SockJS makes initial HTTP requests to these paths.
                registry.addMapping("/ws/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "http://localhost:5173",
                                "http://localhost:63342"
                                // Add your deployed frontend production URL here later
                        )
                        .allowedMethods("GET", "POST", "OPTIONS") // Handshake typically uses these
                        .allowedHeaders("*")
                        .allowCredentials(true) // SockJS often needs this with tokens/sessions
                        .maxAge(3600);
            }
        };
    }

 */
}