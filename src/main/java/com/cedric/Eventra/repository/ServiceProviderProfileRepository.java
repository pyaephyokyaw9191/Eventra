package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.ServiceProviderProfile;
import com.cedric.Eventra.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceProviderProfileRepository extends JpaRepository<ServiceProviderProfile, Long> {

    // For fetching profile using user ID (since profile is 1-to-1 with User)
    Optional<ServiceProviderProfile> findByUserId(Long userId);

    // Optional: for checking profile existence
    boolean existsByUserId(Long userId);

    List<ServiceProviderProfile> findByServiceCategory(ServiceCategory category);
}
