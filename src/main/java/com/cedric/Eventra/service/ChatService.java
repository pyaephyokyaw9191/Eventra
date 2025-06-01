package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.ChatMessageDTO;
import com.cedric.Eventra.dto.Response; // Your standard response DTO
import com.cedric.Eventra.dto.CreateChatRoomRequestDTO;
import com.cedric.Eventra.dto.SendMessageRequestDTO;
import com.cedric.Eventra.entity.User;

public interface ChatService {

    Response getOrCreateChatRoom(CreateChatRoomRequestDTO requestDTO);

    Response getMyChatRooms();

    Response getChatMessagesForRoom(Long chatRoomId); // No pagination for simplicity

    ChatMessageDTO saveAndPrepareMessage(SendMessageRequestDTO messageRequest, User sender);
}