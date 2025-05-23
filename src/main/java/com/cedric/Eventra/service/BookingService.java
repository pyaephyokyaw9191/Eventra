package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.BookingReference;

public interface BookingService {

    Response createBooking(BookingDTO bookingDTO);

    Response providerAcceptBooking(String bookingReference);

    Response providerRejectBooking(String bookingReference);

    Response customerCancelBooking(String bookingReference);

    Response providerCancelBooking(String bookingReference, String reason);

    Response confirmBookingPayment(String bookingReference);

    Response markBookingAsCompleted(String bookingReference);

    Response getMyBookingAsCustomer();

    Response getMyBookingAsProvider();

    Response getBookingByReference(String bookingReference);
}
