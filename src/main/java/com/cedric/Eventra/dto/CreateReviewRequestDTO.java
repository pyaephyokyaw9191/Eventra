package com.cedric.Eventra.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReviewRequestDTO {

    @NotNull(message = "Provider ID to review is required.")
    private Long providerProfileId; // The userId of the ServiceProviderProfile being reviewed

    @NotNull(message = "Service ID being reviewed is required.") // New field
    private Long offeredServiceId; // The ID of the specific service this review is for

    @NotNull(message = "Rating is required.")
    @Min(value = 1, message = "Rating must be at least 1.")
    @Max(value = 5, message = "Rating must be at most 5.")
    private Float rating;

    @NotBlank(message = "Comment cannot be empty.")
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters.")
    private String comment;

    // Optional: If you require a review to be tied to a specific completed booking
    // @NotNull(message = "Booking ID is required for the review.")
    // private Long bookingId;
}