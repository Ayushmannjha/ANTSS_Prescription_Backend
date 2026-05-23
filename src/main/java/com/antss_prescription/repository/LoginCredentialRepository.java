package com.antss_prescription.repository;

import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoginCredentialRepository extends JpaRepository<LoginCredential, UUID> {
    Optional<LoginCredential> findByUsername(String username);
    Optional<LoginCredential> findByUser(User user);
    Optional<LoginCredential> findByUserId(UUID userId);
}
