package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import org.springframework.stereotype.Component;

@Component("BOOKING_REQUEST_ACCEPTED_STRATEGY")
public class BookingAcceptedContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Request Accepted";
        return "Booking Request Accepted: " + booking.getOfferedService().getName();
    }

    @Override
    public String generateBody(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null || recipient == null) {
            return "Your booking request has been accepted. Please log in for details.";
        }
        return String.format(
                "Hello %s,\n\nYour booking request for '%s' (Ref: %s) has been accepted by the provider.\nPlease proceed with any pending actions (e.g., payment) to confirm.",
                recipient.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
    }
}