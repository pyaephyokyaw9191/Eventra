package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.ChatMessageDTO;
import com.cedric.Eventra.dto.SendMessageRequestDTO;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException; // Assuming you have this
import com.cedric.Eventra.service.ChatService;
import com.cedric.Eventra.service.UserService; // To fetch User entity from Principal
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser; // For sending errors back to user
import org.springframework.security.core.Authentication; // For getting authenticated principal
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal; // Standard Java Principal

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService; // To convert Principal to your User entity

    /**
     * Handles incoming chat messages sent by clients via WebSocket/STOMP.
     * Client sends to "/app/chat.send".
     * The message is processed, saved, and then broadcast to subscribers of the specific chat room's topic.
     *
     * @param messageRequest DTO containing chatRoomId and message content.
     * @param principal      The authenticated user sending the message (populated by JwtStompAuthChannelInterceptor).
     */
    @MessageMapping("/chat.send") // Clients send messages to /app/chat.send
    public void handleSendMessage(@Payload @Validated SendMessageRequestDTO messageRequest,
                                  Principal principal) { // Principal is populated by Spring Security via the interceptor

        if (principal == null || principal.getName() == null) {
            log.error("Cannot send message: User not authenticated in WebSocket session for message to room {}.", messageRequest.getChatRoomId());
            // Not easy to send an error response directly back for a void method that broadcasts.
            // Client should handle lack of response or timeout as an error.
            // Or configure a user-specific error queue.
            return;
        }

        User sender;
        try {
            // Assuming principal.getName() returns the username (e.g., email)
            // You'll need a method in UserService to fetch User by username/email
            sender = userService.getUserByEmail(principal.getName()); // Or getUserByEmail
        } catch (Exception e) {
            log.error("Cannot send message: Could not retrieve sender User object for principal '{}' for room {}.", principal.getName(), messageRequest.getChatRoomId(), e);
            // Send error to user's private error queue if configured
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Error: Could not identify sender.");
            return;
        }

        if (sender == null) { // Should be caught by exception above from userService ideally
            log.error("Cannot send message: Sender User object is null for principal: {} for room {}.", principal.getName(), messageRequest.getChatRoomId());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Error: Sender identity not found.");
            return;
        }

        try {
            ChatMessageDTO savedMessageDTO = chatService.saveAndPrepareMessage(messageRequest, sender);

            // Broadcast the saved message to all subscribers of the specific chat room's topic.
            // The destination is typically like "/topic/room/{chatRoomId}".
            String destination = "/topic/room." + messageRequest.getChatRoomId();
            messagingTemplate.convertAndSend(destination, savedMessageDTO);

            log.info("Message from user {} sent to chat room {} and broadcasted to {}", sender.getEmail(), messageRequest.getChatRoomId(), destination);

        } catch (ResourceNotFoundException | UnauthorizedException e) {
            log.warn("Failed to send message for user {} to room {}: {}", sender.getEmail(), messageRequest.getChatRoomId(), e.getMessage());
            // Send error specifically to the user who tried to send the message
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Error sending message: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing or sending chat message for user {} to room {}: {}", sender.getEmail(), messageRequest.getChatRoomId(), e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Error: Could not send your message due to a server issue.");
        }
    }

    /**
     * Example of a general error handler for exceptions thrown from @MessageMapping methods.
     * This message will be sent to the user's private "/queue/errors" destination.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors") // Default destination for STOMP exceptions from this controller
    public String handleMessagingException(Throwable exception) {
        log.error("Error in WebSocket message handling: " + exception.getMessage(), exception);
        return "Error: " + exception.getMessage(); // Simple error message string
    }
}