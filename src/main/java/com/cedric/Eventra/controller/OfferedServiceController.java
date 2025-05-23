package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.service.OfferedServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class OfferedServiceController {

    private final OfferedServiceService offeredServiceService;

    // Offer a new service
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> createOfferedService(@Valid @RequestBody OfferedServiceDTO offeredServiceDTO){
        return ResponseEntity.ok(offeredServiceService.creatOfferedService(offeredServiceDTO));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> updateService(@Valid @RequestBody OfferedServiceDTO offeredServiceDTO) {
        return ResponseEntity.ok(offeredServiceService.updateOfferedService(offeredServiceDTO));
    }

    // Delete an offered service
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    public ResponseEntity<Response> deleteService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(offeredServiceService.deleteOfferedService(serviceId));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<Response> getOfferedServiceById(@PathVariable Long serviceId) {
        return ResponseEntity.ok(offeredServiceService.getOfferedServiceById(serviceId));
    }

    // Get all services offered by a specific provider
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<Response> getServicesByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(offeredServiceService.getOfferedServiceByProviderId(providerId));
    }

    @GetMapping("/my-services")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')") // Ensures only service providers can access
    public ResponseEntity<Response> getMyOfferedServices() {
        return ResponseEntity.ok(offeredServiceService.getMyOfferedServices());
    }
}
