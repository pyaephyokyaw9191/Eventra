package com.cedric.Eventra.dto;

import com.cedric.Eventra.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ProviderDashboardSummaryDTO {
    // Booking Counts
    private long totalBookingsLifetime;
    private long pendingBookings; // Needs provider action
    private long acceptedAwaitingPaymentBookings;
    private long confirmedBookings; // Upcoming/paid
    private long completedBookingsLast30Days; // Example period
    private long completedBookingsLifetime;
    private long cancelledBookingsLifetime;

    // Financial Snapshot (based on Booking.price)
    private BigDecimal potentialRevenueFromConfirmed; // Sum of prices of CONFIRMED bookings
    private BigDecimal totalRevenueFromCompletedLast30Days; // Sum of prices of COMPLETED bookings in period
    private BigDecimal totalRevenueFromCompletedLifetime;

    // Review Snapshot
    private Float currentAverageRating;
    private int totalReviews;

    // Optional: More detailed breakdown
    // private Map<BookingStatus, Long> bookingStatusCounts;
    // private Map<Integer, Long> ratingDistribution; // e.g., {5: 10, 4: 5, ...}
}