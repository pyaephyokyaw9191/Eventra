package com.cedric.Eventra.init;

import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.Review; // Import Review
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.repository.ReviewRepository; // Import ReviewRepository
import com.cedric.Eventra.repository.ServiceProviderProfileRepository; // Make sure this is injected
import com.cedric.Eventra.repository.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository; // Added
    private final ServiceProviderProfileRepository serviceProviderProfileRepository; // Added

    @Value("${file.upload-dir.profile-pictures}")
    private String profilePicturesTargetDir;

    @Value("${file.upload-dir.cover-photos}")
    private String coverPhotosTargetDir;

    @Value("${file.upload-dir.service-images}")
    private String serviceImagesTargetDir;

    private final String SOURCE_IMAGE_BASE_PATH = "Initialize-images/";
    private final String DEFAULT_PROFILE_PIC_NAME = "profile-picture.png";
    private final String DEFAULT_COVER_PHOTO_NAME = "cover-photo.jpg";
    private final String DEFAULT_SERVICE_IMAGE_NAME = "service-image.jpg";

    private final Random random = new Random(); // Class level Random instance

    // Sample review comments
    private static final List<String> SAMPLE_REVIEW_COMMENTS = Arrays.asList(
            "Absolutely fantastic service! Highly recommended.",
            "Very professional and delivered exactly what was promised.",
            "Good value for money, I'm quite satisfied.",
            "Could be better in terms of communication, but the work was okay.",
            "An amazing experience from start to finish. Will use again!",
            "The quality was exceptional. Above and beyond.",
            "Friendly staff and great results.",
            "Met my expectations. Solid service.",
            "There were a few hiccups, but overall a positive experience.",
            "I'm really happy with the outcome. Thank you!",
            "Quick, efficient, and effective.",
            "Wonderful to work with this provider.",
            "Made my event truly special!",
            "The attention to detail was impressive."
    );


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");
        if (userRepository.count() == 0) {
            log.info("No existing users found, proceeding with data initialization.");
            createAdminUsers();
            // These methods now return the created users for review generation
            List<User> serviceProviders = createServiceProviders();
            List<User> customers = createCustomerUsers();
            createReviews(serviceProviders, customers); // Pass them to createReviews
        } else {
            log.info("Data already appears to exist. Skipping initialization.");
        }
        log.info("Data initialization finished.");
    }

    private String copyImageToUploads(String sourceSubDir, String sourceFilename, String targetSubDir, String targetFilePrefix) throws IOException {
        Resource resource = new ClassPathResource(SOURCE_IMAGE_BASE_PATH + sourceSubDir + "/" + sourceFilename);
        if (!resource.exists()) {
            log.warn("Source image not found: {}/{}", sourceSubDir, sourceFilename);
            return "default_image_not_found.png";
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String extension = "";
            int i = sourceFilename.lastIndexOf('.');
            if (i > 0) {
                extension = sourceFilename.substring(i);
            }
            String uniqueTargetFilename = targetFilePrefix + UUID.randomUUID().toString() + extension;

            Path targetDirAsPath = Paths.get(targetSubDir);
            if (!Files.exists(targetDirAsPath)) {
                try {
                    Files.createDirectories(targetDirAsPath);
                    log.info("Created target directory: {}", targetDirAsPath.toString());
                } catch (IOException e) {
                    log.error("Could not create target directory: {}", targetDirAsPath.toString(), e);
                    throw e;
                }
            }

            Path targetPath = targetDirAsPath.resolve(uniqueTargetFilename);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied initial image {} to {}", sourceFilename, targetPath.toString());
            return uniqueTargetFilename;
        }
    }

    private List<User> createServiceProviders() throws IOException {
        List<User> createdProviders = new ArrayList<>();
        List<ServiceCategory> allCategories = Arrays.asList(ServiceCategory.values());
        String[] businessNamePrefixes = {"Elite", "Premium", "Pro", "Affordable", "Creative", "Dynamic", "Local", "Ultimate", "Supreme", "Prime"};
        String[] businessNameSuffixes = {"Services", "Solutions", "Studio", "Co.", "Group", "Experts", "Pros", "Enterprises", "Ventures"};
        String[] serviceNameAdjectives = {"Basic", "Standard", "Premium", "Deluxe", "Express", "Comprehensive"};


        for (int i = 1; i <= 100; i++) {
            User providerUser = User.builder()
                    .firstName("Provider" + i)
                    .lastName("User" + i)
                    .email("provider" + i + "@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("04" + String.format("%08d", random.nextInt(100000000)))
                    .role(UserRole.SERVICE_PROVIDER)
                    .isActive(true)
                    .build();

            ServiceCategory primaryCategory = allCategories.get(random.nextInt(allCategories.size()));
            String providerBusinessName = businessNamePrefixes[random.nextInt(businessNamePrefixes.length)] + " " +
                    primaryCategory.name().replace("_", " ") + " " +
                    businessNameSuffixes[random.nextInt(businessNameSuffixes.length)] + " #" + i;
            String dummyAbn = String.format("%011d", Math.abs(random.nextLong() % 100000000000L));

            String profilePicFilename = copyImageToUploads("profile-pictures", DEFAULT_PROFILE_PIC_NAME, profilePicturesTargetDir, "user_" + i + "_profile_");
            String coverPhotoFilename = copyImageToUploads("cover-photos", DEFAULT_COVER_PHOTO_NAME, coverPhotosTargetDir, "user_" + i + "_cover_");

            ServiceProviderProfile profile = ServiceProviderProfile.builder()
                    .serviceName(providerBusinessName)
                    .serviceCategory(primaryCategory)
                    .ABN(dummyAbn)
                    .location("Suburb " + (random.nextInt(500) + 1) + ", NSW")
                    .postcode(String.valueOf(2000 + random.nextInt(1000)))
                    .profilePictureFilename(profilePicFilename)
                    .coverPhotoFilename(coverPhotoFilename)
                    .averageRating(0.0f) // Initial rating before reviews
                    .build();

            profile.setUser(providerUser);
            providerUser.setServiceProviderProfile(profile);

            User savedProviderUser = userRepository.save(providerUser);
            createdProviders.add(savedProviderUser); // Add to list
            log.info("Created Service Provider: {} ({})", savedProviderUser.getEmail(), savedProviderUser.getServiceProviderProfile().getServiceName());

            List<ServiceCategory> providerServiceCategories = new ArrayList<>(allCategories);
            Collections.shuffle(providerServiceCategories);

            for (int j = 1; j <= 3; j++) {
                ServiceCategory serviceCategoryForThisService = primaryCategory;
                // Example: To get more diverse services under one provider:
                // if (j > 1 && providerServiceCategories.size() >= j) {
                //     serviceCategoryForThisService = providerServiceCategories.get(j - 1);
                // }


                String specificOfferedServiceName = "";
                switch (serviceCategoryForThisService) {
                    case MAKEUPARTIST: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Event Makeup"; break;
                    case HAIRSTYLIST: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Hair Styling"; break;
                    case PHOTOGRAPHER: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Photo Session"; break;
                    case MUSICIAN: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Live Music Set"; break;
                    case CATERING: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Catering Package"; break;
                    case EVENTHOST: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Event Hosting"; break;
                    case VENUEORGANISER: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Venue Setup"; break;
                    case FLORIST: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Floral Arrangement"; break;
                    case VENUERENTAL: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Venue Hire"; break;
                    case BARTENDER: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " Bartending Service"; break;
                    default: specificOfferedServiceName = serviceNameAdjectives[random.nextInt(serviceNameAdjectives.length)] + " " + serviceCategoryForThisService.name().replace("_", " ") + " Service";
                }
                specificOfferedServiceName = (specificOfferedServiceName.length() > 250) ? specificOfferedServiceName.substring(0, 250) : specificOfferedServiceName;

                String serviceImgFilename = copyImageToUploads("service-images", DEFAULT_SERVICE_IMAGE_NAME, serviceImagesTargetDir, "service_" + savedProviderUser.getId() + "_" + (j) + "_");

                OfferedService service = OfferedService.builder()
                        .provider(savedProviderUser)
                        .name(specificOfferedServiceName + " Option " + j)
                        .description("Quality " + specificOfferedServiceName + " provided by " + providerBusinessName + ". This is offering number " + j + ".")
                        .price(BigDecimal.valueOf(50 + random.nextInt(451)))
                        .available(random.nextBoolean())
                        .location(savedProviderUser.getServiceProviderProfile().getLocation())
                        .imageFilename(serviceImgFilename)
                        .build();
                offeredServiceRepository.save(service);
                log.debug("   - Offered Service {}: {} for Provider {}", j, service.getName(), savedProviderUser.getEmail());
            }
        }
        return createdProviders;
    }

    private List<User> createCustomerUsers() {
        List<User> createdCustomers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            User customerUser = User.builder()
                    .firstName("Customer" + i)
                    .lastName("User" + i)
                    .email("customer" + i + "@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("04" + String.format("%08d", random.nextInt(100000000)))
                    .role(UserRole.CUSTOMER)
                    .isActive(true)
                    .build();
            createdCustomers.add(userRepository.save(customerUser)); // Save and add to list
            log.info("Created Customer: {}", customerUser.getEmail());
        }
        return createdCustomers;
    }

    private void createAdminUsers() {
        String[][] adminDetails = {
                {"Cedric", "Admin", "cedric.admin@example.com", "0400000001"},
                {"Jason", "Admin", "jason.admin@example.com", "0400000002"},
                {"May", "Admin", "may.admin@example.com", "0400000003"}
        };

        for (String[] details : adminDetails) {
            if (userRepository.findByEmail(details[2]).isEmpty()) {
                User adminUser = User.builder()
                        .firstName(details[0])
                        .lastName(details[1])
                        .email(details[2])
                        .password(passwordEncoder.encode("adminP@$$wOrd"))
                        .phoneNumber(details[3])
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(adminUser);
                log.info("Created Admin User: {}", adminUser.getEmail());
            } else {
                log.info("Admin User {} already exists. Skipping.", details[2]);
            }
        }
    }

    // New method to create reviews
    private void createReviews(List<User> serviceProviders, List<User> customers) {
        log.info("Starting review creation...");
        if (customers.isEmpty() || serviceProviders.isEmpty()) {
            log.warn("No customers or service providers available to create reviews.");
            return;
        }

        for (User providerUser : serviceProviders) {
            ServiceProviderProfile providerProfile = providerUser.getServiceProviderProfile();
            if (providerProfile == null) {
                log.warn("Provider {} has no profile, skipping review creation.", providerUser.getEmail());
                continue;
            }

            List<OfferedService> offeredServices = offeredServiceRepository.findByProvider(providerUser);
            if (offeredServices.isEmpty()) {
                log.warn("Provider {} has no offered services, skipping review creation.", providerUser.getEmail());
                continue;
            }

            int numberOfReviewsToCreate = random.nextInt(8) + 2; // Create 2 to 9 reviews per provider
            log.debug("Attempting to create {} reviews for provider {}", numberOfReviewsToCreate, providerUser.getEmail());


            for (int i = 0; i < numberOfReviewsToCreate; i++) {
                User reviewer = customers.get(random.nextInt(customers.size()));
                OfferedService serviceToReview = offeredServices.get(random.nextInt(offeredServices.size()));

                // Simple check to avoid duplicate review for the same service by the same user in this seed run.
                // A more robust check would query the repository.
                boolean alreadyReviewedThisServiceByThisUser = reviewRepository.findAll().stream()
                        .anyMatch(r -> r.getReviewer().getId().equals(reviewer.getId()) &&
                                r.getProvider().getUserId().equals(providerProfile.getUserId()) &&
                                r.getOfferedService().getId().equals(serviceToReview.getId()));

                if(alreadyReviewedThisServiceByThisUser) {
                    log.trace("Customer {} already reviewed service {} for provider {}. Skipping.", reviewer.getId(), serviceToReview.getId(), providerProfile.getUserId());
                    continue; // Skip if this customer already reviewed this specific service for this provider
                }


                float rating = (float) (Math.round(random.nextDouble() * 4 + 1) * 2) / 2.0f; // Ratings: 1.0, 1.5, ..., 4.5, 5.0
                if (rating < 1.0f) rating = 1.0f;
                if (rating > 5.0f) rating = 5.0f;


                String comment = SAMPLE_REVIEW_COMMENTS.get(random.nextInt(SAMPLE_REVIEW_COMMENTS.size()));
                if (rating < 3.0 && random.nextBoolean()) { // Add some constructive criticism for lower ratings
                    comment += " Room for improvement.";
                } else if (rating >= 4.5 && random.nextBoolean()) {
                    comment += " Truly outstanding!";
                }


                Review review = Review.builder()
                        .reviewer(reviewer)
                        .provider(providerProfile)
                        .offeredService(serviceToReview) // Link review to a specific service
                        .rating(rating)
                        .comment(comment)
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(30))) // Reviews from the past 30 days
                        .build();
                reviewRepository.save(review);
                log.debug("   - Created review by {} for service '{}' (Provider: {}) with rating {}",
                        reviewer.getEmail(), serviceToReview.getName(), providerUser.getEmail(), rating);
            }
            // Update average rating for the provider after creating all their reviews
            updateProviderAverageRating(providerProfile.getUserId());
        }
        log.info("Review creation finished.");
    }

    // Helper method to update average rating (similar to one in ReviewServiceImpl)
    private void updateProviderAverageRating(Long providerProfileId) {
        ServiceProviderProfile providerProfile = serviceProviderProfileRepository.findByUserId(providerProfileId)
                .orElse(null);

        if (providerProfile == null) {
            log.warn("Attempted to update average rating for non-existent provider profile ID during init: {}", providerProfileId);
            return;
        }

        List<Review> reviews = reviewRepository.findByProviderOrderByCreatedAtDesc(providerProfile);
        if (reviews.isEmpty()) {
            providerProfile.setAverageRating(0.0f);
        } else {
            double average = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
            // Round to one decimal place
            providerProfile.setAverageRating((float) (Math.round(average * 10.0) / 10.0));
        }
        serviceProviderProfileRepository.save(providerProfile); // Save the updated profile
        log.info("Updated average rating for provider ID {} to: {}", providerProfileId, providerProfile.getAverageRating());
    }
}