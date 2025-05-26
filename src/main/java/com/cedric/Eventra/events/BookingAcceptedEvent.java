package com.cedric.Eventra.events;

import com.cedric.Eventra.entity.Booking;
import org.springframework.context.ApplicationEvent;

public class BookingAcceptedEvent extends ApplicationEvent {
    private Booking booking;

    public BookingAcceptedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }

    public Booking getBooking() {
        return booking;
    }
}