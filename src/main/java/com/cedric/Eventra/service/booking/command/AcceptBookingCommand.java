package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingAcceptedEvent; // Ensure you have this
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component // If you want Spring to manage it
@RequiredArgsConstructor
@Slf4j
public class AcceptBookingCommand implements BookingCommand {

    // Parameters
    private String bookingReference;

    // Dependencies
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    public AcceptBookingCommand init(String bookingReference) {
        this.bookingReference = bookingReference;
        return this;
    }

    @Override
    public Response execute() {
        if (bookingReference == null) {
            throw new IllegalStateException("BookingReference must be set before executing AcceptBookingCommand.");
        }
        User provider = userService.getCurrentLoggedInUser();
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));

        if (!booking.getOfferedService().getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to accept this booking.")
                    .build();
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be accepted if it's in PENDING state. Current status: " + booking.getStatus())
                    .build();
        }

        booking.setStatus(BookingStatus.ACCEPTED_AWAITING_PAYMENT);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been accepted by provider: {}", bookingReference, provider.getEmail());

        try {
            eventPublisher.publishEvent(new BookingAcceptedEvent(this, updatedBooking));
            log.info("Published BookingAcceptedEvent for booking ref {}", updatedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingAcceptedEvent for booking ref {}: {}", bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request accepted by provider. Now awaiting payment from customer.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }
}
