package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import org.springframework.stereotype.Component;

@Component("BOOKING_REQUEST_REJECTED_STRATEGY")
public class BookingRejectedContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Request Update";
        return "Booking Request Update: " + booking.getOfferedService().getName();
    }

    @Override
    public String generateBody(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null || recipient == null) {
            return "There's an update on your booking request. Please log in for details.";
        }
        return String.format(
                "Hello %s,\n\nRegarding your booking request for '%s' (Ref: %s), the provider was unable to accept it at this time.\nWe encourage you to browse for other available services.",
                recipient.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
    }
}