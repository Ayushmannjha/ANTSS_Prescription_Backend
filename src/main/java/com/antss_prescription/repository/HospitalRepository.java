package com.antss_prescription.repository;

import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByHospitalCode(String hospitalCode);
    List<Hospital> findByUserId(UUID userId);
    List<Hospital> findByOwnerId(UUID ownerId);
    List<Hospital> findByUserIdOrOwnerId(UUID userId, UUID ownerId);
    
    /** All hospitals where this user is the owner. */
    List<Hospital> findByOwner(User owner);
 
    /** Convenience alias matching the field name used in the service. */
 
}
