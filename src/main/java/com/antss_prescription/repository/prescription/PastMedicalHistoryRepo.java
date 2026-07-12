package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.PastMedicalHistory;
import java.util.List;
import java.util.UUID;

public interface PastMedicalHistoryRepo extends JpaRepository<PastMedicalHistory, Integer> {
    List<PastMedicalHistory> findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(UUID doctorId, String entityType, Long entityId);
}
