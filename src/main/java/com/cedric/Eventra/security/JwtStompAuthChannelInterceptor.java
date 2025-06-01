package com.cedric.Eventra.security; // Or your designated security package

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException; // Correct import for JJWT's SignatureException
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtStompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService; // Your CustomUserDetailsService implements this

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("STOMP CONNECT command received, attempting JWT authentication.");

            List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
            String bearerToken = null;

            if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
                String authHeaderValue = authorizationHeaders.get(0);
                if (StringUtils.hasText(authHeaderValue) && authHeaderValue.startsWith("Bearer ")) {
                    bearerToken = authHeaderValue.substring(7);
                    log.debug("Bearer token extracted from STOMP CONNECT headers.");
                } else {
                    log.warn("Authorization header found but not in 'Bearer <token>' format.");
                }
            } else {
                log.warn("No 'Authorization' header found in STOMP CONNECT message.");
                // For a system where all WebSocket connections MUST be authenticated,
                // you might throw an exception here to deny the connection.
                // e.g., throw new MessageDeliveryException("Missing authentication token for WebSocket connection.");
                // If some topics/queues are public, you let it pass, and endpoint security handles it.
                // For most chat applications, CONNECT usually implies an authentication attempt.
            }

            if (bearerToken != null) {
                String username = null;
                try {
                    username = jwtUtils.getUsernameFromToken(bearerToken); // Extracts email

                    if (StringUtils.hasText(username)) {
                        // It's generally good practice not to set Authentication in SecurityContextHolder
                        // from a previous request if a new CONNECT attempt is made with a token.
                        // However, accessor.getUser() should reflect this session's user.
                        // If SecurityContextHolder.getContext().getAuthentication() is null, it means no prior auth on this thread.

                        UserDetails userDetails = userDetailsService.loadUserByUsername(username); // Throws UsernameNotFoundException

                        if (jwtUtils.isTokenValid(bearerToken, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            // This is the crucial part for STOMP sessions
                            accessor.setUser(authentication);

                            // Optionally, if other parts of your system running in this thread need it:
                            // SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.info("User '{}' authenticated successfully for WebSocket session via STOMP CONNECT.", username);
                        } else {
                            log.warn("Invalid JWT token received for WebSocket connection for user: {}. Token validation failed.", username);
                            // To deny connection for invalid token:
                            // throw new MessageDeliveryException("Invalid JWT token.");
                        }
                    } else {
                        log.warn("Username could not be extracted from JWT token for WebSocket.");
                    }
                } catch (UsernameNotFoundException e) {
                    log.warn("User not found from JWT token for WebSocket: {}", (username != null ? username : "N/A"), e);
                    // throw new MessageDeliveryException("User from token not found.");
                } catch (ExpiredJwtException e) {
                    log.warn("Expired JWT token received for WebSocket connection: {}", e.getMessage());
                    // throw new MessageDeliveryException("Expired JWT token.");
                } catch (SignatureException e) {
                    log.warn("JWT signature validation failed for WebSocket connection: {}", e.getMessage());
                    // throw new MessageDeliveryException("Invalid JWT signature.");
                } catch (MalformedJwtException e) {
                    log.warn("Malformed JWT token for WebSocket connection: {}", e.getMessage());
                    // throw new MessageDeliveryException("Malformed JWT token.");
                } catch (Exception e) {
                    log.error("Unexpected error during WebSocket STOMP CONNECT JWT authentication: {}", e.getMessage(), e);
                    // throw new MessageDeliveryException("Authentication error.");
                }
            }
            // If no token was provided, or if token validation failed and we didn't throw an exception,
            // the connection proceeds without an authenticated user set on the accessor.
            // Subsequent @MessageMapping security or subscriptions might then fail if they require authentication.
        }
        return message; // Always return the message, possibly with accessor.setUser(auth) updated.
    }

    // postSend, afterSendCompletion, afterSendFailed can be overridden for more detailed logging or handling if needed.
}