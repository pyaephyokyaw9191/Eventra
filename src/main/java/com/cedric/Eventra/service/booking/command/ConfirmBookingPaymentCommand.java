package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.service.booking.command.BookingCommand;
import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingConfirmedEvent; // Ensure this event class exists
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
// UserService might not be needed if the actor isn't strictly checked here, but good for logging
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmBookingPaymentCommand implements BookingCommand {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;
    // private final UserService userService; // Optional: for logging who confirmed, if needed

    private String bookingReference;

    public ConfirmBookingPaymentCommand init(String bookingReference) {
        this.bookingReference = bookingReference;
        return this;
    }

    @Override
    @Transactional
    public Response execute() {
        if (this.bookingReference == null) {
            throw new IllegalStateException("BookingReference must be set before executing ConfirmBookingPaymentCommand.");
        }

        Booking booking = bookingRepository.findByBookingReference(this.bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + this.bookingReference));

        if (booking.getStatus() != BookingStatus.ACCEPTED_AWAITING_PAYMENT) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be confirmed if it's in ACCEPTED_AWAITING_PAYMENT state. Current status: " + booking.getStatus())
                    .build();
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Payment confirmed for booking reference: {}. Status set to CONFIRMED.", this.bookingReference);

        try {
            eventPublisher.publishEvent(new BookingConfirmedEvent(this, updatedBooking));
            log.info("Published BookingConfirmedEvent for booking ref {}", updatedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingConfirmedEvent for booking ref {}: {}", this.bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking payment confirmed. Booking is now CONFIRMED.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }
}
