package com.antss_prescription.repository.prescription;

import com.antss_prescription.entity.prescription.Investigations;
import com.antss_prescription.entity.prescription.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestigationsRepo extends JpaRepository<Investigations, Integer> {

    List<Investigations> findByPatientRegistrationRegistrationNumber(String registrationNumber);

    List<Investigations> findByDocumentId(Integer documentId);
    
    List<Investigations> findByPrescription(Prescription prescription);
    
    void deleteByPrescription(Prescription prescription);

    List<Investigations> findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(UUID doctorId, String entityType, Long entityId);
}
