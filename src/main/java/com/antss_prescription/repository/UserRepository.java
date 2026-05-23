package com.antss_prescription.repository;

import com.antss_prescription.entity.User;
import com.antss_prescription.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByStatus(RegistrationStatus status);
    Optional<User> findByPasswordResetToken(String token);
}
