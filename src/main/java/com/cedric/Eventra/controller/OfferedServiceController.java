package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.service.OfferedServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class OfferedServiceController {

    private final OfferedServiceService offeredServiceService;

    // Offer a new service
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> createOfferedService(@Valid @RequestBody OfferedServiceDTO offeredServiceDTO){
        // Service method name typo: should be createOfferedService if that's what's in the interface/impl
        // Also, ResponseEntity.ok() always returns 200. Better to use HttpStatus.valueOf(serviceResponse.getStatus())
        Response serviceResponse = offeredServiceService.createOfferedService(offeredServiceDTO); // Assuming service method is createOfferedService
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @PutMapping("/update") // PROBLEM 1: Needs {serviceId} in path. Service expects (Long, DTO)
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> updateService(@Valid @RequestBody OfferedServiceDTO offeredServiceDTO) {
        // This will fail because your service method updateOfferedService(Long serviceId, OfferedServiceDTO dto)
        // needs the serviceId as a separate parameter, typically from the path.
        // You need to get offeredServiceDTO.getId() and pass it as the first argument.
        // And the endpoint should be like PUT /api/services/{serviceId}
        // Response serviceResponse = offeredServiceService.updateOfferedService(offeredServiceDTO); // OLD
        // Corrected call based on common service signature:
        if (offeredServiceDTO.getId() == null) {
            Response errorResponse = Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Service ID must be provided in the request body for updates via this path.")
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        Response serviceResponse = offeredServiceService.updateOfferedService(offeredServiceDTO.getId(), offeredServiceDTO);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // Delete an offered service
    @DeleteMapping("/{serviceId}") // This is good, uses path variable
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteService(@PathVariable Long serviceId) {
        Response serviceResponse = offeredServiceService.deleteOfferedService(serviceId); // This matches service (Long)
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @GetMapping("/{serviceId}") // Good
    public ResponseEntity<Response> getOfferedServiceById(@PathVariable Long serviceId) {
        Response serviceResponse = offeredServiceService.getOfferedServiceById(serviceId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    // Get all services offered by a specific provider
    @GetMapping("/provider/{providerId}") // Good
    public ResponseEntity<Response> getServicesByProvider(@PathVariable Long providerId) {
        Response serviceResponse = offeredServiceService.getOfferedServiceByProviderId(providerId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    @GetMapping("/my-services") // Good
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> getMyOfferedServices() {
        Response serviceResponse = offeredServiceService.getMyOfferedServices();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for a service provider to upload/update an image for their offered service.
     */
    @PostMapping("/{serviceId}/image") // Good
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> uploadServiceImage(@PathVariable Long serviceId,
                                                       @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            Response errorResponse = Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Please select an image file to upload.")
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        Response serviceResponse = offeredServiceService.uploadOfferedServiceImage(serviceId, file);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for a service provider to delete an image for their offered service.
     */
    @DeleteMapping("/{serviceId}/image") // Good
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteServiceImage(@PathVariable Long serviceId) {
        Response serviceResponse = offeredServiceService.deleteOfferedServiceImage(serviceId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}
