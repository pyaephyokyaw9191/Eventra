package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.ServiceProviderProfileDTO; // Assuming you have this
import org.springframework.web.multipart.MultipartFile;

public interface ServiceProviderProfileService {

    // ... other methods for managing profile text details (create, update, get) ...
    Response getMyServiceProviderProfile(); // Example: gets profile of logged-in provider
    Response updateMyServiceProviderProfile(ServiceProviderProfileDTO profileDetailsDTO); // Example

    Response uploadProfilePicture(MultipartFile imageFile);
    Response deleteProfilePicture();

    Response uploadCoverPhoto(MultipartFile imageFile);
    Response deleteCoverPhoto();
}