package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.GeneralExamination;
import java.util.List;
import java.util.UUID;

@Repository
public interface GeneralExaminationRepo extends JpaRepository<GeneralExamination, Integer> {
    List<GeneralExamination> findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(UUID doctorId, String entityType, Long entityId);
}
