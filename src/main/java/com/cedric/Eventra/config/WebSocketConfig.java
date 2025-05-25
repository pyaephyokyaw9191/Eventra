package com.cedric.Eventra.config;

import com.cedric.Eventra.security.JwtStompAuthChannelInterceptor; // You'll need this for secure WebSockets
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
// Apply this after Spring Security's default WebSocket security if any conflicts arise.
// @Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // For secure WebSockets, you would inject and use this:
    private final JwtStompAuthChannelInterceptor jwtStompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // For messages from server to client
        config.setApplicationDestinationPrefixes("/app"); // For messages from client to server (@MessageMapping)
        config.setUserDestinationPrefix("/user"); // For user-specific messages
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // Your WebSocket handshake endpoint
                // Replace with your actual frontend origin(s) for development and production
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:63342", "YOUR_FRONTEND_DOMAIN_HERE")
                .withSockJS(); // For fallback compatibility
    }

    // To enable JWT authentication for WebSockets (RECOMMENDED for more than basic demo)
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompAuthChannelInterceptor);
    }
}