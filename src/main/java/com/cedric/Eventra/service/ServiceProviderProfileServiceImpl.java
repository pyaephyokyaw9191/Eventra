package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.ServiceProviderProfileDTO;
import com.cedric.Eventra.dto.UserDTO;
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.ServiceProviderProfileRepository;
import com.cedric.Eventra.service.FileStorageService;
import com.cedric.Eventra.service.ServiceProviderProfileService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderProfileServiceImpl implements ServiceProviderProfileService {

    private final ServiceProviderProfileRepository profileRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ModelMapper modelMapper;

    // Inject subdirectory names from properties to pass to FileStorageService
    @Value("${file.upload-dir.profile-pictures}")
    private String profilePicturesSubDir; // e.g., "profile-pictures"

    @Value("${file.upload-dir.cover-photos}")
    private String coverPhotosSubDir; // e.g., "cover-photos"

    // --- Placeholder for existing methods ---
    @Override
    @Transactional(readOnly = true)
    public Response getMyServiceProviderProfile() {
        User currentUser = userService.getCurrentLoggedInUser();
        if (currentUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("User is not a service provider.");
        }
        ServiceProviderProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found."));
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Profile retrieved successfully.")
                .serviceProviderProfile(mapToDTO(profile)) // Assuming a generic data field or specific DTO field in Response
                .build();
    }

    @Override
    @Transactional
    public Response updateMyServiceProviderProfile(ServiceProviderProfileDTO profileDetailsDTO) {
        User currentUser = userService.getCurrentLoggedInUser();
        if (currentUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("User is not a service provider.");
        }
        ServiceProviderProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found."));

        // Update text fields from DTO
        if(profileDetailsDTO.getLocation() != null) profile.setLocation(profileDetailsDTO.getLocation());
        if(profileDetailsDTO.getPostcode() != null) profile.setPostcode(profileDetailsDTO.getPostcode());
        if(profileDetailsDTO.getServiceCategory() != null) profile.setServiceCategory(profileDetailsDTO.getServiceCategory());
        // Do not update userId, user, reviews, averageRating, or image filenames here

        ServiceProviderProfile updatedProfile = profileRepository.save(profile);
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Profile updated successfully.")
                .serviceProviderProfile(mapToDTO(updatedProfile))
                .build();
    }
    // --- End Placeholder ---


    @Override
    @Transactional
    public Response uploadProfilePicture(MultipartFile imageFile) {
        User providerUser = userService.getCurrentLoggedInUser(); // Ensures user is authenticated
        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("Only service providers can upload profile pictures.");
        }

        ServiceProviderProfile profile = profileRepository.findByUserId(providerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found for user ID: " + providerUser.getId()));

        String oldFilename = profile.getProfilePictureFilename();
        String filePrefix = "user_" + providerUser.getId() + "_profile_";
        String newFilename = fileStorageService.storeFile(imageFile, profilePicturesSubDir, filePrefix, oldFilename);

        profile.setProfilePictureFilename(newFilename);
        ServiceProviderProfile updatedProfile = profileRepository.save(profile);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Profile picture uploaded successfully.")
                .serviceProviderProfile(mapToDTO(updatedProfile)) // Assuming Response.data or add specific field
                .build();
    }

    @Override
    @Transactional
    public Response deleteProfilePicture() {
        User providerUser = userService.getCurrentLoggedInUser();
        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("Only service providers can manage profile pictures.");
        }
        ServiceProviderProfile profile = profileRepository.findByUserId(providerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found."));

        if (StringUtils.hasText(profile.getProfilePictureFilename())) {
            fileStorageService.deleteFile(profilePicturesSubDir, profile.getProfilePictureFilename());
            profile.setProfilePictureFilename(null);
            profileRepository.save(profile);
        }
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Profile picture deleted successfully.")
                .serviceProviderProfile(mapToDTO(profile))
                .build();
    }

    @Override
    @Transactional
    public Response uploadCoverPhoto(MultipartFile imageFile) {
        User providerUser = userService.getCurrentLoggedInUser();
        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("Only service providers can upload cover photos.");
        }
        ServiceProviderProfile profile = profileRepository.findByUserId(providerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found."));

        String oldFilename = profile.getCoverPhotoFilename();
        String filePrefix = "user_" + providerUser.getId() + "_cover_";
        String newFilename = fileStorageService.storeFile(imageFile, coverPhotosSubDir, filePrefix, oldFilename);

        profile.setCoverPhotoFilename(newFilename);
        ServiceProviderProfile updatedProfile = profileRepository.save(profile);

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Cover photo uploaded successfully.")
                .serviceProviderProfile(mapToDTO(updatedProfile))
                .build();
    }

    @Override
    @Transactional
    public Response deleteCoverPhoto() {
        User providerUser = userService.getCurrentLoggedInUser();
        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            throw new UnauthorizedException("Only service providers can manage cover photos.");
        }
        ServiceProviderProfile profile = profileRepository.findByUserId(providerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found."));

        if (StringUtils.hasText(profile.getCoverPhotoFilename())) {
            fileStorageService.deleteFile(coverPhotosSubDir, profile.getCoverPhotoFilename());
            profile.setCoverPhotoFilename(null);
            profileRepository.save(profile);
        }
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Cover photo deleted successfully.")
                .serviceProviderProfile(mapToDTO(profile))
                .build();
    }

    // Helper to map ServiceProviderProfile Entity to DTO
    private ServiceProviderProfileDTO mapToDTO(ServiceProviderProfile profile) {
        ServiceProviderProfileDTO dto = modelMapper.map(profile, ServiceProviderProfileDTO.class);
        // Manually set URLs using FileStorageService
        if (StringUtils.hasText(profile.getProfilePictureFilename())) {
            dto.setProfilePictureUrl(fileStorageService.getFileUrl(profilePicturesSubDir, profile.getProfilePictureFilename()));
        }
        if (StringUtils.hasText(profile.getCoverPhotoFilename())) {
            dto.setCoverPhotoUrl(fileStorageService.getFileUrl(coverPhotosSubDir, profile.getCoverPhotoFilename()));
        }
        // Map user details if not handled by ModelMapper perfectly or if UserDTO needs specific setup
        if (profile.getUser() != null) {
            dto.setUserEmail(profile.getUser().getEmail());
            dto.setUserFirstName(profile.getUser().getFirstName());
            dto.setUserLastName(profile.getUser().getLastName());
        }
        // averageRating and totalReviews are assumed to be on the DTO
        // If averageRating is calculated on-the-fly it would happen here too.
        // If persisted, ModelMapper should get it.
        // For totalReviews, you might need to fetch reviewRepository.countByProvider(profile)
        if (profile.getReviews() != null) {
            dto.setTotalReviews(profile.getReviews().size());
        } else {
            dto.setTotalReviews(0);
        }

        return dto;
    }
}