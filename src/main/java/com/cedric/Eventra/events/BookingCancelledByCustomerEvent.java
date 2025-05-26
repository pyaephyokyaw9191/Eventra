package com.cedric.Eventra.events;

import com.cedric.Eventra.entity.Booking;
import org.springframework.context.ApplicationEvent;

public class BookingCancelledByCustomerEvent extends ApplicationEvent {
    private Booking booking;

    public BookingCancelledByCustomerEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }

    public Booking getBooking() {
        return booking;
    }
}