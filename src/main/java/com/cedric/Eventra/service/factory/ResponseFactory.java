package com.cedric.Eventra.service.factory;

import com.cedric.Eventra.dto.*; // Assuming all your DTOs are here
import com.cedric.Eventra.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Collections;

@Component
public class ResponseFactory {

    // --- Generic Success Responses ---
    public Response createSuccessResponse(String message) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .build();
    }

    // --- Specific Success Responses (more type-safe and aligned with your Response DTO) ---
    public Response createSuccessUserResponse(String message, UserDTO user) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .user(user)
                .build();
    }

    public Response createSuccessUsersResponse(String message, List<UserDTO> users) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .users(users == null ? Collections.emptyList() : users)
                .build();
    }

    public Response createSuccessBookingResponse(String message, BookingDTO booking) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .booking(booking)
                .build();
    }

    public Response createSuccessBookingsResponse(String message, List<BookingDTO> bookings) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .bookings(bookings == null ? Collections.emptyList() : bookings)
                .build();
    }
    // Add more specific success methods for other DTO types like ReviewDTO, PaymentDTO etc. as needed

    public Response createLoginSuccessResponse(String message, String token, UserRole role, Boolean isActive, String expirationTime) {
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .token(token)
                .role(role)
                .isActive(isActive)
                .expirationTime(expirationTime)
                .build();
    }


    // --- Generic Created Response ---
    public Response createCreatedResponse(String message) {
        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .build();
    }

    // Example for a created response that includes the created entity
    public Response createCreatedResponseWithUser(String message, UserDTO user) {
        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .user(user) // Assuming you might want to return the created user
                .build();
    }
    public Response createCreatedResponseWithBooking(String message, BookingDTO booking) {
        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .booking(booking)
                .build();
    }


    // --- Error Responses ---
    public Response createErrorResponse(HttpStatus status, String message) {
        return Response.builder()
                .status(status.value())
                .message(message)
                .build();
    }

    public Response createBadRequestResponse(String message) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    public Response createNotFoundResponse(String message) {
        return createErrorResponse(HttpStatus.NOT_FOUND, message);
    }

    public Response createNotFoundResponse(String resourceName, Object identifier) {
        return createErrorResponse(HttpStatus.NOT_FOUND, resourceName + " not found with identifier: " + identifier);
    }

    public Response createUnauthorizedResponse(String message) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, message);
    }

    public Response createForbiddenResponse(String message) {
        return createErrorResponse(HttpStatus.FORBIDDEN, message);
    }
}