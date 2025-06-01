package com.cedric.Eventra.service;

import com.cedric.Eventra.dto.ProviderDashboardSummaryDTO;
import com.cedric.Eventra.dto.Response;
import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.entity.Review;
import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.BookingStatus;
import com.cedric.Eventra.enums.UserRole;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.exception.UnauthorizedException;
import com.cedric.Eventra.repository.BookingRepository;
import com.cedric.Eventra.repository.ReviewRepository;
import com.cedric.Eventra.repository.ServiceProviderProfileRepository;
import com.cedric.Eventra.service.ReportService;
import com.cedric.Eventra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository; // To get profile details

    @Override
    public Response getMyProviderDashboardSummary() {
        User providerUser = userService.getCurrentLoggedInUser();
        if (providerUser.getRole() != UserRole.SERVICE_PROVIDER) {
            return Response.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("User is not a service provider.")
                    .build();
        }

        ServiceProviderProfile providerProfile = serviceProviderProfileRepository.findByUserId(providerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Service provider profile not found for user ID: " + providerUser.getId()));

        // Fetch all bookings for this provider
        List<Booking> allBookings = bookingRepository.findByOfferedService_Provider_Id(providerUser.getId());
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Calculate Booking Counts
        long totalBookingsLifetime = allBookings.size();
        Map<BookingStatus, Long> bookingStatusCounts = allBookings.stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));

        long pendingBookings = bookingStatusCounts.getOrDefault(BookingStatus.PENDING, 0L);
        long acceptedAwaitingPaymentBookings = bookingStatusCounts.getOrDefault(BookingStatus.ACCEPTED_AWAITING_PAYMENT, 0L);
        long confirmedBookings = bookingStatusCounts.getOrDefault(BookingStatus.CONFIRMED, 0L);
        long cancelledBookingsLifetime = bookingStatusCounts.getOrDefault(BookingStatus.CANCELLED, 0L);

        long completedBookingsLifetime = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();
        long completedBookingsLast30Days = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getCreatedAt() != null && // Ensure createdAt is not null
                        b.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        // Calculate Financial Snapshot
        BigDecimal potentialRevenueFromConfirmed = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getPrice() != null)
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueFromCompletedLifetime = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getPrice() != null)
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueFromCompletedLast30Days = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED &&
                        b.getPrice() != null &&
                        b.getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(thirtyDaysAgo))
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Review Snapshot
        // The average rating is already on providerProfile, assuming it's kept up-to-date by ReviewService
        Float currentAverageRating = providerProfile.getAverageRating() != null ? providerProfile.getAverageRating() : 0.0f;
        // Fetch total reviews count for this provider
        List<Review> reviews = reviewRepository.findByProviderOrderByCreatedAtDesc(providerProfile);
        int totalReviews = reviews.size();


        ProviderDashboardSummaryDTO summaryDTO = ProviderDashboardSummaryDTO.builder()
                .totalBookingsLifetime(totalBookingsLifetime)
                .pendingBookings(pendingBookings)
                .acceptedAwaitingPaymentBookings(acceptedAwaitingPaymentBookings)
                .confirmedBookings(confirmedBookings)
                .completedBookingsLast30Days(completedBookingsLast30Days)
                .completedBookingsLifetime(completedBookingsLifetime)
                .cancelledBookingsLifetime(cancelledBookingsLifetime)
                .potentialRevenueFromConfirmed(potentialRevenueFromConfirmed)
                .totalRevenueFromCompletedLast30Days(totalRevenueFromCompletedLast30Days)
                .totalRevenueFromCompletedLifetime(totalRevenueFromCompletedLifetime)
                .currentAverageRating(currentAverageRating)
                .totalReviews(totalReviews)
                .bookingStatusCounts(new EnumMap<>(bookingStatusCounts)) // Ensure EnumMap for consistent ordering if needed
                .build();

        return Response.builder()
                .status(HttpStatus.OK.value())
                .message("Provider dashboard summary retrieved successfully.")
                .dashboardSummary(summaryDTO) // Use the new field in Response DTO
                .build();
    }
}