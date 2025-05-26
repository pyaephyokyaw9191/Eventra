package com.cedric.Eventra.service.notification.strategy;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.events.BookingCancelledByProviderEvent; // Assuming event carries reason
import org.springframework.stereotype.Component;

@Component("BOOKING_CANCELLED_BY_PROVIDER_STRATEGY")
public class BookingCancelledByProviderContentStrategy implements NotificationContentStrategy {

    private String reason; // To hold reason if passed contextually

    // If you pass reason through a custom event that strategy is aware of.
    // This is a bit of a workaround if the strategy interface is generic.
    // A better way might be to pass a context object to generateSubject/Body.
    public void setContext(BookingCancelledByProviderEvent event) {
        this.reason = event.getReason();
    }


    @Override
    public String generateSubject(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null) return "Booking Cancelled by Provider";
        return "Booking Cancelled by Provider: " + booking.getOfferedService().getName() + " (Ref: " + booking.getBookingReference() + ")";
    }

    @Override
    public String generateBody(Booking booking, User recipient /* customer */) {
        if (booking == null || booking.getOfferedService() == null || recipient == null) {
            return "A booking you made has been cancelled by the provider. Please log in for details.";
        }
        String bodyStr = String.format(
                "Hello %s,\n\nYour booking for '%s' (Ref: %s) has been cancelled by the provider.",
                recipient.getFirstName(),
                booking.getOfferedService().getName(),
                booking.getBookingReference()
        );
        if (this.reason != null && !this.reason.isEmpty()) {
            bodyStr += "\nReason: " + this.reason;
        }
        // Reset reason for next use if this strategy instance is reused (though Spring beans are singletons)
        this.reason = null;
        return bodyStr;
    }
}