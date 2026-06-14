package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.MedicineMaster;

@Repository
public interface MedicineMasterRepository extends JpaRepository<MedicineMaster, Long> {

    List<MedicineMaster> findByUserUserId(Long userId);

    List<MedicineMaster> findByUserUserIdAndMedicineNameContainingIgnoreCase(
            UUID userId,
            String keyword
    );

    Optional<MedicineMaster> findByMedicineIdAndUserUserId(
            Long medicineId,
            Long userId
    );
}