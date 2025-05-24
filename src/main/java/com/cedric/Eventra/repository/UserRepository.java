package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRoleAndIsActiveTrue(UserRole role); // Or findByRoleAndIsActive(UserRole role, Boolean isActive)
    // if you also want to fetch inactive ones sometimes.
    // For "get all service providers" for display, active is usually what you want.
}
