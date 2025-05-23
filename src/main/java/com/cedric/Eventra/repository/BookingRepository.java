package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.Booking;
import com.cedric.Eventra.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Book;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByOfferedService_Provider_Id(Long providerId);

    List<Booking> findByOfferedServiceId(Long serviceId);

    List<Booking> findByStatus(BookingStatus status);
}
