package com.cedric.Eventra.dto;

import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private UserRole role; // CUSTOMER or SERVICE_PROVIDER

    // --- Optional fields for SERVICE_PROVIDER only ---
    private String serviceName;
    private String serviceDescription;
    private String ABN;
    private ServiceCategory serviceCategory;
    private BigDecimal serviceRate;
    private String location;
    private String postcode;
    private String profilePictureUrl;
    private String coverPhotoUrl;
}
