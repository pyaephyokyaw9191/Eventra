package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.ChatMessage;
import com.cedric.Eventra.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Finds all messages for a given chat room, ordered by timestamp ascending (oldest first).
     * For a simplified version, we are not using pagination here.
     * @param chatRoom The chat room.
     * @return A list of chat messages.
     */
    List<ChatMessage> findByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);

    /**
     * Optional: Find the most recent message for a chat room.
     * @param chatRoom The chat room.
     * @return An Optional containing the last ChatMessage if it exists.
     */
    Optional<ChatMessage> findTopByChatRoomOrderByTimestampDesc(ChatRoom chatRoom);
}