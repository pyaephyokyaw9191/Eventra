package com.cedric.Eventra.events;

import com.cedric.Eventra.entity.Booking;
import org.springframework.context.ApplicationEvent;

public class BookingCancelledByProviderEvent extends ApplicationEvent {
    private Booking booking;
    private String reason; // Optional: include reason if needed for notification

    public BookingCancelledByProviderEvent(Object source, Booking booking, String reason) {
        super(source);
        this.booking = booking;
        this.reason = reason;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getReason() {
        return reason;
    }
}