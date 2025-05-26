package com.cedric.Eventra.controller;


import com.cedric.Eventra.dto.BookingDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.service.BookingService;
import com.cedric.Eventra.service.booking.command.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ApplicationContext applicationContext;

    /**
     * Endpoint for a customer to create a new booking request.
     * The authenticated user is assumed to be the customer.
     *
     * @param bookingDetailsDTO DTO containing booking request details.
     * Crucially, include 'service.id' to identify the OfferedService.
     * @return ResponseEntity containing the standard Response object.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")// Any authenticated user can create a booking
    public ResponseEntity<Response> createBooking(@Valid @RequestBody BookingDTO bookingDetailsDTO) {
        CreateBookingCommand command = applicationContext.getBean(CreateBookingCommand.class).init(bookingDetailsDTO);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated service provider to accept a pending booking request.
     *
     * @param bookingReference The unique reference of the booking to accept.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/provider-accept")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> providerAcceptBooking(@PathVariable String bookingReference) {
        AcceptBookingCommand command = applicationContext.getBean(AcceptBookingCommand.class).init(bookingReference);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated service provider to reject a pending booking request.
     *
     * @param bookingReference The unique reference of the booking to reject.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/provider-reject")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> providerRejectBooking(@PathVariable String bookingReference) {
        RejectBookingCommand command = applicationContext.getBean(RejectBookingCommand.class).init(bookingReference);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated customer to cancel their own booking.
     *
     * @param bookingReference The unique reference of the booking to cancel.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/customer-cancel")
    @PreAuthorize("isAuthenticated()") // Customer must be authenticated
    public ResponseEntity<Response> customerCancelBooking(@PathVariable String bookingReference) {
        CustomerCancelBookingCommand command = applicationContext.getBean(CustomerCancelBookingCommand.class).init(bookingReference);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated service provider to cancel a booking.
     *
     * @param bookingReference The unique reference of the booking to cancel.
     * @param reason           Optional reason for cancellation.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/provider-cancel")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> providerCancelBooking(@PathVariable String bookingReference,
                                                          @RequestParam(required = false) String reason) {
        ProviderCancelBookingCommand command = applicationContext.getBean(ProviderCancelBookingCommand.class).init(bookingReference, reason);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint to confirm payment for a booking.
     * This might be called by an admin or a payment callback system in a real scenario.
     * For now, let's protect it for authenticated users, assuming an admin might do this manually.
     *
     * @param bookingReference The unique reference of the booking for which payment is confirmed.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/confirm-payment")
    // @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SYSTEM')") // Example: Or just isAuthenticated() if a user action after payment
    public ResponseEntity<Response> confirmBookingPayment(@PathVariable String bookingReference) {
        ConfirmBookingPaymentCommand command = applicationContext.getBean(ConfirmBookingPaymentCommand.class).init(bookingReference);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated service provider to mark a booking as completed.
     *
     * @param bookingReference The unique reference of the booking to mark as completed.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{bookingReference}/complete")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> markBookingAsCompleted(@PathVariable String bookingReference) {
        MarkBookingAsCompletedCommand command = applicationContext.getBean(MarkBookingAsCompletedCommand.class).init(bookingReference);
        Response serviceResponse = command.execute();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated customer to retrieve all of their bookings.
     *
     * @return ResponseEntity containing the standard Response object with their list of bookings.
     */
    @GetMapping("/my-bookings/customer")
    @PreAuthorize("isAuthenticated()") // Assuming any logged-in user might be a customer
    public ResponseEntity<Response> getMyBookingsAsCustomer() {
        Response serviceResponse = bookingService.getMyBookingAsCustomer();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated service provider to retrieve all bookings related to their services.
     *
     * @return ResponseEntity containing the standard Response object with relevant bookings.
     */
    @GetMapping("/my-bookings/provider")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> getMyBookingsAsProvider() {
        Response serviceResponse = bookingService.getMyBookingAsProvider();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user (customer or relevant provider) to retrieve a specific booking by its reference.
     *
     * @param bookingReference The unique reference of the booking.
     * @return ResponseEntity containing the standard Response object with the booking details.
     */
    @GetMapping("/{bookingReference}")
    @PreAuthorize("isAuthenticated()") // Service layer will check if user is customer or related provider
    public ResponseEntity<Response> getBookingByReference(@PathVariable String bookingReference) {
        Response serviceResponse = bookingService.getBookingByReference(bookingReference);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}
