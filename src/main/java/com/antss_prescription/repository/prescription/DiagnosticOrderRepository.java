package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.DiagnosticOrder;
import com.antss_prescription.entity.prescription.Prescription;

public interface DiagnosticOrderRepository extends JpaRepository<DiagnosticOrder, Integer> {
    List<DiagnosticOrder> findByPatientRegistrationRegistrationNumber(String registrationNumber);
    List<DiagnosticOrder> findByPrescription(Prescription prescription);
    List<DiagnosticOrder> findByReportDocumentId(Integer documentId);
    void deleteByPrescription(Prescription prescription);
    Optional<DiagnosticOrder> findByLegacySource(String legacySource);
}
