package com.cedric.Eventra.controller;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Endpoint for an authenticated user to retrieve all of their notifications.
     *
     * @return ResponseEntity containing the standard Response object with their list of notifications.
     */
    @GetMapping("/my-notifications")
    @PreAuthorize("isAuthenticated()") // Ensures user is logged in
    public ResponseEntity<Response> getMyNotifications() {
        Response serviceResponse = notificationService.getMyNotifications();
        // If serviceResponse.getNotifications() is null due to Response DTO not having the field,
        // this will still work but the list won't be in the JSON.
        // Ensure Response DTO has the 'notifications' field for the list to be included.
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user to delete one of their notifications.
     *
     * @param notificationId The ID of the notification to delete.
     * @return ResponseEntity containing the standard Response object.
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()") // Ensures user is logged in
    public ResponseEntity<Response> deleteMyNotification(@PathVariable Long notificationId) {
        Response serviceResponse = notificationService.deleteMyNotification(notificationId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}