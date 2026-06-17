package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.MedicineMaster;

@Repository
public interface MedicineMasterRepository extends JpaRepository<MedicineMaster, Long> {

    List<MedicineMaster> findByUserId(UUID userId);

    List<MedicineMaster> findByUserIdAndMedicineNameContainingIgnoreCase(
            UUID userId,
            String keyword
    );

    Optional<MedicineMaster> findByMedicineIdAndUserId(
            Long medicineId,
            UUID userId
    );
}