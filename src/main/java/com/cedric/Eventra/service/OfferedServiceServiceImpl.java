package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.OfferedServiceDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferedServiceServiceImpl implements OfferedServiceService{

    private final OfferedServiceRepository offeredServiceRepository;
    private final UserService userService; // for current loggedIn user
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public Response creatOfferedService(OfferedServiceDTO offeredServiceDTO) {
        User provider = userService.getCurrentLoggedInUser();
        if(provider.getRole() != UserRole.SERVICE_PROVIDER){
            return Response.builder()
                    .status(403)
                    .message("Only service providers can create services")
                    .build();
        }

        // Validate essential fields from DTO before mapping
        if (offeredServiceDTO.getName() == null || offeredServiceDTO.getName().isBlank()) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service name is required.").build();
        }
        if (offeredServiceDTO.getDescription() == null || offeredServiceDTO.getDescription().isBlank()) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service description is required.").build();
        }
        if (offeredServiceDTO.getPrice() == null) {
            return Response.builder().status(HttpStatus.BAD_REQUEST.value()).message("Service price is required.").build();
        }

        OfferedService serviceToSave = modelMapper.map(offeredServiceDTO, OfferedService.class);
        // set the provider of service with current logged in provider
        serviceToSave.setProvider(provider);
        serviceToSave.setAvailable(true);

        offeredServiceRepository.save(serviceToSave);
        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message("Service created successfully")
                .service(modelMapper.map(serviceToSave, OfferedServiceDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response deleteOfferedService(Long serviceId) {
        User provider = userService.getCurrentLoggedInUser();

        OfferedService offeredService = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));


        if (!offeredService.getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to delete this service.")
                    .build();
        }

        offeredServiceRepository.delete(offeredService);
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Offered service deleted successfully.")
                .build();
    }

    @Override
    @Transactional
    public Response updateOfferedService(OfferedServiceDTO offeredServiceDTO) {

        User provider = userService.getCurrentLoggedInUser();
        OfferedService existingService = offeredServiceRepository.findById(offeredServiceDTO.getId())
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (!existingService.getProvider().getId().equals(provider.getId())) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to update this service.")
                    .build();
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

        if (!updated) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("No fields provided for update.")
                    .service(modelMapper.map(existingService, OfferedServiceDTO.class)) // Return current state
                    .build();
        }

        OfferedService updatedService = offeredServiceRepository.save(existingService);
        OfferedServiceDTO responseDto = modelMapper.map(updatedService, OfferedServiceDTO.class);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service updated successfully.")
                .service(responseDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read operations
    public Response getOfferedServiceById(Long serviceId) {
        OfferedService offeredService = offeredServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("OfferedService not found"));


        OfferedServiceDTO serviceDTO = modelMapper.map(offeredService, OfferedServiceDTO.class);
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Offered service retrieved successfully.")
                .service(serviceDTO) // Using the 'service' field in your Response DTO
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMyOfferedServices() {
        User currentProvider = userService.getCurrentLoggedInUser(); // Use your existing UserService method

        if (currentProvider.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Access denied. User is not a service provider.")
                    .build();
        }

        List<OfferedService> services = offeredServiceRepository.findByProvider(currentProvider);
        List<OfferedServiceDTO> serviceDTOs = services.stream()
                .map(service -> modelMapper.map(service, OfferedServiceDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Successfully retrieved your offered services.")
                .services(serviceDTOs) // Using the 'services' field in your Response DTO
                .build();
    }

    @Override
    public Response getOfferedServiceByProviderId(Long serviceProviderId) {
        User provider = userRepository.findById(serviceProviderId)
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        // check if the role is SERVICE_PROVIDER
         if (provider.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("User with ID " + serviceProviderId + " is not a service provider.")
                    .build();
         }

        List<OfferedService> services = offeredServiceRepository.findByProvider(provider);
        List<OfferedServiceDTO> serviceDTOs = services.stream()
                .map(service -> modelMapper.map(service, OfferedServiceDTO.class))
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Services retrieved successfully for provider ID: " + serviceProviderId)
                .services(serviceDTOs)
                .build();
    }
}
