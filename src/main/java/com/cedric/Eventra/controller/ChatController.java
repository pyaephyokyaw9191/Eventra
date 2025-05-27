package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.CreateChatRoomRequestDTO;
import com.cedric.Eventra.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat") // Base path for chat-related REST API
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Endpoint to get an existing chat room with another user or create a new one.
     * The authenticated user is one participant; the other is specified in the request.
     * Can optionally be linked to a booking.
     *
     * @param requestDTO DTO containing otherUserId and optional bookingId.
     * @return ResponseEntity containing the Response with ChatRoomDTO.
     */
    @PostMapping("/rooms/initiate") // Changed from get-or-create for clarity
    @PreAuthorize("isAuthenticated()") // Any authenticated user can initiate a chat
    public ResponseEntity<Response> initiateChatRoom(@Valid @RequestBody CreateChatRoomRequestDTO requestDTO) {
        Response serviceResponse = chatService.getOrCreateChatRoom(requestDTO);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user to retrieve all of their chat rooms.
     * Rooms are typically ordered by the most recent message.
     *
     * @return ResponseEntity containing the Response with a list of ChatRoomDTOs.
     */
    @GetMapping("/rooms/my") // Changed from my-rooms for brevity
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getMyChatRooms() {
        Response serviceResponse = chatService.getMyChatRooms();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint to retrieve messages for a specific chat room.
     * Authenticated user must be a participant of the chat room.
     * (Simplified version - no pagination for now)
     *
     * @param chatRoomId The ID of the chat room.
     * @return ResponseEntity containing the Response with a list of ChatMessageDTOs.
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getChatMessagesForRoom(@PathVariable Long chatRoomId) {
        Response serviceResponse = chatService.getChatMessagesForRoom(chatRoomId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}