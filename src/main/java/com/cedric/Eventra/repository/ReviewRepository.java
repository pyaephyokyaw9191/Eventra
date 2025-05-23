package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.Review;
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Finds all reviews for a specific service provider profile.
     * Ordered by creation date descending.
     */
    List<Review> findByProviderOrderByCreatedAtDesc(ServiceProviderProfile provider);

    /**
     * Finds all reviews written by a specific user (reviewer).
     * Ordered by creation date descending.
     */
    List<Review> findByReviewerOrderByCreatedAtDesc(User reviewer);

    /**
     * Checks if a specific reviewer has already submitted a review for a specific provider.
     * Useful if you want to limit one review per user per provider (unless reviews are per booking).
     */
    boolean existsByReviewerAndProvider(User reviewer, ServiceProviderProfile provider);
}