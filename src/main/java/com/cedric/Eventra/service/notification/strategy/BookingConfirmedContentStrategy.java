package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import org.springframework.stereotype.Component;

@Component("BOOKING_CONFIRMED_STRATEGY")
public class BookingConfirmedContentStrategy implements NotificationContentStrategy {

    @Override
    public String generateSubject(Booking booking, User recipient) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Confirmed";
        String serviceName = booking.getOfferedService().getName();
        String bookingRef = booking.getBookingReference();

        if (recipient.getRole() == UserRole.CUSTOMER) {
            return "Booking Confirmed: " + serviceName + " (Ref: " + bookingRef + ")";
        } else if (recipient.getRole() == UserRole.SERVICE_PROVIDER) {
            User customer = booking.getUser();
            return "Booking Confirmed: " + serviceName + " (Ref: " + bookingRef + ") with " + (customer != null ? customer.getFirstName() : "Customer");
        }
        return "Booking Confirmed: " + serviceName;
    }

    @Override
    public String generateBody(Booking booking, User recipient) {
        if (booking == null || booking.getOfferedService() == null || recipient == null) {
            return "A booking has been confirmed. Please log in for details.";
        }
        String serviceName = booking.getOfferedService().getName();
        String bookingRef = booking.getBookingReference();

        if (recipient.getRole() == UserRole.CUSTOMER) {
            return String.format(
                    "Hello %s,\n\nYour booking for '%s' is confirmed! We look forward to serving you.\nBooking Reference: %s",
                    recipient.getFirstName(),
                    serviceName,
                    bookingRef
            );
        } else if (recipient.getRole() == UserRole.SERVICE_PROVIDER) {
            User customer = booking.getUser();
            if (customer == null) return "A booking for your service has been confirmed. Login for details.";
            return String.format(
                    "Hello %s,\n\nThe booking for '%s' (Ref: %s) by %s %s is now confirmed.",
                    recipient.getFirstName(),
                    serviceName,
                    bookingRef,
                    customer.getFirstName(),
                    customer.getLastName()
            );
        }
        return "A booking has been confirmed. Please log in for details.";
    }
}