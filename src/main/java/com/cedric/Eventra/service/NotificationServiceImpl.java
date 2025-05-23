package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.NotificationDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.Notification;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.NotificationType;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public NotificationDTO createNotification(User recipientUser, NotificationType type, String subject, String body, String bookingReference) {
        if (recipientUser == null) {
            throw new IllegalArgumentException("Recipient user cannot be null for notification.");
        }

        Notification notification = Notification.builder()
                .recipientUser(recipientUser) // Set the User entity directly
                .notificationType(type)
                .subject(subject)
                .body(body)
                .bookingReference(bookingReference)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created: type={}, recipientUserId={}", type, recipientUser.getId());

        // Later add for email **
        // if (emailService != null) {
        //     emailService.sendSimpleMail(recipientUser.getEmail(), subject, body);
        // }

        return modelMapper.map(savedNotification, NotificationDTO.class);
    }

    @Override
    @Transactional
    public void sendNewBookingRequestNotification(Booking booking) {
        if (booking == null || booking.getOfferedService() == null || booking.getOfferedService().getProvider() == null || booking.getUser() == null) {
            log.error("Cannot send NewBookingRequestNotification, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User provider = booking.getOfferedService().getProvider();
        User customer = booking.getUser();
        String subject = "New Booking Request: " + booking.getRequestName() + " for " + booking.getOfferedService().getName();
        String body = String.format(
                "Hello %s,\n\nYou have a new booking request from %s %s for your service '%s'.\nRequest: %s\nBooking Reference: %s\nPlease review and respond.",
                provider.getFirstName(),
                customer.getFirstName(),
                customer.getLastName(),
                booking.getOfferedService().getName(),
                booking.getRequestName(),
                booking.getBookingReference()
        );
        createNotification(provider, NotificationType.NEW_BOOKING_REQUEST, subject, body, booking.getBookingReference());
    }

    @Override
    public void sendBookingAcceptedNotification(Booking booking) {
        if (booking == null || booking.getUser() == null || booking.getOfferedService() == null) {
            log.error("Cannot send BookingAcceptedNotification, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        String subject = "Booking Request Accepted: " + booking.getOfferedService().getName();
        String body = String.format(
                "Hello %s,\n\nYour booking request for '%s' (Ref: %s) has been accepted by the provider.\nPlease proceed with any pending actions (e.g., payment) to confirm.",
                customer.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
        createNotification(customer, NotificationType.BOOKING_REQUEST_ACCEPTED, subject, body, booking.getBookingReference());
    }

    @Override
    public void sendBookingRejectedNotification(Booking booking) {
        if (booking == null || booking.getUser() == null || booking.getOfferedService() == null) {
            log.error("Cannot send BookingRejectedNotification, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        String subject = "Booking Request Update: " + booking.getOfferedService().getName();
        String body = String.format(
                "Hello %s,\n\nRegarding your booking request for '%s' (Ref: %s), the provider was unable to accept it at this time.\nWe encourage you to browse for other available services.",
                customer.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
        createNotification(customer, NotificationType.BOOKING_REQUEST_REJECTED, subject, body, booking.getBookingReference());
    }

    @Override
    public void sendBookingConfirmedNotification(Booking booking) {
        if (booking == null || booking.getUser() == null || booking.getOfferedService() == null || booking.getOfferedService().getProvider() == null) {
            log.error("Cannot send BookingConfirmedNotification, booking details are incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        User provider = booking.getOfferedService().getProvider();
        String serviceName = booking.getOfferedService().getName();
        String bookingRef = booking.getBookingReference();

        // Notification to Customer
        String customerSubject = "Booking Confirmed: " + serviceName + " (Ref: " + bookingRef + ")";
        String customerBody = "Hello " + customer.getFirstName() + ",\n\nYour booking for '" + serviceName +
                "' is confirmed! We look forward to serving you.\nBooking Reference: " + bookingRef;
        createNotification(customer, NotificationType.BOOKING_CONFIRMED, customerSubject, customerBody, bookingRef);

        // Notification to Provider
        String providerSubject = "Booking Confirmed: " + serviceName + " (Ref: " + bookingRef + ") with " + customer.getFirstName();
        String providerBody = "Hello " + provider.getFirstName() + ",\n\nThe booking for '" + serviceName +
                "' (Ref: " + bookingRef + ") by " + customer.getFirstName() + " " + customer.getLastName() + " is now confirmed.";
        createNotification(provider, NotificationType.BOOKING_CONFIRMED, providerSubject, providerBody, bookingRef);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMyNotifications() {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            log.warn("Attempt to fetch notifications for unauthenticated user.", e);
            return Response.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated.")
                    .build();
        }

        List<Notification> notifications = notificationRepository.findByRecipientUserOrderByCreatedAtDesc(currentUser);

        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(notificationDTOs.isEmpty() ? "You have no notifications." : "Notifications retrieved successfully.")
                .notifications(notificationDTOs)
                .build();
    }

    @Override
    @Transactional
    public Response deleteMyNotification(Long notificationId) {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            log.warn("Attempt to delete notification for unauthenticated user.", e);
            return Response.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated.")
                    .build();
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempting to delete notification {} not belonging to them.", currentUser.getId(), notificationId);
            throw new UnauthorizedException("You are not authorized to delete this notification.");
        }

        notificationRepository.delete(notification);
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Notification deleted successfully.")
                .build();
    }
}
