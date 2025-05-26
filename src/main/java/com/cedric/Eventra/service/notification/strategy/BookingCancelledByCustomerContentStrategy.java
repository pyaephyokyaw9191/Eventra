package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import org.springframework.stereotype.Component;

@Component("BOOKING_CANCELLED_BY_CUSTOMER_STRATEGY")
public class BookingCancelledByCustomerContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient /* provider */) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Cancelled by Customer";
        return "Booking Cancelled by Customer: " + booking.getOfferedService().getName() + " (Ref: " + booking.getBookingReference() + ")";
    }

    @Override
    public String generateBody(Booking booking, User recipient /* provider */) {
        if (booking == null || booking.getOfferedService() == null || booking.getUser() == null || recipient == null) {
            return "A booking for your service has been cancelled by the customer. Please log in for details.";
        }
        return String.format(
                "Hello %s,\n\nThe booking for your service '%s' (Ref: %s) made by %s %s has been cancelled by the customer.",
                recipient.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference(),
                booking.getUser().getFirstName(),
                booking.getUser().getLastName()
        );
    }
}