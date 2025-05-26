package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.events.*;
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService{

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final OfferedServiceRepository offeredServiceRepository;
    //private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final ApplicationEventPublisher eventPublisher; // Added


    @Override
    public Response getMyBookingAsCustomer() {
        User customer = userService.getCurrentLoggedInUser();

        List<Booking> bookings = bookingRepository.findByUserId(customer.getId());
        bookings.sort(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<BookingDTO> bookingDTOs = bookings.stream()
                .map(booking -> modelMapper.map(booking, BookingDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(bookingDTOs.isEmpty() ? "You have no bookings." : "Your bookings retrieved successfully.")
                .bookings(bookingDTOs.isEmpty() ? Collections.emptyList() : bookingDTOs) // Use 'bookings' field
                .build();
    }

    @Override
    public Response getMyBookingAsProvider() {
        User provider = userService.getCurrentLoggedInUser();

        if (provider.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("User is not a service provider.").build();
        }

        List<Booking> bookings = bookingRepository.findByOfferedService_Provider_Id(provider.getId());
        bookings.sort(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<BookingDTO> bookingDTOs = bookings.stream()
                .map(booking -> modelMapper.map(booking, BookingDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(bookingDTOs.isEmpty() ? "You have no bookings for your services." : "Provider bookings retrieved successfully.")
                .bookings(bookingDTOs.isEmpty() ? Collections.emptyList() : bookingDTOs) // Use 'bookings' field
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getBookingByReference(String bookingReference) {
        User currentUser = userService.getCurrentLoggedInUser();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));

        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }

        // Authorization: User must be the customer OR the provider of the service for this booking
        boolean isCustomer = booking.getUser().getId().equals(currentUser.getId());
        boolean isProviderForThisBooking = false;
        if (currentUser.getRole() == UserRole.SERVICE_PROVIDER) { // Only check if current user is a provider
            isProviderForThisBooking = booking.getOfferedService().getProvider().getId().equals(currentUser.getId());
        }

        if (!isCustomer && !isProviderForThisBooking) {
            log.warn("User {} attempted to access booking ref {} without authorization.", currentUser.getEmail(), bookingReference);
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to view this booking.")
                    .build();
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking retrieved successfully.")
                .booking(modelMapper.map(booking, BookingDTO.class)) // Ensure 'booking' field is in Response DTO
                .build();
    }
}
