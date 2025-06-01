package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import org.springframework.stereotype.Component;

@Component("BOOKING_COMPLETED_STRATEGY")
public class BookingCompletedContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Completed";
        return "Booking Completed: " + booking.getOfferedService().getName() + " (Ref: " + booking.getBookingReference() + ")";
    }

    @Override
    public String generateBody(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null || recipient == null) {
            return "Your booking has been marked as completed. We hope you enjoyed the service!";
        }
        return String.format(
                "Hello %s,\n\nYour booking for '%s' (Ref: %s) has been marked as completed by the provider.\nWe hope you enjoyed the service! Please consider leaving a review.",
                recipient.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
    }
}