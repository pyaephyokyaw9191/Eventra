package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.UserDTO;
import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> getOwnAccountDetails(){
        return ResponseEntity.ok(userService.getOwnAccountDetails());
    }

    @PutMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> updateOwnAccount(@RequestBody @Valid UserDTO userDTO){
        return ResponseEntity.ok(userService.updateOwnAccount(userDTO));
    }

    @DeleteMapping("/account")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteOwnAccount(){
        return ResponseEntity.ok(userService.deleteOwnAccount());
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response> getMyBookingHistory(){
        return ResponseEntity.ok(userService.getMyBookingHistory());
    }

    @GetMapping("/service-providers/category/{categoryName}")
    public ResponseEntity<Response> getServiceProvidersByCategory(@PathVariable String categoryName) {
        try {
            ServiceCategory category = ServiceCategory.valueOf(categoryName.toUpperCase());
            Response serviceResponse = userService.getServiceProvidersByCategory(category); // Call the new service method
            return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
        } catch (IllegalArgumentException e) {
            Response errorResponse = Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid service category: " + categoryName)
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/service-providers")
    public ResponseEntity<Response> getAllActiveServiceProviders() {
        Response serviceResponse = userService.getAllActiveServiceProviders();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint to retrieve a specific service provider by their User ID.
     * Publicly accessible.
     *
     * @param providerUserId The User ID of the service provider.
     * @return ResponseEntity containing the standard Response object with the service provider's details.
     */
    @GetMapping("/service-providers/{providerUserId}")
    // No @PreAuthorize needed if it's a public endpoint
    public ResponseEntity<Response> getServiceProviderById(@PathVariable Long providerUserId) {
        Response serviceResponse = userService.getServiceProviderById(providerUserId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // ------------- NEW ENDPOINT -------------
    /**
     * Endpoint to search for active service providers by service name.
     * Publicly accessible.
     *
     * @param name The service name query to search for.
     * @return ResponseEntity containing the standard Response object with the list of matching service providers.
     */
    @GetMapping("/service-providers/search")
    public ResponseEntity<Response> searchServiceProvidersByName(@RequestParam("name") String name) {
        Response serviceResponse = userService.searchServiceProvidersByName(name);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // --- Admin Specific Endpoints ---

    @GetMapping("/admin/customers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllCustomersAdmin() {
        return ResponseEntity.ok(userService.getAllCustomersAdmin());
    }

    @GetMapping("/admin/service-providers/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllServiceProvidersAdmin() {
        // This fetches all service providers, regardless of their isActive status
        return ResponseEntity.ok(userService.getAllServiceProvidersAdmin());
    }

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getUserByIdAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserByIdAdmin(userId));
    }

    @DeleteMapping("/admin/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> deleteUserByIdAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.deleteUserByIdAdmin(userId));
    }

    @PutMapping("/admin/user/{userId}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> activateUserAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.activateUserAdmin(userId));
    }

    @PutMapping("/admin/user/{userId}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> deactivateUserAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.deactivateUserAdmin(userId));
    }
}

