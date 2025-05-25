package com.cedric.Eventra.service;


import com.cedric.Eventra.dto.PaymentDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;
import com.cedric.Eventra.dto.SimulatedPaymentRequestDTO;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.Payment;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.PaymentStatus;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.ResourceNotFoundException; // Ensure you have this
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.PaymentRepository;
import com.cedric.Eventra.repository.UserRepository;
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

import java.math.BigDecimal;
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
    private final UserRepository userRepository;

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

    @Override
    public Response processSimulatedSubscriptionFee(SimulatedPaymentRequestDTO paymentRequest) {
        User providerUser;
        try {
            providerUser = userService.getCurrentLoggedInUser();
        } catch (Exception e) {
            return Response.builder().status(HttpStatus.UNAUTHORIZED.value()).message("User not authenticated.").build();
        }

        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Only service providers can pay a subscription fee.")
                    .build();
        }

        if (providerUser.getIsActive()) { // Assuming getIsActive() exists
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("This service provider account is already active.")
                    .user(modelMapper.map(providerUser, com.cedric.Eventra.dto.UserDTO.class))
                    .build();
        }

        // --- Simulation Logic (copied from your booking payment simulation) ---
        boolean paymentSuccessful;
        String failureReason = null;
        final BigDecimal SUBSCRIPTION_FEE = new BigDecimal("20.00");

        if (paymentRequest.getDummyCardNumber().endsWith("0000")) {
            paymentSuccessful = true;
        } else if (paymentRequest.getDummyCardNumber().endsWith("9999")) {
            paymentSuccessful = false;
            failureReason = "Simulated subscription payment decline by bank (test card).";
        } else {
            paymentSuccessful = Math.random() < 0.9; // High success rate for general testing
            if (!paymentSuccessful) {
                failureReason = "Simulated random subscription payment failure.";
            }
        }
        // --- End Simulation Logic ---

        Payment payment = Payment.builder()
                .user(providerUser)
                .bookingReference(null) // No booking reference for subscription
                .amount(SUBSCRIPTION_FEE)
                .paymentDate(LocalDateTime.now())
                .transactionId("SUB-SIM-" + UUID.randomUUID().toString().toUpperCase())
                .build();

        if (paymentSuccessful) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.info("Simulated subscription fee successful for provider: {}. Transaction ID: {}", providerUser.getEmail(), payment.getTransactionId());

            // Activate the provider's account
            // This might involve calling a method in UserService, e.g., userService.activateProviderAccount(providerUser);
            // For now, directly update and save if UserService doesn't have such a method yet.
            providerUser.setIsActive(true); // Assuming setIsActive() exists
            User updatedProviderUser = userRepository.save(providerUser); // Assuming you have UserRepository here or in UserService

            // Optional: Send "Subscription Activated" notification
            // notificationService.sendSubscriptionActivatedNotification(updatedProviderUser);

            return Response.builder()
                    .status(HttpStatus.OK.value())
                    .message("Subscription fee paid successfully. Your provider account is now active.")
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .user(modelMapper.map(updatedProviderUser, com.cedric.Eventra.dto.UserDTO.class)) // Show updated user
                    .build();
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);
            log.warn("Simulated subscription fee FAILED for provider: {}. Reason: {}", providerUser.getEmail(), failureReason);

            // Optional: Send "Subscription Payment Failed" notification
            // notificationService.sendSubscriptionPaymentFailedNotification(providerUser, failureReason);

            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Simulated subscription payment failed: " + failureReason)
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .user(modelMapper.map(providerUser, com.cedric.Eventra.dto.UserDTO.class)) // Show original user state
                    .build();
        }
    }
}
