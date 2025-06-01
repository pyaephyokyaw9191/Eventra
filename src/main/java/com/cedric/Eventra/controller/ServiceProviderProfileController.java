package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.ServiceProviderProfileDTO; // If used for update
import com.cedric.Eventra.service.ServiceProviderProfileService;
import jakarta.validation.Valid; // For DTO validation
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/service-provider-profiles") // Or just /api/v1/profiles
@RequiredArgsConstructor
public class ServiceProviderProfileController {

    private final ServiceProviderProfileService profileService;

    // Endpoint for the authenticated provider to get their own profile
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> getMyProfile() {
        Response serviceResponse = profileService.getMyServiceProviderProfile();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // Endpoint for the authenticated provider to update their textual profile details
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> updateMyProfile(@Valid @RequestBody ServiceProviderProfileDTO profileDetailsDTO) {
        Response serviceResponse = profileService.updateMyServiceProviderProfile(profileDetailsDTO);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }


    @PostMapping("/me/profile-picture")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> uploadMyProfilePicture(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Profile picture file is empty.").build(), HttpStatus.BAD_REQUEST);
        }
        Response serviceResponse = profileService.uploadProfilePicture(file);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @DeleteMapping("/me/profile-picture")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteMyProfilePicture() {
        Response serviceResponse = profileService.deleteProfilePicture();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @PostMapping("/me/cover-photo")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> uploadMyCoverPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Cover photo file is empty.").build(), HttpStatus.BAD_REQUEST);
        }
        Response serviceResponse = profileService.uploadCoverPhoto(file);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @DeleteMapping("/me/cover-photo")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteMyCoverPhoto() {
        Response serviceResponse = profileService.deleteCoverPhoto();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // Public endpoint to get a provider's profile by their USER ID
    @GetMapping("/{userId}")
    public ResponseEntity<Response> getProviderProfileByUserId(@PathVariable Long userId) {
        // You'll need a method in your ServiceProviderProfileService for this:
        // e.g., Response getProfileByUserId(Long userId);
        // This method would fetch the profile and map to DTO.
        // For now, this is a placeholder.
        // Response serviceResponse = profileService.getProfileByUserId(userId);
        // return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); // Placeholder
    }
}