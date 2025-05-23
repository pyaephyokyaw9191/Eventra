package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.NotificationDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.NotificationType;

public interface NotificationService {

    /**
     * Core method to create and persist a notification.
     * Typically called internally by other services.
     *
     * @param recipientUser    The User entity who is the recipient.
     * @param type             The type of the notification.
     * @param subject          The subject of the notification.
     * @param body             The main content of the notification.
     * @param bookingReference booking reference if related to a booking.
     * @return The created NotificationDTO
     */
    NotificationDTO createNotification(User recipientUser,
                                       NotificationType type, String subject,
                                       String body, String bookingReference);

    void sendNewBookingRequestNotification(Booking booking);

    void sendBookingAcceptedNotification(Booking booking);

    void sendBookingRejectedNotification(Booking booking);

    void sendBookingConfirmedNotification(Booking booking);
    // Add more specific notification event methods as needed (e.g., payment success, cancellation)

    // --- Methods for users to manage their notifications (exposed via Controller) ---
    Response getMyNotifications();

    Response deleteMyNotification(Long notificationId);
}
