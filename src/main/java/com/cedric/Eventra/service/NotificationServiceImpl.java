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
import com.cedric.Eventra.events.*; // Import all your event classes
import com.cedric.Eventra.service.notification.strategy.NotificationContentStrategy; // Import strategy interface
import com.cedric.Eventra.service.notification.strategy.BookingCancelledByProviderContentStrategy; // Specific import for reason handling

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final Map<String, NotificationContentStrategy> contentStrategies; // Injected by Spring

    // Helper to get strategy from the map
    private NotificationContentStrategy getStrategy(NotificationType type) {
        String strategyName = type.name() + "_STRATEGY";
        NotificationContentStrategy strategy = contentStrategies.get(strategyName);
        if (strategy == null) {
            log.error("No content strategy found for NotificationType: {}. Notifications for this type may be incorrect.", type);
            // Fallback or throw an exception. For now, let's return a default or throw.
            // This default strategy should exist as a bean if used.
            // return contentStrategies.get("DEFAULT_CONTENT_STRATEGY");
            throw new IllegalArgumentException("Missing NotificationContentStrategy for type: " + type);
        }
        return strategy;
    }

    @Override
    @Transactional
    public NotificationDTO createNotification(User recipientUser, NotificationType type, String subject, String body, String bookingReference) {
        if (recipientUser == null) {
            log.error("Recipient user cannot be null for notification. Type: {}, Subject: {}, BookingRef: {}", type, subject, bookingReference);
            throw new IllegalArgumentException("Recipient user cannot be null for notification.");
        }

        Notification notification = Notification.builder()
                .recipientUser(recipientUser)
                .notificationType(type)
                .subject(subject)
                .body(body)
                .bookingReference(bookingReference)
                .build(); // createdAt will be set by @PrePersist

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created: id={}, type={}, recipientUserId={}", savedNotification.getId(), type, recipientUser.getId());
        return modelMapper.map(savedNotification, NotificationDTO.class);
    }

    // --- Event Listener Methods using Strategies ---

    @EventListener
    @Async
    @Transactional
    public void handleBookingCreated(BookingCreatedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingCreatedEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getOfferedService() == null || booking.getOfferedService().getProvider() == null) {
            log.error("Cannot process BookingCreatedEvent, crucial booking details missing. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User provider = booking.getOfferedService().getProvider();
        NotificationType type = NotificationType.NEW_BOOKING_REQUEST;
        NotificationContentStrategy strategy = getStrategy(type);

        String subject = strategy.generateSubject(booking, provider);
        String body = strategy.generateBody(booking, provider);
        createNotification(provider, type, subject, body, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingAccepted(BookingAcceptedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingAcceptedEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getUser() == null) {
            log.error("Cannot process BookingAcceptedEvent, customer or booking details missing. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        NotificationType type = NotificationType.BOOKING_REQUEST_ACCEPTED;
        NotificationContentStrategy strategy = getStrategy(type);

        String subject = strategy.generateSubject(booking, customer);
        String body = strategy.generateBody(booking, customer);
        createNotification(customer, type, subject, body, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingRejected(BookingRejectedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingRejectedEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getUser() == null) {
            log.error("Cannot process BookingRejectedEvent, customer or booking details missing. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        NotificationType type = NotificationType.BOOKING_REQUEST_REJECTED;
        NotificationContentStrategy strategy = getStrategy(type);

        String subject = strategy.generateSubject(booking, customer);
        String body = strategy.generateBody(booking, customer);
        createNotification(customer, type, subject, body, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingConfirmedEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getUser() == null || booking.getOfferedService() == null || booking.getOfferedService().getProvider() == null) {
            log.error("Cannot process BookingConfirmedEvent, booking details are incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        User provider = booking.getOfferedService().getProvider();
        NotificationType type = NotificationType.BOOKING_CONFIRMED;
        NotificationContentStrategy strategy = getStrategy(type);

        // Notify Customer
        String customerSubject = strategy.generateSubject(booking, customer);
        String customerBody = strategy.generateBody(booking, customer);
        createNotification(customer, type, customerSubject, customerBody, booking.getBookingReference());

        // Notify Provider
        String providerSubject = strategy.generateSubject(booking, provider);
        String providerBody = strategy.generateBody(booking, provider);
        createNotification(provider, type, providerSubject, providerBody, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingCancelledByCustomer(BookingCancelledByCustomerEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingCancelledByCustomerEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getOfferedService() == null || booking.getOfferedService().getProvider() == null) {
            log.error("Cannot process BookingCancelledByCustomerEvent, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User provider = booking.getOfferedService().getProvider();
        NotificationType type = NotificationType.BOOKING_CANCELLED_BY_USER;
        NotificationContentStrategy strategy = getStrategy(type);

        String subject = strategy.generateSubject(booking, provider);
        String body = strategy.generateBody(booking, provider);
        createNotification(provider, type, subject, body, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingCancelledByProvider(BookingCancelledByProviderEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingCancelledByProviderEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getUser() == null) {
            log.error("Cannot process BookingCancelledByProviderEvent, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        NotificationType type = NotificationType.BOOKING_CANCELLED_BY_PROVIDER;
        NotificationContentStrategy strategy = getStrategy(type);

        // Special handling for strategies needing more context like 'reason'
        if (strategy instanceof BookingCancelledByProviderContentStrategy) {
            ((BookingCancelledByProviderContentStrategy) strategy).setContext(event);
        }

        String subject = strategy.generateSubject(booking, customer);
        String body = strategy.generateBody(booking, customer);
        createNotification(customer, type, subject, body, booking.getBookingReference());
    }

    @EventListener
    @Async
    @Transactional
    public void handleBookingCompleted(BookingCompletedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingCompletedEvent for booking ref: {}", booking.getBookingReference());
        if (booking == null || booking.getUser() == null) {
            log.error("Cannot process BookingCompletedEvent, booking details incomplete. Booking ID: {}", booking != null ? booking.getId() : "null");
            return;
        }
        User customer = booking.getUser();
        NotificationType type = NotificationType.BOOKING_COMPLETED;
        NotificationContentStrategy strategy = getStrategy(type);

        String subject = strategy.generateSubject(booking, customer);
        String body = strategy.generateBody(booking, customer);
        createNotification(customer, type, subject, body, booking.getBookingReference());
    }

    // --- Deprecated/Legacy direct call methods (can be removed if interface is updated) ---
    // These are no longer the primary way notifications are triggered for booking events
    @Override
    public void sendNewBookingRequestNotification(Booking booking) {
        log.warn("Deprecated: Direct call to sendNewBookingRequestNotification for booking ref {}. Event-driven approach is preferred.", booking.getBookingReference());
        // This could call the event handler or be removed.
        // handleBookingCreated(new BookingCreatedEvent(this, booking)); // Avoid re-triggering if already event-driven
    }
    @Override
    public void sendBookingAcceptedNotification(Booking booking) {
        log.warn("Deprecated: Direct call to sendBookingAcceptedNotification for booking ref {}. Event-driven approach is preferred.", booking.getBookingReference());
    }
    @Override
    public void sendBookingRejectedNotification(Booking booking) {
        log.warn("Deprecated: Direct call to sendBookingRejectedNotification for booking ref {}. Event-driven approach is preferred.", booking.getBookingReference());
    }
    @Override
    public void sendBookingConfirmedNotification(Booking booking) {
        log.warn("Deprecated: Direct call to sendBookingConfirmedNotification for booking ref {}. Event-driven approach is preferred.", booking.getBookingReference());
    }


    // --- Other existing methods from NotificationServiceImpl ---
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

        // Make sure your Response DTO has a field like 'notifications' (plural)
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

    @Override
    @Transactional(readOnly = true)
    public Response getMyNotificationById(Long notificationId) {
        User currentUser;
        try {
            currentUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            log.warn("Attempt to fetch notification by ID for unauthenticated user.", e);
            return Response.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated.")
                    .build();
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);

        if (notification == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Notification not found with ID: " + notificationId)
                    .build();
        }

        if (!notification.getRecipientUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to access notification ID {} not belonging to them.", currentUser.getId(), notificationId);
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to view this notification.")
                    .build();
        }

        NotificationDTO notificationDTO = modelMapper.map(notification, NotificationDTO.class);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Notification retrieved successfully.")
                .notification(notificationDTO) // Use 'notification' field (singular)
                .build();
    }
}