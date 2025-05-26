package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.BookingReference;

public interface BookingService {

    Response getMyBookingAsCustomer();

    Response getMyBookingAsProvider();

    Response getBookingByReference(String bookingReference);
}
