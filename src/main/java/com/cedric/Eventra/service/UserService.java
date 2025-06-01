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

    User getUserByEmail(String email);

    // New method for searching by service name
    Response searchServiceProvidersByName(String serviceName);

    // Admin specific methods
    Response getAllCustomersAdmin();
    Response getAllServiceProvidersAdmin();
    Response deleteUserByIdAdmin(Long userId);
    Response activateUserAdmin(Long userId);
    Response deactivateUserAdmin(Long userId);
    Response getUserByIdAdmin(Long userId);
}
