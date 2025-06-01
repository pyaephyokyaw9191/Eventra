package com.cedric.Eventra.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatRoomRequestDTO {
    @NotNull(message = "Other user ID cannot be null")
    private Long otherUserId;

    private Long bookingId; // Optional
}