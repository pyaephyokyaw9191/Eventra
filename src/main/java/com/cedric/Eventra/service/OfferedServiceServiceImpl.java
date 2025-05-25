package com.cedric.Eventra.service; // Assuming this is the correct package for your Impl

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.UserDTO; // Make sure UserDTO is imported if mapToOfferedServiceDTO uses it
import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.NotFoundException; // Or your ResourceNotFoundException
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.repository.UserRepository;
import com.cedric.Eventra.service.FileStorageService; // Import the FileStorageService
import com.cedric.Eventra.service.OfferedServiceService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferedServiceServiceImpl implements OfferedServiceService {

    private final OfferedServiceRepository offeredServiceRepository;
    private final UserService userService;
    private final UserRepository userRepository; // Keep if used by other methods
    private final ModelMapper modelMapper;
    private final FileStorageService fileStorageService; // Correctly injected

    @Value("${file.upload-dir.service-images}") // Inject the specific subdirectory name
    private String serviceImagesSubDir; // e.g., "service-images"


    @Override
    @Transactional
    public Response createOfferedService(OfferedServiceDTO offeredServiceDTO) { // Renamed from creatOfferedeService
        User provider = userService.getCurrentLoggedInUser();
        if (provider.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Only service providers can create services")
                    .build();
        }

        if (!provider.getIsActive()) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Your provider account is not active. Please complete your subscription payment.")
                    .build();
        }
        // Basic validation
        if (offeredServiceDTO.getName() == null || offeredServiceDTO.getName().isBlank()) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service name is required.").build();
        }
        if (offeredServiceDTO.getDescription() == null || offeredServiceDTO.getDescription().isBlank()) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service description is required.").build();
        }
        if (offeredServiceDTO.getPrice() == null) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service price is required.").build();
        }
        // REMOVED: The strict check for offeredServiceDTO.getAvailable() being null.

        OfferedService serviceToSave = modelMapper.map(offeredServiceDTO, OfferedService.class);
        serviceToSave.setId(null); // Ensure ID is null for creation
        serviceToSave.setProvider(provider);

        // Set availability: Use value from DTO if provided, otherwise default to true.
        if (offeredServiceDTO.getAvailable() != null) {
            serviceToSave.setAvailable(offeredServiceDTO.getAvailable());
        } else {
            serviceToSave.setAvailable(true); // Default to true if not specified in DTO
        }
        // imageFilename will be set upon image upload via a separate endpoint

        OfferedService savedService = offeredServiceRepository.save(serviceToSave);
        log.info("Service created by active provider {}: ID {}", provider.getEmail(), savedService.getId());

        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message("Service created successfully. You can now upload an image for it.")
                .service(mapToOfferedServiceDTO(savedService)) // Use the helper
                .build();
    }

    @Override
    @Transactional
    public Response uploadOfferedServiceImage(Long serviceId, MultipartFile imageFile) {
        User provider = userService.getCurrentLoggedInUser();
        OfferedService service = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Offered Service not found with ID: " + serviceId));

        if (!service.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not authorized to update this service's image.");
        }

        String oldFilename = service.getImageFilename();
        // Using a prefix for service images, e.g., "service_<serviceId>_"
        String filePrefix = "service_" + serviceId + "_";
        String newFilename = fileStorageService.storeFile(imageFile, serviceImagesSubDir, filePrefix, oldFilename);
        service.setImageFilename(newFilename);
        OfferedService updatedService = offeredServiceRepository.save(service);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service image uploaded successfully.")
                .service(mapToOfferedServiceDTO(updatedService))
                .build();
    }

    @Override
    @Transactional
    public Response deleteOfferedServiceImage(Long serviceId) {
        User provider = userService.getCurrentLoggedInUser();
        OfferedService service = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Offered Service not found with ID: " + serviceId));

        if (!service.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this service's image.");
        }

        if (StringUtils.hasText(service.getImageFilename())) {
            fileStorageService.deleteFile(serviceImagesSubDir, service.getImageFilename());
            service.setImageFilename(null);
            offeredServiceRepository.save(service);
        }

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service image deleted successfully.")
                .service(mapToOfferedServiceDTO(service))
                .build();
    }

    @Override
    @Transactional
    public Response deleteOfferedService(Long serviceId) {
        User provider = userService.getCurrentLoggedInUser();

        if (!provider.getIsActive()) { // Ensure provider is active to perform this
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Your provider account is not active.")
                    .build();
        }

        OfferedService offeredService = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Offered Service not found with ID: " + serviceId));


        if (!offeredService.getProvider().getId().equals(provider.getId())) {
            // Prefer throwing exception if GlobalExceptionHandler is set up
            throw new UnauthorizedException("You are not authorized to delete this service.");
        }

        // Delete associated image first
        if (StringUtils.hasText(offeredService.getImageFilename())) {
            try {
                fileStorageService.deleteFile(serviceImagesSubDir, offeredService.getImageFilename());
            } catch (Exception e) {
                log.error("Could not delete image {} for service {}: {}", offeredService.getImageFilename(), serviceId, e.getMessage(), e);
                // Decide if this should halt the service deletion.
                // For now, we log and proceed to delete the service record.
            }
        }

        offeredServiceRepository.delete(offeredService);
        log.info("Service ID {} deleted by provider {}", serviceId, provider.getEmail());
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Offered service deleted successfully.")
                .build();
    }

    @Override
    @Transactional
    public Response updateOfferedService(Long serviceId, OfferedServiceDTO offeredServiceDTO) { // Added serviceId parameter
        User provider = userService.getCurrentLoggedInUser();
        if (!provider.getIsActive()) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Your provider account is not active.")
                    .build();
        }
        OfferedService existingService = offeredServiceRepository.findById(serviceId) // Use serviceId from path
                .orElseThrow(() -> new NotFoundException("Offered Service not found with ID: " + serviceId));

        if (!existingService.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not authorized to update this service.");
        }

        boolean updated = false;
        if (offeredServiceDTO.getName() != null && !offeredServiceDTO.getName().isBlank()) {
            existingService.setName(offeredServiceDTO.getName());
            updated = true;
        }
        if (offeredServiceDTO.getDescription() != null && !offeredServiceDTO.getDescription().isBlank()) {
            existingService.setDescription(offeredServiceDTO.getDescription());
            updated = true;
        }
        if (offeredServiceDTO.getPrice() != null) {
            existingService.setPrice(offeredServiceDTO.getPrice());
            updated = true;
        }
        if (offeredServiceDTO.getAvailable() != null) {
            existingService.setAvailable(offeredServiceDTO.getAvailable());
            updated = true;
        }
        // Location can also be updated
        if (offeredServiceDTO.getLocation() != null) {
            existingService.setLocation(offeredServiceDTO.getLocation());
            updated = true;
        }
        // imageFilename is managed by uploadOfferedServiceImage/deleteOfferedServiceImage

        if (!updated) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("No fields provided for update or data is the same.")
                    .service(mapToOfferedServiceDTO(existingService)) // Return current state
                    .build();
        }

        OfferedService updatedServiceEntity = offeredServiceRepository.save(existingService);
        log.info("Service ID {} updated by provider {}", serviceId, provider.getEmail());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service updated successfully.")
                .service(mapToOfferedServiceDTO(updatedServiceEntity))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getOfferedServiceById(Long serviceId) {
        OfferedService offeredService = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Offered Service not found with ID: " + serviceId));
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Offered service retrieved successfully.")
                .service(mapToOfferedServiceDTO(offeredService))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMyOfferedServices() {
        User currentProvider = userService.getCurrentLoggedInUser();

        if (currentProvider.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("Access denied. User is not a service provider.");
        }
        if (!currentProvider.getIsActive()) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Your provider account is not active. Please complete your subscription.")
                    .services(Collections.emptyList())
                    .build();
        }

        List<OfferedService> services = offeredServiceRepository.findByProvider(currentProvider);
        List<OfferedServiceDTO> serviceDTOs = services.stream()
                .map(this::mapToOfferedServiceDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(serviceDTOs.isEmpty() ? "You have no offered services." : "Successfully retrieved your offered services.")
                .services(serviceDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true) // Added @Transactional
    public Response getOfferedServiceByProviderId(Long serviceProviderId) {
        User provider = userRepository.findById(serviceProviderId)
                .orElseThrow(() -> new NotFoundException("Provider not found with ID: " + serviceProviderId));

        if (provider.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value()) // Or BAD_REQUEST
                    .message("User with ID " + serviceProviderId + " is not a service provider.")
                    .build();
        }
        // For public view, usually we don't gate by provider's active status here unless business rule.

        List<OfferedService> services = offeredServiceRepository.findByProvider(provider);
        List<OfferedServiceDTO> serviceDTOs = services.stream()
                .map(this::mapToOfferedServiceDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Services retrieved successfully for provider ID: " + serviceProviderId)
                .services(serviceDTOs)
                .build();
    }

    // Helper method to map to DTO (ensure it sets imageUrl)
    private OfferedServiceDTO mapToOfferedServiceDTO(OfferedService service) {
        OfferedServiceDTO dto = modelMapper.map(service, OfferedServiceDTO.class);
        if (StringUtils.hasText(service.getImageFilename())) {
            // Pass the URL path segment that corresponds to serviceImagesSubDir
            // This segment should match what's in your MvcConfig's addResourceHandler
            // e.g., if serviceImagesSubDir is "service-images", the path segment is also "service-images"
            dto.setImageUrl(fileStorageService.getFileUrl(serviceImagesSubDir, service.getImageFilename()));
        }
        // Ensure provider DTO is mapped
        if (service.getProvider() != null) {
            // Assuming UserDTO is the correct type for dto.setProvider
            dto.setProvider(modelMapper.map(service.getProvider(), UserDTO.class));
        }
        return dto;
    }
}