package com.antss_prescription.repository;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    Optional<Clinic> findByClinicCode(String clinicCode);
    List<Clinic> findByUser(User user);
    List<Clinic> findByUserId(UUID userId);
    List<Clinic> findByOwnerId(UUID ownerId);
    List<Clinic> findByUserIdOrOwnerId(UUID userId, UUID ownerId);
    /** All clinics where this user is the owner. */
    List<Clinic> findByOwner(User owner);
 
    default List<Clinic> findByOwnerUser(User user) {
        return findByOwner(user);
    }

}
