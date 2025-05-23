package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.CreateReviewRequestDTO;
import com.cedric.Eventra.dto.Response;

public interface ReviewService {

    /**
     * Allows an authenticated user (customer) to create a review for a service provider.
     * Business logic should verify if the user is eligible to review (e.g., completed a booking).
     *
     * @param createReviewRequestDTO DTO containing review details.
     * @return Response object with the created ReviewDTO.
     */
    Response createReview(CreateReviewRequestDTO createReviewRequestDTO);

    /**
     * Gets all reviews for a specific service provider.
     *
     * @param providerProfileId The userId of the ServiceProviderProfile.
     * @return Response object with a list of ReviewDTOs.
     */
    Response getReviewsForProvider(Long providerProfileId);

    /**
     * Gets all reviews written by the currently authenticated user.
     *
     * @return Response object with a list of ReviewDTOs.
     */
    Response getMyReviews();

    /**
     * Allows an authenticated user to update their own review.
     *
     * @param reviewId            The ID of the review to update.
     * @param updateReviewRequestDTO DTO containing updated review details (e.g., rating, comment).
     * @return Response object with the updated ReviewDTO.
     */
    Response updateMyReview(Long reviewId, CreateReviewRequestDTO updateReviewRequestDTO); // Can reuse Create DTO for update fields

    /**
     * Allows an authenticated user to delete their own review.
     *
     * @param reviewId The ID of the review to delete.
     * @return Response object indicating success or failure.
     */
    Response deleteMyReview(Long reviewId);

    // This method might be called internally after review changes or periodically.
    // It could also be part of ServiceProviderProfileService.
    // void updateProviderAverageRating(Long providerProfileId);
}
