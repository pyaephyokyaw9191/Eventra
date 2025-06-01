package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    // Show all services by a provider
    List<OfferedService> findByProvider(User provider);

    // NEW: Find services offered by providers in a specific category
    @Query("SELECT s FROM OfferedService s WHERE s.provider.serviceProviderProfile.serviceCategory = :category")
    List<OfferedService> findByProviderCategory(com.cedric.Eventra.enums.ServiceCategory category);
}
