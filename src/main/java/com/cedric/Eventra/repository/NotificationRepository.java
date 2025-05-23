package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.Notification;
import com.cedric.Eventra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds notifications for a specific user, ordered by creation date descending.
     * @param recipientUser The User entity who is the recipient.
     * @return A list of notifications.
     */
    List<Notification> findByRecipientUserOrderByCreatedAtDesc(User recipientUser);


}