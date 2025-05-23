package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final BookingCodeGenerator bookingCodeGenerator;

    @Override
    @Transactional
    public Response createBooking(BookingDTO bookingDetailsDTO) {

        // Get the logged in customer
        User customer = userService.getCurrentLoggedInUser();

        if(bookingDetailsDTO.getService() == null || bookingDetailsDTO.getService().getId() == null){
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Offered service ID is required.")
                    .build();
        }

        // Get the offered service
        OfferedService offeredService = offeredServiceRepository.findById(bookingDetailsDTO.getService().getId())
                .orElseThrow(() -> new NotFoundException("Offered service not found"));

        if (!offeredService.getAvailable()) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Selected service is currently not available.")
                    .build();
        }

        // Prevent provider from booking their own service
        if (offeredService.getProvider().getId().equals(customer.getId())) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Service provider cannot book their own service.")
                    .build();
        }

        // Booking
        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setOfferedService(offeredService);
        booking.setRequestName(bookingDetailsDTO.getRequestName());
        booking.setDescription(bookingDetailsDTO.getDescription());
        booking.setLocation(bookingDetailsDTO.getLocation());
        booking.setPreferredDate(bookingDetailsDTO.getPreferredDate());
        booking.setPreferredTime(bookingDetailsDTO.getPreferredTime());

        // Price is taken from the offered service at the time of booking
        booking.setPrice(offeredService.getPrice() != null ? offeredService.getPrice() : BigDecimal.ZERO);

        // Use the injected service to generate the reference
        String newBookingReference = bookingCodeGenerator.generateBookingReference();
        booking.setBookingReference(newBookingReference);

        // Status and createdAt are set by @PrePersist in Booking entity
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with reference: {} by user: {}", savedBooking.getBookingReference(), customer.getEmail());

        // Send notification to provider
        try {
            notificationService.sendNewBookingRequestNotification(savedBooking);
        } catch (Exception e) {
            log.error("Failed to send new booking request notification for booking ref {}: {}", savedBooking.getBookingReference(), e.getMessage());
            // Continue even if notification fails, booking is already made.
        }

        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message("Booking request submitted successfully. Awaiting provider confirmation.")
                .booking(modelMapper.map(savedBooking, BookingDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response providerAcceptBooking(String bookingReference) {

        // Get the logged in provider
        User provider = userService.getCurrentLoggedInUser();

        Booking booking = bookingRepository.findByBookingReference(bookingReference).orElse(null);
        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }

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

        // Update booking status to ACCEPTED
        booking.setStatus(BookingStatus.ACCEPTED_AWAITING_PAYMENT);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been accepted by provider: {}", bookingReference, provider.getEmail());

        try{
            notificationService.sendBookingAcceptedNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking accepted notification for booking ref {}: {}", bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request accepted by provider. Now awaiting payment from customer.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response providerRejectBooking(String bookingReference) {

        User provider = userService.getCurrentLoggedInUser();
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));

        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }

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
        log.info("Booking with reference: {} has been rejected by provider: {}", bookingReference, provider.getEmail());

        try{
            notificationService.sendBookingRejectedNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking rejected notification for booking ref {}: {}", bookingReference, e.getMessage());
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request rejected by provider.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response customerCancelBooking(String bookingReference) {
        User customer = userService.getCurrentLoggedInUser();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));

        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }

        if (!booking.getUser().getId().equals(customer.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to cancel this booking.")
                    .build();
        }

        // Business rule: Customer can cancel if PENDING or ACCEPTED_AWAITING_PAYMENT
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACCEPTED_AWAITING_PAYMENT) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be cancelled if it's in PENDING or ACCEPTED_AWAITING_PAYMENT state. Current status: " + booking.getStatus())
                    .build();
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been cancelled by customer: {}", bookingReference, customer.getEmail());

        // Need to implement the logic to send notification to provider (if required)
        /*
        try{
            notificationService.sendBookingCancelledNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking cancelled notification for booking ref {}: {}", bookingReference, e.getMessage());
        }
         */

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request cancelled by customer.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response providerCancelBooking(String bookingReference, String reason) {

        User provider = userService.getCurrentLoggedInUser();

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));

        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }

        if (!booking.getOfferedService().getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to cancel this booking.")
                    .build();
        }

        // Business rule: Provider can cancel if ACCEPTED_AWAITING_PAYMENT or BOOKED or Even Confimed
        if(booking.getStatus() == BookingStatus.COMPLETED ||
                booking.getStatus() == BookingStatus.REJECTED ||
                booking.getStatus() == BookingStatus.CONFIRMED){
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be cancelled if it's in ACCEPTED_AWAITING_PAYMENT or BOOKED state. Current status: " + booking.getStatus())
                    .build();
        }
        log.info("Provider {} cancelling booking ref {} with reason: {}", provider.getEmail(), bookingReference, reason);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);

        // Send notification to customer (Optional)
        /*
        try{
            notificationService.sendBookingCancelledNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking cancelled notification for booking ref {}: {}", bookingReference, e.getMessage());
        }
        */

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request cancelled by provider.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

    @Override
    public Response confirmBookingPayment(String bookingReference) {

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));
        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
                    .build();
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED_AWAITING_PAYMENT) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Booking can only be confirmed if it's in ACCEPTED_AWAITING_PAYMENT state. Current status: " + booking.getStatus())
                    .build();
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking with reference: {} has been confirmed by provider: {}", bookingReference, booking.getOfferedService().getProvider().getEmail());
        try{
            notificationService.sendBookingConfirmedNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking confirmed notification for booking ref {}: {}", bookingReference, e.getMessage());
        }
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request confirmed by provider. Booking is now in now CONFIRMED.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response markBookingAsCompleted(String bookingReference) {

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new NotFoundException("Booking not found with reference: " + bookingReference));
        if (booking == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Booking not found with reference: " + bookingReference)
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
        log.info("Booking with reference: {} has been marked as completed by provider: {}", bookingReference, booking.getOfferedService().getProvider().getEmail());
        // Send notification to customer
        /*
        try{
            notificationService.sendBookingCompletedNotification(updatedBooking);
        } catch (Exception e) {
            log.error("Failed to send booking completed notification for booking ref {}: {}", bookingReference, e.getMessage());
        }
         */
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Booking request marked as completed.")
                .booking(modelMapper.map(updatedBooking, BookingDTO.class))
                .build();
    }

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
