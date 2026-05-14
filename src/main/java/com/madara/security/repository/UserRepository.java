package com.madara.security.repository;

import com.madara.security.model.AuthProvider;
import com.madara.security.model.Role;
import com.madara.security.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.isAccountEnabled = true")
    long countVerifiedUsers(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.isAccountEnabled = false")
    long countUnverifiedUsers(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.authProvider = :provider")
    long countByRoleAndAuthProvider(@Param("role") Role role, @Param("provider") AuthProvider provider);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.createdAt >= :since")
    long countByRoleAndCreatedAfter(@Param("role") Role role, @Param("since") Instant since);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findAllByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.seededAdmin = true")
    Optional<User> findSeededAdmin();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role AND u.seededAdmin = false")
    long countRealAdmins(@Param("role") Role role);
}
