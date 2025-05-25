package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.LoginRequest;
import com.cedric.Eventra.dto.RegistrationRequest;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.UserDTO;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;

public interface UserService {

    Response registerUser(RegistrationRequest request);

    Response loginUser(LoginRequest loginRequest);

    Response getAllUsers();

    Response getOwnAccountDetails();

    User getCurrentLoggedInUser();

    Response updateOwnAccount(UserDTO userDTO);

    Response deleteOwnAccount();

    Response getMyBookingHistory();

    Response getServiceProvidersByCategory(ServiceCategory category);

    Response getAllActiveServiceProviders();

    Response getServiceProviderById(Long providerUserId);

    /**
     * Retrieves a user by their email.
     * Primarily for internal use, e.g., by security services or other services needing to fetch a User entity.
     *
     * @param email The email of the user to retrieve.
     * @return The User entity.
     * @throws com.cedric.Eventra.exception.ResourceNotFoundException if user with the email is not found.
     *                                                                (Or your CustomUserDetailsService might throw UsernameNotFoundException directly)
     */
    User getUserByEmail(String email);
}
