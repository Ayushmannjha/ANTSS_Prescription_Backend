package com.antss_prescription.repository.prescription;

import com.antss_prescription.entity.prescription.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long> {

    List<Document> findByPatientId(Integer patientId);
    Optional<Document> findByIdAndPatientId(Integer documentId, Integer patientId);
    void deleteByIdAndPatientId(Integer documentId, Integer patientId);
}
