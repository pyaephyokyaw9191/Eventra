package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import org.springframework.stereotype.Component;

@Component("NEW_BOOKING_REQUEST_STRATEGY")
public class NewBookingRequestContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient /* provider */) {
        if (booking == null || booking.getOfferedService() == null) return "New Booking Request";
        return "New Booking Request: " + booking.getRequestName() + " for " + booking.getOfferedService().getName();
    }

    @Override
    public String generateBody(Booking booking, User recipient /* provider */) {
        if (booking == null || booking.getUser() == null || booking.getOfferedService() == null || recipient == null) {
            return "You have a new booking request. Please log in to view details.";
        }
        User customer = booking.getUser();
        return String.format(
                "Hello %s,\n\nYou have a new booking request from %s %s for your service '%s'.\nRequest Name: %s\nBooking Reference: %s\nPlease review and respond.",
                recipient.getFirstName(),
                customer.getFirstName(),
                customer.getLastName(),
                booking.getOfferedService().getName(),
                booking.getRequestName() != null ? booking.getRequestName() : "N/A",
                booking.getBookingReference()
        );
    }
}