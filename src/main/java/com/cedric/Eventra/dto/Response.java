package com.cedric.Eventra.dto;

import com.cedric.Eventra.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    // generic
    private int status;
    private String message;

    // for login
    private String token;
    private UserRole role;
    private Boolean isActive;
    private String expirationTime;

    // user data output
    private UserDTO user;
    private List<UserDTO> users;

    // Booking data output
    private BookingDTO booking;
    private List<BookingDTO> bookings;

    // Room data output
    private OfferedServiceDTO service;
    private List<OfferedServiceDTO> services;

    // Payment data output
    private PaymentDTO payment;
    private List<PaymentDTO> payments;

    // Notifications
    private NotificationDTO notification;
    private List<NotificationDTO> notifications;

    // Review data output (NEWLY ADDED)
    private ReviewDTO review;                   // For a single ReviewDTO
    private List<ReviewDTO> reviews;             // For a list of ReviewDTOs

    private final LocalDateTime timestamp = LocalDateTime.now();
}
