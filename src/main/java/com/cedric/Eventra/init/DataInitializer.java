//package com.cedric.Eventra.init;
//
//import com.cedric.Eventra.entity.OfferedService;
//import com.cedric.Eventra.entity.ServiceProviderProfile;
//import com.cedric.Eventra.entity.User;
//import com.cedric.Eventra.enums.ServiceCategory;
//import com.cedric.Eventra.enums.UserRole;
//import com.cedric.Eventra.repository.OfferedServiceRepository;
//import com.cedric.Eventra.repository.ServiceProviderProfileRepository;
//import com.cedric.Eventra.repository.UserRepository;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.FileCopyUtils;
//import org.springframework.beans.factory.annotation.Value;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Random;
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    // ServiceProviderProfileRepository is not strictly needed here if User cascades saves to it
//    // private final ServiceProviderProfileRepository serviceProviderProfileRepository;
//    private final OfferedServiceRepository offeredServiceRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    // Inject the target directory paths from application.properties
//    @Value("${file.upload-dir.profile-pictures}")
//    private String profilePicturesTargetDir;
//
//    @Value("${file.upload-dir.cover-photos}")
//    private String coverPhotosTargetDir;
//
//    @Value("${file.upload-dir.service-images}")
//    private String serviceImagesTargetDir;
//
//    // Relative path within resources to your source images
//    private final String SOURCE_IMAGE_BASE_PATH = "Initialize-images/"; // Assuming this is directly under src/main/resources
//    private final String DEFAULT_PROFILE_PIC_NAME = "profile-picture.png";
//    private final String DEFAULT_COVER_PHOTO_NAME = "cover-photo.jpg";
//    private final String DEFAULT_SERVICE_IMAGE_NAME = "service-image.jpg";
//
//    @Override
//    @Transactional
//    public void run(String... args) throws Exception {
//        log.info("Starting data initialization...");
//        if (userRepository.count() == 0) {
//            log.info("No existing users found, proceeding with data initialization.");
//            createServiceProviders();
//            createCustomerUsers();
//        } else {
//            log.info("Data already appears to exist. Skipping initialization.");
//        }
//        log.info("Data initialization finished.");
//    }
//
//    private String copyImageToUploads(String sourceSubDir, String sourceFilename, String targetSubDir, String targetFilePrefix) throws IOException {
//        Resource resource = new ClassPathResource(SOURCE_IMAGE_BASE_PATH + sourceSubDir + "/" + sourceFilename);
//        if (!resource.exists()) {
//            log.warn("Source image not found: {}/{}", sourceSubDir, sourceFilename);
//            return null;
//        }
//
//        try (InputStream inputStream = resource.getInputStream()) {
//            String extension = "";
//            int i = sourceFilename.lastIndexOf('.');
//            if (i > 0) {
//                extension = sourceFilename.substring(i);
//            }
//            String uniqueTargetFilename = targetFilePrefix + UUID.randomUUID().toString() + extension;
//
//            // --- START FIX ---
//            // Ensure the target directory exists
//            Path targetDirAsPath = Paths.get(targetSubDir);
//            if (!Files.exists(targetDirAsPath)) {
//                try {
//                    Files.createDirectories(targetDirAsPath); // This creates parent directories if needed
//                    log.info("Created target directory: {}", targetDirAsPath.toString());
//                } catch (IOException e) {
//                    log.error("Could not create target directory: {}", targetDirAsPath.toString(), e);
//                    throw e; // Re-throw the exception to halt initialization if directory creation fails
//                }
//            }
//            // --- END FIX ---
//
//            Path targetPath = targetDirAsPath.resolve(uniqueTargetFilename); // Use targetDirAsPath here
//            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
//            log.info("Copied initial image {} to {}", sourceFilename, targetPath.toString());
//            return uniqueTargetFilename; // Return the new unique filename
//        }}
//
//
//    private void createServiceProviders() throws IOException { // Add throws IOException
//        List<ServiceCategory> categories = Arrays.asList(ServiceCategory.values());
//        String[] businessNamePrefixes = {"Elite", "Premium", "Pro", "Affordable", "Creative", "Dynamic", "Local"};
//        String[] businessNameSuffixes = {"Services", "Solutions", "Studio", "Co.", "Group", "Experts"};
//        Random random = new Random();
//
//        for (int i = 1; i <= 20; i++) {
//            User providerUser = User.builder()
//                    .firstName("Provider" + i)
//                    .lastName("User" + i)
//                    .email("provider" + i + "@example.com")
//                    .password(passwordEncoder.encode("password123"))
//                    .phoneNumber("04123456" + String.format("%02d", i))
//                    .role(UserRole.SERVICE_PROVIDER)
//                    .isActive(true)
//                    .build();
//
//            ServiceCategory assignedCategory = categories.get(random.nextInt(categories.size()));
//            String providerBusinessName = businessNamePrefixes[random.nextInt(businessNamePrefixes.length)] + " " +
//                    assignedCategory.name().replace("_", " ") + " " +
//                    businessNameSuffixes[random.nextInt(businessNameSuffixes.length)] + " #" + i;
//            String dummyAbn = String.format("%011d", random.nextLong(100000000000L));
//
//            // Copy images and get their new filenames
//            String profilePicFilename = copyImageToUploads("profile-pictures", DEFAULT_PROFILE_PIC_NAME, profilePicturesTargetDir, "user_" + i + "_profile_");
//            String coverPhotoFilename = copyImageToUploads("cover-photos", DEFAULT_COVER_PHOTO_NAME, coverPhotosTargetDir, "user_" + i + "_cover_");
//
//            ServiceProviderProfile profile = ServiceProviderProfile.builder()
//                    .serviceName(providerBusinessName)
//                    .serviceCategory(assignedCategory)
//                    .ABN(dummyAbn)
//                    .location("Suburb " + i + ", NSW")
//                    .postcode(String.valueOf(2000 + i * 5))
//                    .profilePictureFilename(profilePicFilename) // Set the filename
//                    .coverPhotoFilename(coverPhotoFilename)   // Set the filename
//                    .averageRating(0.0f)
//                    .build();
//
//            profile.setUser(providerUser);
//            providerUser.setServiceProviderProfile(profile);
//
//            User savedProviderUser = userRepository.save(providerUser);
//
//            String serviceImgFilename = copyImageToUploads("service-images", DEFAULT_SERVICE_IMAGE_NAME, serviceImagesTargetDir, "service_" + savedProviderUser.getServiceProviderProfile().getUserId() + "_");
//
//
//            String offeredServiceName = "";
//            // ... (your switch statement for offeredServiceName) ...
//            switch (assignedCategory) {
//                case MAKEUPARTIST: offeredServiceName = "Event Makeup Artistry"; break;
//                case HAIRSTYLIST: offeredServiceName = "Formal Hair Design"; break;
//                case PHOTOGRAPHER: offeredServiceName = "Lifestyle Photography Session"; break;
//                case MUSICIAN: offeredServiceName = "DJ for Parties"; break;
//                case CATERING: offeredServiceName = "Boutique Catering Service"; break;
//                default: offeredServiceName = "General " + assignedCategory.name().replace("_", " ") + " Task";
//            }
//
//
//            OfferedService service = OfferedService.builder()
//                    .provider(savedProviderUser)
//                    .name(offeredServiceName)
//                    .description("Professional " + offeredServiceName + " by " + providerBusinessName + ".")
//                    .price(BigDecimal.valueOf(80 + random.nextInt(321)))
//                    .available(true)
//                    .location(savedProviderUser.getServiceProviderProfile().getLocation())
//                    .imageFilename(serviceImgFilename) // Set the filename// Set category if your OfferedService has it
//                    .build();
//            offeredServiceRepository.save(service);
//
//            log.info("Created Service Provider: {} ({}) with ABN: {} and Offered Service: {}",
//                    savedProviderUser.getEmail(),
//                    savedProviderUser.getServiceProviderProfile().getServiceName(),
//                    savedProviderUser.getServiceProviderProfile().getABN(),
//                    service.getName());
//        }
//    }
//
//    private void createCustomerUsers() {
//        for (int i = 1; i <= 5; i++) {
//            User customerUser = User.builder()
//                    .firstName("Customer")
//                    .lastName(String.valueOf(i))
//                    .email("customer" + i + "@example.com")
//                    .password(passwordEncoder.encode("password123"))
//                    .phoneNumber("04555555" + String.format("%02d", i))
//                    .role(UserRole.CUSTOMER)
//                    .isActive(true)
//                    .build();
//            userRepository.save(customerUser);
//            log.info("Created Customer: {}", customerUser.getEmail());
//        }
//    }
//}

