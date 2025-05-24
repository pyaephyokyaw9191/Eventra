package com.cedric.Eventra.service;


import com.cedric.Eventra.dto.PaymentDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.Payment;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.PaymentStatus;
import com.cedric.Eventra.exception.ResourceNotFoundException; // Ensure you have this
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.PaymentRepository;
import com.cedric.Eventra.service.BookingService;
import com.cedric.Eventra.service.NotificationService;
import com.cedric.Eventra.service.PaymentService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService; // To call confirmBookingPayment
    private final NotificationService notificationService; // To send payment notifications
    private final ModelMapper modelMapper;
    private final UserService userService; // To get current user for payment record

    @Override
    @Transactional
    public Response processSimulatedPayment(String bookingReference, SimulatedPaymentRequestDTO paymentRequest) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        if (booking.getStatus() != BookingStatus.ACCEPTED_AWAITING_PAYMENT) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking is not in the correct state (ACCEPTED_AWAITING_PAYMENT) for payment. Current status: " + booking.getStatus())
                    .booking(modelMapper.map(booking, com.cedric.Eventra.dto.BookingDTO.class)) // Using fully qualified name
                    .build();
        }

        // --- Simulation Logic ---
        boolean paymentSuccessful;
        String failureReason = null;
        // Example rule: Card number ending with "0000" succeeds, "9999" fails for specific testing.
        // Any other card number can have a random outcome or default to success for easier testing.
        if (paymentRequest.getDummyCardNumber().endsWith("0000")) {
            paymentSuccessful = true;
        } else if (paymentRequest.getDummyCardNumber().endsWith("9999")) {
            paymentSuccessful = false;
            failureReason = "Simulated payment decline by bank (test card).";
        } else {
            // Default simulation: 70% chance of success for other cards
            paymentSuccessful = Math.random() < 0.9; // High success rate for general testing
            if (!paymentSuccessful) {
                failureReason = "Simulated random payment failure.";
            }
        }

        // --- End Simulation Logic ---
        Payment payment = Payment.builder()
                .user(booking.getUser()) // Associate payment with the user who made the booking
                .bookingReference(bookingReference) // Link to booking
                .amount(booking.getPrice())
                .paymentDate(LocalDateTime.now())
                .transactionId("SIM-" + UUID.randomUUID().toString().toUpperCase()) // Simulated transaction ID
                .build();

        if (paymentSuccessful) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.info("Simulated payment successful for booking ref: {}. Transaction ID: {}", bookingReference, payment.getTransactionId());

            // Call BookingService to update booking status to CONFIRMED
            // This call itself will handle notifications for booking confirmation.
            Response bookingConfirmationResponse = bookingService.confirmBookingPayment(bookingReference);

            // If confirmBookingPayment already sends good response, we can use that or build a new one.
            // Let's build a payment-specific success response.
            return Response.builder()
                    .status(HttpStatus.OK.value())
                    .message("Simulated payment successful. Booking confirmed.")
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .booking(bookingConfirmationResponse.getBooking()) // Get updated booking DTO from the response
                    .build();
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);
            log.warn("Simulated payment FAILED for booking ref: {}. Reason: {}", bookingReference, failureReason);

            // Optionally update booking status to PAYMENT_FAILED
            booking.setStatus(BookingStatus.PAYMENT_FAILED); // Ensure this status is handled in your flow
            bookingRepository.save(booking);

            // Send payment failure notification (optional)
            // notificationService.sendPaymentFailedNotification(booking, failureReason);

            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // Or another appropriate error code for payment failure
                    .message("Simulated payment failed: " + failureReason)
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .booking(modelMapper.map(booking, com.cedric.Eventra.dto.BookingDTO.class))
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getPaymentDetailsForBooking(String bookingReference) {
        // This assumes one primary payment attempt per booking reference for simplicity.
        List<Payment> payments = paymentRepository.findByBookingReferenceOrderByPaymentDateDesc(bookingReference);

        if (payments.isEmpty()) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("No payment details found for booking reference: " + bookingReference)
                    .build();
        }

        List<PaymentDTO> paymentDTOs = payments.stream()
                .map(p -> modelMapper.map(p, PaymentDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Payment details retrieved successfully.")
                .payments(paymentDTOs) // Use the 'payments' field in your Response DTO
                .build();
    }
}
