package com.cedric.Eventra.repository;

import com.cedric.Eventra.entity.User;
import com.cedric.Eventra.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRoleAndIsActiveTrue(UserRole role);

    // Added method to fetch users by role (both active and inactive)
    List<User> findByRole(UserRole role);


}