package com.cedric.Eventra.init;

import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.repository.ServiceProviderProfileRepository; // Not strictly needed if cascade is set up
import com.cedric.Eventra.repository.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    // ServiceProviderProfileRepository is not strictly needed here if User cascades saves to it
    // private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final PasswordEncoder passwordEncoder;

    // Inject the target directory paths from application.properties
    @Value("${file.upload-dir.profile-pictures}")
    private String profilePicturesTargetDir;

    @Value("${file.upload-dir.cover-photos}")
    private String coverPhotosTargetDir;

    @Value("${file.upload-dir.service-images}")
    private String serviceImagesTargetDir;

    // Relative path within resources to your source images
    private final String SOURCE_IMAGE_BASE_PATH = "Initialize-images/"; // Assuming this is directly under src/main/resources
    private final String DEFAULT_PROFILE_PIC_NAME = "profile-picture.png"; //
    private final String DEFAULT_COVER_PHOTO_NAME = "cover-photo.jpg"; //
    private final String DEFAULT_SERVICE_IMAGE_NAME = "service-image.jpg"; //

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization..."); //
        if (userRepository.count() == 0) { //
            log.info("No existing users found, proceeding with data initialization."); //
            createAdminUsers(); // Create Admins first
            createServiceProviders(); //
            createCustomerUsers(); //
        } else {
            log.info("Data already appears to exist. Skipping initialization."); //
        }
        log.info("Data initialization finished."); //
    }

    private String copyImageToUploads(String sourceSubDir, String sourceFilename, String targetSubDir, String targetFilePrefix) throws IOException { //
        Resource resource = new ClassPathResource(SOURCE_IMAGE_BASE_PATH + sourceSubDir + "/" + sourceFilename); //
        if (!resource.exists()) { //
            log.warn("Source image not found: {}/{}", sourceSubDir, sourceFilename); //
            return null; //
        }

        try (InputStream inputStream = resource.getInputStream()) { //
            String extension = ""; //
            int i = sourceFilename.lastIndexOf('.'); //
            if (i > 0) { //
                extension = sourceFilename.substring(i); //
            }
            String uniqueTargetFilename = targetFilePrefix + UUID.randomUUID().toString() + extension; //

            Path targetDirAsPath = Paths.get(targetSubDir); //
            if (!Files.exists(targetDirAsPath)) { //
                try {
                    Files.createDirectories(targetDirAsPath); //
                    log.info("Created target directory: {}", targetDirAsPath.toString()); //
                } catch (IOException e) {
                    log.error("Could not create target directory: {}", targetDirAsPath.toString(), e); //
                    throw e; //
                }
            }

            Path targetPath = targetDirAsPath.resolve(uniqueTargetFilename); //
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING); //
            log.info("Copied initial image {} to {}", sourceFilename, targetPath.toString()); //
            return uniqueTargetFilename; //
        }
    }


    private void createServiceProviders() throws IOException { //
        List<ServiceCategory> categories = Arrays.asList(ServiceCategory.values()); //
        String[] businessNamePrefixes = {"Elite", "Premium", "Pro", "Affordable", "Creative", "Dynamic", "Local"}; //
        String[] businessNameSuffixes = {"Services", "Solutions", "Studio", "Co.", "Group", "Experts"}; //
        Random random = new Random(); //

        for (int i = 1; i <= 20; i++) { //
            User providerUser = User.builder() //
                    .firstName("Provider" + i) //
                    .lastName("User" + i) //
                    .email("provider" + i + "@example.com") //
                    .password(passwordEncoder.encode("password123")) //
                    .phoneNumber("04123456" + String.format("%02d", i)) //
                    .role(UserRole.SERVICE_PROVIDER) //
                    .isActive(true) //
                    .build();

            ServiceCategory assignedCategory = categories.get(random.nextInt(categories.size())); //
            String providerBusinessName = businessNamePrefixes[random.nextInt(businessNamePrefixes.length)] + " " + //
                    assignedCategory.name().replace("_", " ") + " " + //
                    businessNameSuffixes[random.nextInt(businessNameSuffixes.length)] + " #" + i; //
            String dummyAbn = String.format("%011d", random.nextLong(100000000000L)); //

            String profilePicFilename = copyImageToUploads("profile-pictures", DEFAULT_PROFILE_PIC_NAME, profilePicturesTargetDir, "user_" + i + "_profile_"); //
            String coverPhotoFilename = copyImageToUploads("cover-photos", DEFAULT_COVER_PHOTO_NAME, coverPhotosTargetDir, "user_" + i + "_cover_"); //

            ServiceProviderProfile profile = ServiceProviderProfile.builder() //
                    .serviceName(providerBusinessName) //
                    .serviceCategory(assignedCategory) //
                    .ABN(dummyAbn) //
                    .location("Suburb " + i + ", NSW") //
                    .postcode(String.valueOf(2000 + i * 5)) //
                    .profilePictureFilename(profilePicFilename) //
                    .coverPhotoFilename(coverPhotoFilename) //
                    .averageRating(0.0f) //
                    .build();

            profile.setUser(providerUser); //
            providerUser.setServiceProviderProfile(profile); //

            User savedProviderUser = userRepository.save(providerUser); //

            String serviceImgFilename = copyImageToUploads("service-images", DEFAULT_SERVICE_IMAGE_NAME, serviceImagesTargetDir, "service_" + savedProviderUser.getServiceProviderProfile().getUserId() + "_"); //


            String offeredServiceName = ""; //
            switch (assignedCategory) { //
                case MAKEUPARTIST: offeredServiceName = "Event Makeup Artistry"; break; //
                case HAIRSTYLIST: offeredServiceName = "Formal Hair Design"; break; //
                case PHOTOGRAPHER: offeredServiceName = "Lifestyle Photography Session"; break; //
                case MUSICIAN: offeredServiceName = "DJ for Parties"; break; //
                case CATERING: offeredServiceName = "Boutique Catering Service"; break; //
                default: offeredServiceName = "General " + assignedCategory.name().replace("_", " ") + " Task"; //
            }


            OfferedService service = OfferedService.builder() //
                    .provider(savedProviderUser) //
                    .name(offeredServiceName) //
                    .description("Professional " + offeredServiceName + " by " + providerBusinessName + ".") //
                    .price(BigDecimal.valueOf(80 + random.nextInt(321))) //
                    .available(true) //
                    .location(savedProviderUser.getServiceProviderProfile().getLocation()) //
                    .imageFilename(serviceImgFilename) //
                    .build();
            offeredServiceRepository.save(service); //

            log.info("Created Service Provider: {} ({}) with ABN: {} and Offered Service: {}", //
                    savedProviderUser.getEmail(), //
                    savedProviderUser.getServiceProviderProfile().getServiceName(), //
                    savedProviderUser.getServiceProviderProfile().getABN(), //
                    service.getName()); //
        }
    }

    private void createCustomerUsers() { //
        for (int i = 1; i <= 5; i++) { //
            User customerUser = User.builder() //
                    .firstName("Customer") //
                    .lastName(String.valueOf(i)) //
                    .email("customer" + i + "@example.com") //
                    .password(passwordEncoder.encode("password123")) //
                    .phoneNumber("04555555" + String.format("%02d", i)) //
                    .role(UserRole.CUSTOMER) //
                    .isActive(true) //
                    .build();
            userRepository.save(customerUser); //
            log.info("Created Customer: {}", customerUser.getEmail()); //
        }
    }

    private void createAdminUsers() {
        String[][] adminDetails = {
                {"Cedric", "Admin", "cedric.admin@example.com", "0400000001"},
                {"Jason", "Admin", "jason.admin@example.com", "0400000002"},
                {"May", "Admin", "may.admin@example.com", "0400000003"}
        };

        for (String[] details : adminDetails) {
            User adminUser = User.builder()
                    .firstName(details[0])
                    .lastName(details[1])
                    .email(details[2])
                    .password(passwordEncoder.encode("adminP@$$wOrd")) // Use a strong, unique password for admins
                    .phoneNumber(details[3])
                    .role(UserRole.ADMIN)
                    .isActive(true) // Admins are typically active immediately
                    .build();
            userRepository.save(adminUser);
            log.info("Created Admin User: {}", adminUser.getEmail());
        }
    }
}