package com.cedric.Eventra.controller;

import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.dto.CreateReviewRequestDTO; // Assuming this is the correct package
import com.cedric.Eventra.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews") // Base path for review-related endpoints
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Endpoint for an authenticated user (typically a customer) to create a new review
     * for a service provider and a specific service they offered.
     *
     * @param createReviewRequestDTO DTO containing review details (providerId, offeredServiceId, rating, comment).
     * @return ResponseEntity containing the standard Response object.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()") // Any authenticated user can attempt to create a review;
    // will check eligibility (e.g., completed booking).
    public ResponseEntity<Response> createReview(@Valid @RequestBody CreateReviewRequestDTO createReviewRequestDTO) {
        Response serviceResponse = reviewService.createReview(createReviewRequestDTO);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint to retrieve all reviews for a specific service provider.
     * Publicly accessible.
     *
     * @param providerProfileId The User ID of the ServiceProviderProfile whose reviews are to be fetched.
     * @return ResponseEntity containing the standard Response object.
     */
    @GetMapping("/provider/{providerProfileId}")
    public ResponseEntity<Response> getReviewsForProvider(@PathVariable Long providerProfileId) {
        Response serviceResponse = reviewService.getReviewsForProvider(providerProfileId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user to retrieve all reviews they have written.
     *
     * @return ResponseEntity containing the standard Response object with their list of reviews.
     */
    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> getMyReviews() {
        Response serviceResponse = reviewService.getMyReviews();
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user to update their own existing review.
     *
     * @param reviewId               The ID of the review to update.
     * @param updateReviewRequestDTO DTO containing the updated review details (rating, comment).
     * The providerProfileId and offeredServiceId in this DTO should match the original review's context.
     * @return ResponseEntity containing the standard Response object.
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> updateMyReview(@PathVariable Long reviewId,
                                                   @Valid @RequestBody CreateReviewRequestDTO updateReviewRequestDTO) {
        // The service layer will verify that the authenticated user is the owner of the review.
        // The updateReviewRequestDTO reuses CreateReviewRequestDTO; service layer will ensure provider/service context matches.
        Response serviceResponse = reviewService.updateMyReview(reviewId, updateReviewRequestDTO);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }

    /**
     * Endpoint for an authenticated user to delete their own review.
     *
     * @param reviewId The ID of the review to delete.
     * @return ResponseEntity containing the standard Response object.
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> deleteMyReview(@PathVariable Long reviewId) {
        // The service layer will verify that the authenticated user is the owner of the review.
        Response serviceResponse = reviewService.deleteMyReview(reviewId);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}