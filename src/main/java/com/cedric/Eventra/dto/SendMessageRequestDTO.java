package com.cedric.Eventra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequestDTO {
    @NotNull(message = "Chat room ID cannot be null.")
    private Long chatRoomId;

    @NotBlank(message = "Message content cannot be empty.")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters.")
    private String content;

    // **Temporary Simplification (Less Secure):**
    // If full WebSocket JWT security (via ChannelInterceptor) is too complex to implement quickly,
    // *could* pass senderId here. The backend would then trust this ID.
    // **THIS IS A SECURITY VULNERABILITY and should be clearly documented as such if used.**
    // The ideal way is to derive sender from an authenticated WebSocket session Principal.
    // private Long senderId; // <<<<<<<<<<<<< SECURITY CAVEAT IF USED
}