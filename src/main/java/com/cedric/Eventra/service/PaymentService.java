package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;

public interface PaymentService {

    /**
     * Processes a simulated payment for a given booking.
     *
     * @param bookingReference The reference of the booking to pay for.
     * @param paymentRequest   DTO containing dummy payment details for simulation.
     * @return Response object indicating the outcome of the payment simulation and updated booking status.
     */
    Response processSimulatedPayment(String bookingReference, SimulatedPaymentRequestDTO paymentRequest);

    /**
     * Retrieves payment details for a given booking reference.
     * (Optional method, useful for checking payment status)
     * @param bookingReference The booking reference.
     * @return Response object with payment details.
     */
    Response getPaymentDetailsForBooking(String bookingReference);
}