package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.ChatRoom;
import com.cedric.Eventra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "(cr.participant1 = :user1 AND cr.participant2 = :user2) OR " +
            "(cr.participant1 = :user2 AND cr.participant2 = :user1)")
    Optional<ChatRoom> findChatRoomByParticipants(@Param("user1") User user1, @Param("user2") User user2);

    // Fetches rooms for a user, ordered by most recent activity
    List<ChatRoom> findByParticipant1OrParticipant2OrderByLastMessageAtDesc(User participant1, User participant2);

    // If have a booking link and need to find a chat room by it
    Optional<ChatRoom> findByBookingId(Long bookingId);
}