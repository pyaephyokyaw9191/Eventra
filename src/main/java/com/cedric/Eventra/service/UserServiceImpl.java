package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.*;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.InvalidCredentialException;
import com.cedric.Eventra.exception.NotFoundException;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.ServiceProviderProfileRepository;
import com.cedric.Eventra.repository.UserRepository;
import com.cedric.Eventra.security.JwtUtils;
import com.cedric.Eventra.service.factory.ResponseFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ModelMapper modelMapper;
    private final BookingRepository bookingRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final ResponseFactory responseFactory;

    @Override
    public Response registerUser(RegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return responseFactory.createBadRequestResponse("Email already registered");
        }

        // Set default role if not provided
        // need to implement for ADMIN Role
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        // Create base user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .build();

        // Set isActive based on role
        if (request.getRole() == UserRole.CUSTOMER) {
            user.setIsActive(true); // Customers are active immediately
            log.info("Registering new CUSTOMER {} - setting isActive to true.", user.getEmail());
        } else if (request.getRole() == UserRole.SERVICE_PROVIDER) {
            user.setIsActive(false); // Service Providers start as inactive, pending subscription
            log.info("Registering new SERVICE_PROVIDER {} - setting isActive to false.", user.getEmail());
            // Logic to create ServiceProviderProfile would also go here
        } else if (user.getRole() == UserRole.ADMIN) {
            user.setIsActive(true); // Admins are typically active immediately (or created via a different mechanism)
        } else {
            // Handle unknown role or default to inactive
            user.setIsActive(false);
            log.warn("User {} registered with an unhandled role {} or no role, setting isActive to false.", user.getEmail(), request.getRole());
        }


        // Persist user to generate ID
        userRepository.save(user);

        // Declare profile outside
        ServiceProviderProfile profile = null;

        // If the user is a service provider, create and attach the profile
        if (role == UserRole.SERVICE_PROVIDER) {
            profile = ServiceProviderProfile.builder()
                    .location(request.getLocation())
                    .postcode(request.getPostcode())
                    .profilePictureFilename(request.getProfilePictureUrl())
                    .coverPhotoFilename(request.getCoverPhotoUrl())
                    .user(user)
                    .ABN(request.getAbn())
                    .serviceCategory(request.getServiceCategory())
                    .serviceName(request.getServiceName())
                    .ABN(request.getAbn())
                    .build();

            user.setServiceProviderProfile(profile);
            // Save profile explicitly
            serviceProviderProfileRepository.save(profile);
        }
        return responseFactory.createSuccessResponse("User created successfully");
    }

    @Override
    public Response loginUser(LoginRequest loginRequest) {
        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        // Checking if password matches
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new InvalidCredentialException("Password doesn't match");
        }
        // JWT Token
        String token = jwtUtils.generateToken(user.getEmail());
        // logging
        log.info("User logged in: {}", user.getEmail());
        // Using the specific factory method for login success from ResponseFactory
        return responseFactory.createLoginSuccessResponse(
                "User logged in successfully",
                token,
                user.getRole(),
                user.getIsActive(),
                "6 months" // This could be a configurable value
        );
    }

    @Override
    public User getUserByEmail(String email) {
        log.debug("Attempting to fetch user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    // This exception type should align with what your security setup expects
                    // or what your global exception handler can process gracefully.
                    // If CustomUserDetailsService throws UsernameNotFoundException, this method might also
                    // throw that, or a more generic ResourceNotFoundException if that's your pattern.
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }

    @Override
    public Response getAllUsers() {
        return null;
    }

    @Override
    public Response getOwnAccountDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        log.info("Inside getOwnAccountDetails user email is {}", email);
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return responseFactory.createSuccessUserResponse("Account details retrieved successfully.", userDTO);
    }

    @Override
    public User getCurrentLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User Not Found"));
    }

    @Override
    public Response updateOwnAccount(UserDTO userDTO) {
        User existingUser = getCurrentLoggedInUser();
        // Logging
        log.info("Inside updateOwnAccount for user {}", existingUser.getEmail());

        if(userDTO.getEmail() != null) existingUser.setEmail(userDTO.getEmail());
        if(userDTO.getFirstName() != null) existingUser.setFirstName(userDTO.getFirstName());
        if(userDTO.getLastName() != null) existingUser.setLastName(userDTO.getLastName());
        if(userDTO.getPhoneNumber() != null) existingUser.setPhoneNumber(userDTO.getPhoneNumber());

        if(userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()){
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        userRepository.save(existingUser);

        return Response.builder()
                .status(200)
                .message("User updated successfully")
                .build();
    }

    @Override
    public Response deleteOwnAccount() {
        User user = getCurrentLoggedInUser();
        userRepository.delete(user);

        return Response.builder()
                .status(200)
                .message("User deleted successfully")
                .build();
    }

    @Override
    public Response getMyBookingHistory() {
        User user = getCurrentLoggedInUser();
        List<Booking> bookingList = bookingRepository.findByUserId((user.getId()));
        List<BookingDTO> bookingDTOList = modelMapper.map(bookingList,
                new TypeToken<List<BookingDTO>>(){}.getType());
        // Logging
        log.info("Fetched {} bookings for user {}", bookingDTOList.size(), user.getEmail());
        String message = bookingDTOList.isEmpty() ? "No bookings found" : "Success";
        return Response.builder()
                .status(200)
                .message(message)
                .bookings(bookingDTOList)
                .build();
    }

    @Override
    public Response getServiceProvidersByCategory(ServiceCategory category) {
        if (category == null) {
            return Response.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Service category cannot be null.")
                    .build();
        }

        // Fetch active service provider profiles directly for the given category
        // Assuming User has an 'isActive' field and ServiceProviderProfile is linked to User
        // Adjust query if 'isActive' is on ServiceProviderProfile or if no such combined query exists
        List<ServiceProviderProfile> profiles = serviceProviderProfileRepository.findByServiceCategory(category);
        // If findByServiceCategoryAndUser_IsActiveTrue doesn't exist, you can do:
        // List<ServiceProviderProfile> profiles = serviceProviderProfileRepository.findByServiceCategory(category);
        // And then filter:
        // .stream().filter(p -> p.getUser() != null && p.getUser().getIsActive() && p.getUser().getRole() == UserRole.SERVICE_PROVIDER).collect(Collectors.toList());

        if (profiles.isEmpty()) {
            return Response.builder()
                    .status(HttpStatus.OK.value()) // Or NOT_FOUND if you prefer for empty results
                    .message("No active service providers found for category: " + category.name())
                    .users(new ArrayList<>()) // Return empty list
                    .build();
        }

        // Map the User associated with each profile to UserDTO
        // UserDTO should contain ServiceProviderProfileDTO
        List<UserDTO> providerUserDTOs = profiles.stream()
                .map(ServiceProviderProfile::getUser) // Get the User from the Profile
                .filter(user -> user != null && user.getIsActive() && user.getRole() == UserRole.SERVICE_PROVIDER) // Double check role and active status
                .map(user -> modelMapper.map(user, UserDTO.class)) // Map User entity to UserDTO
                .collect(Collectors.toList());

        return responseFactory.createSuccessUsersResponse(
                "Service providers retrieved successfully for category: " + category.name(),
                providerUserDTOs
        );
    }


    @Override
    public Response getAllActiveServiceProviders() {
        List<User> serviceProviderEntities = userRepository.findByRoleAndIsActiveTrue(UserRole.SERVICE_PROVIDER);

        if (serviceProviderEntities.isEmpty()) {
            return responseFactory.createSuccessUsersResponse("No active service providers found.", Collections.emptyList());
        }

        List<UserDTO> serviceProviderDTOs = serviceProviderEntities.stream()
                .map(user -> modelMapper.map(user, UserDTO.class)) // Your UserDTO includes ServiceProviderProfileDTO
                .collect(Collectors.toList());

        return responseFactory.createSuccessUsersResponse("Active service providers retrieved successfully.", serviceProviderDTOs);
    }

    @Override
    public Response getServiceProviderById(Long providerUserId) {
        User user = userRepository.findById(providerUserId).
                orElseThrow(() -> new NotFoundException("Service provider not found with ID: " + providerUserId));

        if (user == null) {
            return responseFactory.createNotFoundResponse("Service provider", providerUserId);
        }

        if (user.getRole() != UserRole.SERVICE_PROVIDER) {
            return responseFactory.createNotFoundResponse("User with ID " + providerUserId + " is not a service provider.");
        }

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return responseFactory.createSuccessUserResponse("Service provider retrieved successfully.", userDTO);
    }

    // --- Admin Specific Method Implementations ---

    @Override
    public Response getAllCustomersAdmin() {
        List<User> customerEntities = userRepository.findByRole(UserRole.CUSTOMER);
        List<UserDTO> customerDTOs = customerEntities.stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());
        log.info("Admin fetched all customers. Count: {}", customerDTOs.size());
        return responseFactory.createSuccessUsersResponse("All customers retrieved successfully.", customerDTOs);
    }

    @Override
    public Response getAllServiceProvidersAdmin() {
        List<User> serviceProviderEntities = userRepository.findByRole(UserRole.SERVICE_PROVIDER);
        List<UserDTO> serviceProviderDTOs = serviceProviderEntities.stream()
                .map(user -> modelMapper.map(user, UserDTO.class)) // UserDTO includes ServiceProviderProfileDTO
                .collect(Collectors.toList());
        log.info("Admin fetched all service providers. Count: {}", serviceProviderDTOs.size());
        return responseFactory.createSuccessUsersResponse("All service providers retrieved successfully (active and inactive).", serviceProviderDTOs);
    }

    @Override
    @Transactional
    public Response deleteUserByIdAdmin(Long userId) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Prevent admin from deleting themselves through this generic endpoint
        User currentUser = getCurrentLoggedInUser();
        if (currentUser.getId().equals(userId) && currentUser.getRole() == UserRole.ADMIN) {
            log.warn("Admin user {} attempted to delete themselves via admin endpoint.", currentUser.getEmail());
            return responseFactory.createForbiddenResponse("Admin cannot delete their own account using this endpoint.");
        }

        // Cascade delete for service provider profile if it exists
        if (userToDelete.getRole() == UserRole.SERVICE_PROVIDER && userToDelete.getServiceProviderProfile() != null) {
            serviceProviderProfileRepository.delete(userToDelete.getServiceProviderProfile());
        }
        // Add any other cleanup logic here (e.g., related bookings, reviews by this user)

        userRepository.delete(userToDelete);
        log.info("Admin deleted user with ID: {}. User email: {}", userId, userToDelete.getEmail());
        return responseFactory.createSuccessResponse("User with ID " + userId + " deleted successfully by admin.");
    }


    @Override
    @Transactional
    public Response activateUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (user.getIsActive()) {
            return responseFactory.createSuccessUserResponse("User with ID " + userId + " is already active.", modelMapper.map(user, UserDTO.class));
        }

        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        log.info("Admin activated user with ID: {}. User email: {}", userId, user.getEmail());
        return responseFactory.createSuccessUserResponse("User with ID " + userId + " activated successfully by admin.", modelMapper.map(savedUser, UserDTO.class));
    }

    @Override
    @Transactional
    public Response deactivateUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Prevent admin from deactivating themselves
        User currentUser = getCurrentLoggedInUser();
        if (currentUser.getId().equals(userId) && currentUser.getRole() == UserRole.ADMIN) {
            log.warn("Admin user {} attempted to deactivate themselves.", currentUser.getEmail());
            return responseFactory.createForbiddenResponse("Admin cannot deactivate their own account.");
        }

        if (!user.getIsActive()) {
            return responseFactory.createSuccessUserResponse("User with ID " + userId + " is already inactive.", modelMapper.map(user, UserDTO.class));
        }

        user.setIsActive(false);
        User savedUser = userRepository.save(user);
        log.info("Admin deactivated user with ID: {}. User email: {}", userId, user.getEmail());
        return responseFactory.createSuccessUserResponse("User with ID " + userId + " deactivated successfully by admin.", modelMapper.map(savedUser, UserDTO.class));
    }

    @Override
    public Response getUserByIdAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        log.info("Admin retrieved user details for ID: {}", userId);
        return responseFactory.createSuccessUserResponse("User details retrieved successfully.", userDTO);
    }

    // ------------- NEW METHOD IMPLEMENTATION -------------
    @Override
    public Response searchServiceProvidersByName(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return responseFactory.createBadRequestResponse("Service name for search cannot be empty.");
        }

        log.info("Searching for service providers with service name containing: {}", serviceName);

        // Using the new repository method
        List<ServiceProviderProfile> profiles = serviceProviderProfileRepository
                .findByServiceNameContainingIgnoreCaseAndUserIsActiveTrueAndUserRoleServiceProvider(serviceName);

        if (profiles.isEmpty()) {
            return responseFactory.createSuccessUsersResponse(
                    "No active service providers found matching the name: " + serviceName,
                    Collections.emptyList()
            );
        }

        // Map the User associated with each profile to UserDTO
        List<UserDTO> providerUserDTOs = profiles.stream()
                .map(ServiceProviderProfile::getUser) // Get the User from the Profile
                // The repository query already filters by isActive and role, but an additional check here won't harm
                .filter(user -> user != null && user.getIsActive() && user.getRole() == UserRole.SERVICE_PROVIDER)
                .map(user -> modelMapper.map(user, UserDTO.class)) // Map User entity to UserDTO
                .collect(Collectors.toList());

        return responseFactory.createSuccessUsersResponse(
                "Service providers retrieved successfully for service name: " + serviceName,
                providerUserDTOs
        );
    }
}

