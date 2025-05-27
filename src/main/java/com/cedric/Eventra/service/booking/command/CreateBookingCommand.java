package com.cedric.Eventra.service.booking.command;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.events.BookingCreatedEvent; // Ensure you have this event class
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.service.BookingCodeGenerator;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component; // Make command a Spring bean

@Component
@RequiredArgsConstructor // If using constructor injection for all final fields
@Slf4j
public class CreateBookingCommand implements BookingCommand {

    // Parameters for the command
    private BookingDTO bookingDetailsDTO;

    // Dependencies (can be injected if command is a Spring bean, or passed via factory)
    private final UserService userService;
    private final OfferedServiceRepository offeredServiceRepository;
    private final BookingRepository bookingRepository;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    // Setter for parameters or pass via a factory method / constructor if not a prototype bean
    public CreateBookingCommand init(BookingDTO bookingDetailsDTO) {
        this.bookingDetailsDTO = bookingDetailsDTO;
        return this; // fluent interface
    }

    @Override
    public Response execute() {
        if (bookingDetailsDTO == null) {
            throw new IllegalStateException("BookingDetailsDTO must be set before executing CreateBookingCommand.");
        }
        User customer = userService.getCurrentLoggedInUser();

        if (bookingDetailsDTO.getService() == null || bookingDetailsDTO.getService().getId() == null) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Offered service ID is required.")
                    .build();
        }

        OfferedService offeredService = offeredServiceRepository.findById(bookingDetailsDTO.getService().getId())
                .orElseThrow(() -> new NotFoundException("Offered service not found"));

        if (!offeredService.getAvailable()) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Selected service is currently not available.")
                    .build();
        }

        if (offeredService.getProvider().getId().equals(customer.getId())) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Service provider cannot book their own service.")
                    .build();
        }

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setOfferedService(offeredService);
        booking.setRequestName(bookingDetailsDTO.getRequestName());
        booking.setDescription(bookingDetailsDTO.getDescription());
        booking.setLocation(bookingDetailsDTO.getLocation());
        booking.setPreferredDate(bookingDetailsDTO.getPreferredDate());
        booking.setPreferredTime(bookingDetailsDTO.getPreferredTime());
        booking.setPrice(offeredService.getPrice() != null ? offeredService.getPrice() : java.math.BigDecimal.ZERO);
        String newBookingReference = bookingCodeGenerator.generateBookingReference();
        booking.setBookingReference(newBookingReference);
        // Status and createdAt are set by @PrePersist in Booking entity

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with reference: {} by user: {}", savedBooking.getBookingReference(), customer.getEmail());

        try {
            eventPublisher.publishEvent(new BookingCreatedEvent(this, savedBooking));
            log.info("Published BookingCreatedEvent for booking ref {}", savedBooking.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to publish BookingCreatedEvent for booking ref {}: {}", savedBooking.getBookingReference(), e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message("Booking request submitted successfully. Awaiting provider confirmation.")
                .booking(modelMapper.map(savedBooking, BookingDTO.class))
                .build();
    }
}
