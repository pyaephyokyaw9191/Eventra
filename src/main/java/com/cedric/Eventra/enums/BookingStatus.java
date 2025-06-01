package com.cedric.Eventra.enums;

public enum BookingStatus {
    PENDING,                      // Initial request from customer
    ACCEPTED_AWAITING_PAYMENT,  // Provider has accepted, waiting for customer payment
    CONFIRMED,                    // Payment successful, booking is fully confirmed
    REJECTED,                     // Provider rejected the request
    CANCELLED,                    // Cancelled by customer or provider
    COMPLETED,                    // Service has been rendered
    PAYMENT_FAILED                // Optional: If payment attempt fails
}
