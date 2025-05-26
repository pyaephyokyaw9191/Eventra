// In PaymentServiceImpl.java
package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.*;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.Payment;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.PaymentStatus;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.PaymentRepository;
import com.cedric.Eventra.repository.UserRepository;
// Import the command
import com.cedric.Eventra.service.booking.command.ConfirmBookingPaymentCommand;
// Import ApplicationContext to get command beans
import org.springframework.context.ApplicationContext;
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
    // BookingService is no longer used for confirmBookingPayment directly by this service.
    // private final BookingService bookingService; 
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext; // Added to get command beans

    @Override
    @Transactional
    public Response processSimulatedPayment(String bookingReference, SimulatedPaymentRequestDTO paymentRequest) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        if (booking.getStatus() != BookingStatus.ACCEPTED_AWAITING_PAYMENT) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking is not in the correct state (ACCEPTED_AWAITING_PAYMENT) for payment. Current status: " + booking.getStatus())
                    .booking(modelMapper.map(booking, com.cedric.Eventra.dto.BookingDTO.class))
                    .build();
        }

        boolean paymentSuccessful;
        String failureReason = null;
        if (paymentRequest.getDummyCardNumber().endsWith("0000")) {
            paymentSuccessful = true;
        } else if (paymentRequest.getDummyCardNumber().endsWith("9999")) {
            paymentSuccessful = false;
            failureReason = "Simulated payment decline by bank (test card).";
        } else {
            paymentSuccessful = Math.random() < 0.9;
            if (!paymentSuccessful) {
                failureReason = "Simulated random payment failure.";
            }
        }

        Payment payment = Payment.builder()
                .user(booking.getUser())
                .bookingReference(bookingReference)
                .amount(booking.getPrice())
                .paymentDate(LocalDateTime.now())
                .transactionId("SIM-" + UUID.randomUUID().toString().toUpperCase())
                .build();

        if (paymentSuccessful) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.info("Simulated payment successful for booking ref: {}. Transaction ID: {}", bookingReference, payment.getTransactionId());

            // Execute ConfirmBookingPaymentCommand
            ConfirmBookingPaymentCommand command = applicationContext.getBean(ConfirmBookingPaymentCommand.class).init(bookingReference);
            Response bookingConfirmationResponse = command.execute();


            if (bookingConfirmationResponse.getStatus() != HttpStatus.OK.value()) {
                // Handle case where booking confirmation itself failed, though payment was successful.
                // This might indicate an issue with the command or state.
                // You might need to decide on a reconciliation strategy or log a critical error.
                log.error("Payment was successful for booking ref {} but booking confirmation failed: {}", bookingReference, bookingConfirmationResponse.getMessage());
                // Potentially reverse payment or mark for manual review
                return Response.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Payment successful, but booking confirmation encountered an issue: " + bookingConfirmationResponse.getMessage())
                        .payment(modelMapper.map(payment, PaymentDTO.class))
                        .build();
            }

            return Response.builder()
                    .status(HttpStatus.OK.value())
                    .message("Simulated payment successful. Booking confirmed.")
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .booking(bookingConfirmationResponse.getBooking())
                    .build();
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);
            log.warn("Simulated payment FAILED for booking ref: {}. Reason: {}", bookingReference, failureReason);

            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            bookingRepository.save(booking);

            // Potentially publish a PaymentFailedEvent here if other services need to react.
            // For now, directly calling notificationService as per original structure for non-booking events.
            // if(notificationService != null) { // Good practice to check if optional service is present
            //    notificationService.sendPaymentFailedNotification(booking, failureReason); // You'd need to create this method
            // }


            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Simulated payment failed: " + failureReason)
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .booking(modelMapper.map(booking, com.cedric.Eventra.dto.BookingDTO.class))
                    .build();
        }
    }

    // processSimulatedSubscriptionFee method would also need ApplicationContext if it calls commands
    @Override
    @Transactional
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

        if (providerUser.getIsActive()) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("This service provider account is already active.")
                    .user(modelMapper.map(providerUser, com.cedric.Eventra.dto.UserDTO.class))
                    .build();
        }

        boolean paymentSuccessful;
        String failureReason = null;
        final BigDecimal SUBSCRIPTION_FEE = new BigDecimal("20.00");

        if (paymentRequest.getDummyCardNumber().endsWith("0000")) {
            paymentSuccessful = true;
        } else if (paymentRequest.getDummyCardNumber().endsWith("9999")) {
            paymentSuccessful = false;
            failureReason = "Simulated subscription payment decline by bank (test card).";
        } else {
            paymentSuccessful = Math.random() < 0.9;
            if (!paymentSuccessful) {
                failureReason = "Simulated random subscription payment failure.";
            }
        }

        Payment payment = Payment.builder()
                .user(providerUser)
                .bookingReference(null)
                .amount(SUBSCRIPTION_FEE)
                .paymentDate(LocalDateTime.now())
                .transactionId("SUB-SIM-" + UUID.randomUUID().toString().toUpperCase())
                .build();

        if (paymentSuccessful) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.info("Simulated subscription fee successful for provider: {}. Transaction ID: {}", providerUser.getEmail(), payment.getTransactionId());

            providerUser.setIsActive(true);
            User updatedProviderUser = userRepository.save(providerUser);

            // if(notificationService != null) {
            //    notificationService.sendSubscriptionActivatedNotification(updatedProviderUser);  // You'd need to create this
            // }

            return Response.builder()
                    .status(HttpStatus.OK.value())
                    .message("Subscription fee paid successfully. Your provider account is now active.")
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .user(modelMapper.map(updatedProviderUser, com.cedric.Eventra.dto.UserDTO.class))
                    .build();
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);
            log.warn("Simulated subscription fee FAILED for provider: {}. Reason: {}", providerUser.getEmail(), failureReason);

            // if(notificationService != null) {
            //    notificationService.sendSubscriptionPaymentFailedNotification(providerUser, failureReason); // You'd need to create this
            // }

            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Simulated subscription payment failed: " + failureReason)
                    .payment(modelMapper.map(payment, PaymentDTO.class))
                    .user(modelMapper.map(providerUser, com.cedric.Eventra.dto.UserDTO.class))
                    .build();
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Response getPaymentDetailsForBooking(String bookingReference) {
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
                .payments(paymentDTOs)
                .build();
    }
}