package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.OfferedService;
import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    // Show all services in a category (e.g. for discovery)
    List<OfferedService> findByServiceCategory(ServiceCategory serviceCategory);

    // Show all services by a provider
    List<OfferedService> findByProvider(User provider);
}
