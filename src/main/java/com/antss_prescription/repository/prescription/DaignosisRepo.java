package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.Diagnosis;
import java.util.List;
import java.util.UUID;

public interface DaignosisRepo extends JpaRepository<Diagnosis, Integer>{
    List<Diagnosis> findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(UUID doctorId, String entityType, Long entityId);
}
