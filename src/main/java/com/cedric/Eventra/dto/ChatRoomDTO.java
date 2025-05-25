package com.cedric.Eventra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private UserDTO participant1; // Assuming UserDTO has basic info like id, firstName, lastName
    private UserDTO participant2;
    private Long bookingId;       // Optional
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt; // Useful for sorting rooms
    // The actual last message content can be fetched when the room is opened,
    // or you can add it back here later if needed for previews.
}