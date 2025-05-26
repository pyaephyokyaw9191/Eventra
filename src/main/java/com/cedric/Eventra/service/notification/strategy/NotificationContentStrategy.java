package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;

// Defines the contract for generating notification content
public interface NotificationContentStrategy {
    // Generates the subject line for the notification
    // @param booking The booking context for the notification
    // @param recipient The user who will receive the notification
    // @return The generated subject string
    String generateSubject(Booking booking, User recipient);

    // Generates the main body/content of the notification
    // @param booking The booking context for the notification
    // @param recipient The user who will receive the notification
    // @return The generated body string
    String generateBody(Booking booking, User recipient);

    // (Optional) If determining the actual recipient User entity itself has complex logic
    // based on the notification type and booking, you could add a method here:
    // User determineRecipient(Booking booking, NotificationType type);
    // For now, we'll assume the recipient is determined before calling the strategy.
}