package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;
import com.cedric.Eventra.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Endpoint for a customer to simulate making a payment for their booking.
     * Booking must be in ACCEPTED_AWAITING_PAYMENT state.
     *
     * @param bookingReference The reference of the booking to pay for.
     * @param paymentRequest   DTO containing dummy card details for simulation.
     * @return ResponseEntity containing the standard Response object.
     */
    @PostMapping("/booking/{bookingReference}/simulate")
    @PreAuthorize("isAuthenticated()") // Customer must be authenticated
    public ResponseEntity<Response> simulatePaymentForBooking(
            @PathVariable String bookingReference,
            @Valid @RequestBody SimulatedPaymentRequestDTO paymentRequest) {
        Response serviceResponse = paymentService.processSimulatedPayment(bookingReference, paymentRequest);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint to get payment details for a booking.
     *
     * @param bookingReference The reference of the booking.
     * @return ResponseEntity containing payment details.
     */
    @GetMapping("/booking/{bookingReference}")
    @PreAuthorize("isAuthenticated()") // User should be customer or provider of the booking
    public ResponseEntity<Response> getPaymentDetails(@PathVariable String bookingReference) {
        // Service layer should implement authorization logic if needed (e.g., only customer or provider can see)
        Response serviceResponse = paymentService.getPaymentDetailsForBooking(bookingReference);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}