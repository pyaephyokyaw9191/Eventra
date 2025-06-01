package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.service.booking.command.BookingCommand;
import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingCompletedEvent; // Ensure this event class exists
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
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarkBookingAsCompletedCommand implements BookingCommand {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    private String bookingReference;

    public MarkBookingAsCompletedCommand init(String bookingReference) {
        this.bookingReference = bookingReference;
        return this;
    }

    @Override
    @Transactional
    public Response execute() {
        if (this.bookingReference == null) {
            throw new IllegalStateException("BookingReference must be set before executing MarkBookingAsCompletedCommand.");
        }

        User provider = userService.getCurrentLoggedInUser();
        Booking booking = bookingRepository.findByBookingReference(this.bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + this.bookingReference));

        if (!booking.getOfferedService().getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to mark this booking as completed.")
                    .build();
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be marked as completed if it's in CONFIRMED state. Current status: " + booking.getStatus())
                    .build();
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been marked as completed by provider: {}", this.bookingReference, provider.getEmail());

        try {
            eventPublisher.publishEvent(new BookingCompletedEvent(this, updatedBooking));
            log.info("Published BookingCompletedEvent for booking ref {}", updatedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingCompletedEvent for booking ref {}: {}", this.bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request marked as completed.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }
}