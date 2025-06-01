package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.service.booking.command.BookingCommand;
import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingRejectedEvent; // Ensure this event class exists
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Important for commands modifying data

@Component
@RequiredArgsConstructor
@Slf4j
public class RejectBookingCommand implements BookingCommand {

    // Dependencies
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    // Parameters
    private String bookingReference;

    public RejectBookingCommand init(String bookingReference) {
        this.bookingReference = bookingReference;
        return this;
    }

    @Override
    @Transactional
    public Response execute() {
        if (this.bookingReference == null) {
            throw new IllegalStateException("BookingReference must be set before executing RejectBookingCommand.");
        }

        User provider = userService.getCurrentLoggedInUser();
        Booking booking = bookingRepository.findByBookingReference(this.bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + this.bookingReference));

        if (!booking.getOfferedService().getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to reject this booking.")
                    .build();
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be rejected if it's in PENDING state. Current status: " + booking.getStatus())
                    .build();
        }

        booking.setStatus(BookingStatus.REJECTED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been rejected by provider: {}", this.bookingReference, provider.getEmail());

        try {
            eventPublisher.publishEvent(new BookingRejectedEvent(this, updatedBooking));
            log.info("Published BookingRejectedEvent for booking ref {}", updatedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingRejectedEvent for booking ref {}: {}", this.bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request rejected by provider.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }
}