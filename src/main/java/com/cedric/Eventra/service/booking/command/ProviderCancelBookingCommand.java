package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.service.booking.command.BookingCommand;
import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingCancelledByProviderEvent; // Ensure this event class exists
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
public class ProviderCancelBookingCommand implements BookingCommand {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    private String bookingReference;
    private String reason;

    public ProviderCancelBookingCommand init(String bookingReference, String reason) {
        this.bookingReference = bookingReference;
        this.reason = reason; // Can be null
        return this;
    }

    @Override
    @Transactional
    public Response execute() {
        if (this.bookingReference == null) {
            throw new IllegalStateException("BookingReference must be set before executing ProviderCancelBookingCommand.");
        }

        User provider = userService.getCurrentLoggedInUser();
        Booking booking = bookingRepository.findByBookingReference(this.bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + this.bookingReference));

        if (!booking.getOfferedService().getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to cancel this booking.")
                    .build();
        }

        // Original logic from BookingServiceImpl:
        if (booking.getStatus() == BookingStatus.COMPLETED ||
                booking.getStatus() == BookingStatus.REJECTED) {
            // Consider if CONFIRMED should also be non-cancellable or have different rules
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking cannot be cancelled by provider if already COMPLETED or REJECTED. Current status: " + booking.getStatus())
                    .build();
        }
        log.info("Provider {} cancelling booking ref {} with reason: {}", provider.getEmail(), this.bookingReference, this.reason);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        try {
            eventPublisher.publishEvent(new BookingCancelledByProviderEvent(this, updatedBooking, this.reason));
            log.info("Published BookingCancelledByProviderEvent for booking ref {}", updatedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingCancelledByProviderEvent for booking ref {}: {}", this.bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request cancelled by provider.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }
}