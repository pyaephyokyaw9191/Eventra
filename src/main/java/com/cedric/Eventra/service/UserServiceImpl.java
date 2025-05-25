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

    @Override
    public Response registerUser(RegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return Response.builder()
                    .status(400)
                    .message("Email already registered")
                    .build();
        }

        // Set default role if not provided
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        // Create base user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .isActive(false)
                .build();

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
        return Response.builder()
                .status(200)
                .message("User created successfully")
                .build();
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
        // Response
        return Response.builder()
                .status(200)
                .message("User logged in successfully")
                .role(user.getRole())
                .token(token)
                .isActive(user.getIsActive())
                .expirationTime("6 months")
                .build();
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

        return Response.builder()
                .status(200)
                .message("success")
                .user(userDTO)
                .build();
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
                .message("User created successfully")
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

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service providers retrieved successfully for category: " + category.name())
                .users(providerUserDTOs) // Use the 'users' field in your Response DTO
                .build();
    }


    @Override
    public Response getAllActiveServiceProviders() {
        List<User> serviceProviderEntities = userRepository.findByRoleAndIsActiveTrue(UserRole.SERVICE_PROVIDER);

        if (serviceProviderEntities.isEmpty()) {
            return Response.builder()
                    .status(HttpStatus.OK.value())
                    .message("No active service providers found.")
                    .users(Collections.emptyList()) // Use the 'users' field in your Response DTO
                    .build();
        }

        List<UserDTO> serviceProviderDTOs = serviceProviderEntities.stream()
                .map(user -> modelMapper.map(user, UserDTO.class)) // Your UserDTO includes ServiceProviderProfileDTO
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Active service providers retrieved successfully.")
                .users(serviceProviderDTOs) // Use the 'users' field
                .build();
    }


    @Override
    public Response getServiceProviderById(Long providerUserId) {
        User user = userRepository.findById(providerUserId).
                orElseThrow(() -> new NotFoundException("Service provider not found with ID: " + providerUserId));

        if (user == null) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Service provider not found with ID: " + providerUserId)
                    .build();
        }

        if (user.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.NOT_FOUND.value()) // Or BAD_REQUEST if ID exists but wrong type
                    .message("User with ID " + providerUserId + " is not a service provider.")
                    .build();
        }

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Service provider retrieved successfully.")
                .user(userDTO)
                .build();
    }
}
