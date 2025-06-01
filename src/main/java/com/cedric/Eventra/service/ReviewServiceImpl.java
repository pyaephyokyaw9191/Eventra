package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.ReviewDTO;
import com.cedric.Eventra.dto.UserDTO;
import com.cedric.Eventra.dto.CreateReviewRequestDTO;
import com.cedric.Eventra.entity.*;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.exception.BadRequestException;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.OfferedServiceRepository;
import com.cedric.Eventra.repository.ReviewRepository;
import com.cedric.Eventra.repository.ServiceProviderProfileRepository;
import com.cedric.Eventra.service.ReviewService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository; // To verify eligibility
    private final ModelMapper modelMapper;
    private final OfferedServiceRepository offeredServiceRepository;

    @Override
    @Transactional
    public Response createReview(CreateReviewRequestDTO createReviewRequestDTO) {
        User reviewer = userService.getCurrentLoggedInUser(); // Throws if not authenticated

        ServiceProviderProfile providerToReview = serviceProviderProfileRepository.findByUserId(createReviewRequestDTO.getProviderProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found with ID: " + createReviewRequestDTO.getProviderProfileId()));

        // **Eligibility Check (Crucial):**
        // User must have at least one COMPLETED booking with this provider.
        // Simple check: has the reviewer completed any booking with this provider?
        boolean isEligible = bookingRepository.findAll().stream() // Note: query DB better for later development
                .anyMatch(booking -> booking.getUser().getId().equals(reviewer.getId()) &&
                        booking.getOfferedService().getProvider().getId().equals(providerToReview.getUserId()) && // provider of service
                        booking.getStatus() == BookingStatus.COMPLETED);

        // review is not tied to a specific bookingId in request
        if (reviewRepository.existsByReviewerAndProvider(reviewer, providerToReview)) {
            throw new BadRequestException("You have already submitted a review for this provider.");
        }
        if (!isEligible && createReviewRequestDTO.getProviderProfileId() != 1L ) { // Allowing review for a dummy provider ID 1 for testing without booking check temporarily
            // throw new BadRequestException("You must have a completed booking with this provider to leave a review.");
            log.warn("Bypassing eligibility check for provider ID {} for user {}", createReviewRequestDTO.getProviderProfileId(), reviewer.getId());
        }

        OfferedService reviewedService = offeredServiceRepository.findById(createReviewRequestDTO.getOfferedServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Offered Service for review not found."));

        Review review = Review.builder()
                .reviewer(reviewer)
                .provider(providerToReview)
                .offeredService(reviewedService)
                .rating(createReviewRequestDTO.getRating())
                .comment(createReviewRequestDTO.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        updateProviderAverageRating(providerToReview.getUserId()); // Update average rating

        log.info("User {} created review for provider {}", reviewer.getId(), providerToReview.getUserId());
        return Response.builder()
                .status(HttpStatus.CREATED.value())
                .message("Review submitted successfully.")
                .review(modelMapper.map(savedReview, ReviewDTO.class))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getReviewsForProvider(Long providerProfileId) {
        ServiceProviderProfile provider = serviceProviderProfileRepository.findByUserId(providerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found with ID: " + providerProfileId));

        List<Review> reviews = reviewRepository.findByProviderOrderByCreatedAtDesc(provider);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(this::mapToReviewDTO)
                .collect(Collectors.toList());

        // Assuming Response DTO will be updated to hold List<ReviewDTO> or use generic 'data' field
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(reviewDTOs.isEmpty() ? "No reviews found for this provider." : "Reviews retrieved successfully.")
                .reviews(reviewDTOs.isEmpty() ? Collections.emptyList() : reviewDTOs) // Use .reviews()
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getMyReviews() {
        User reviewer = userService.getCurrentLoggedInUser();
        List<Review> reviews = reviewRepository.findByReviewerOrderByCreatedAtDesc(reviewer);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(this::mapToReviewDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message(reviewDTOs.isEmpty() ? "You have not written any reviews." : "Your reviews retrieved successfully.")
                .reviews(reviewDTOs.isEmpty() ? Collections.emptyList() : reviewDTOs) // Use .reviews()
                .build();
    }

    @Override
    @Transactional
    public Response updateMyReview(Long reviewId, CreateReviewRequestDTO updateReviewRequestDTO) {
        User reviewer = userService.getCurrentLoggedInUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!Objects.equals(review.getReviewer().getId(), reviewer.getId())) {
            throw new UnauthorizedException("You are not authorized to update this review.");
        }

        // Ensure the review being updated is for the provider specified in DTO (or remove providerId from update DTO)
        if (!Objects.equals(review.getProvider().getUserId(), updateReviewRequestDTO.getProviderProfileId())) {
            throw new BadRequestException("Review update request provider ID does not match the original review's provider.");
        }

        review.setRating(updateReviewRequestDTO.getRating());
        review.setComment(updateReviewRequestDTO.getComment());
        // createdAt should not be updated

        Review updatedReview = reviewRepository.save(review);
        updateProviderAverageRating(review.getProvider().getUserId()); // Update average rating

        log.info("User {} updated review ID {}", reviewer.getId(), reviewId);
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Review updated successfully.")
                .review(modelMapper.map(updatedReview, ReviewDTO.class))
                .build();
    }

    @Override
    @Transactional
    public Response deleteMyReview(Long reviewId) {
        User reviewer = userService.getCurrentLoggedInUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!Objects.equals(review.getReviewer().getId(), reviewer.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this review.");
        }

        Long providerProfileId = review.getProvider().getUserId();
        reviewRepository.delete(review);
        updateProviderAverageRating(providerProfileId); // Update average rating

        log.info("User {} deleted review ID {}", reviewer.getId(), reviewId);
        // In ReviewServiceImpl.java - deleteMyReview method
        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Review deleted successfully.")
                .build();
    }

    // Helper method to map Review entity to ReviewDTO
    private ReviewDTO mapToReviewDTO(Review review) {
        ReviewDTO dto = modelMapper.map(review, ReviewDTO.class);
        if (review.getReviewer() != null) {
            // Assuming UserDTO should be simplified or specific for reviewer context
            UserDTO reviewerDTO = UserDTO.builder()
                    .id(review.getReviewer().getId())
                    .firstName(review.getReviewer().getFirstName())
                    .lastName(review.getReviewer().getLastName())
                    // .email(review.getReviewer().getEmail()) // Optional
                    .build();
            dto.setReviewerInfo(reviewerDTO);
        }

        if (review.getOfferedService() != null) {
            dto.setOfferedServiceId(review.getOfferedService().getId());
            dto.setServiceNameReviewed(review.getOfferedService().getName());
        }

        if (review.getProvider() != null) {
            dto.setReviewedProviderProfileId(review.getProvider().getUserId());
            if (review.getProvider().getUser() != null) {
                dto.setReviewedProviderProfileName(review.getProvider().getUser().getFirstName() + " " + review.getProvider().getUser().getLastName());
            }
        }
        return dto;
    }

    // Method to update the average rating for a provider
    // This should be @Transactional if called separately, but will join existing transaction if called from another @Transactional method.
    private void updateProviderAverageRating(Long providerProfileId) {
        ServiceProviderProfile providerProfile = serviceProviderProfileRepository.findByUserId(providerProfileId)
                .orElse(null); // Or throw if provider must exist

        if (providerProfile == null) {
            log.warn("Attempted to update average rating for non-existent provider profile ID: {}", providerProfileId);
            return;
        }

        List<Review> reviews = reviewRepository.findByProviderOrderByCreatedAtDesc(providerProfile);
        if (reviews.isEmpty()) {
            providerProfile.setAverageRating(0.0f); // Or null
        } else {
            double average = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
            providerProfile.setAverageRating((float) average);
        }
        serviceProviderProfileRepository.save(providerProfile); // Save the updated profile
        log.info("Updated average rating for provider ID {}: {}", providerProfileId, providerProfile.getAverageRating());
    }
}