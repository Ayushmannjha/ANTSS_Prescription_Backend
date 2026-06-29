package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;

import com.antss_prescription.entity.prescription.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Document;

import jakarta.transaction.Transactional;
@Repository
public interface DocumentRepo extends JpaRepository<Document, Integer> {

    @Query("SELECT d FROM Document d WHERE d.patient.patientId = :patientId")
    List<Document> findByPatientId(@Param("patientId") Integer patientId);

    @Query("SELECT d FROM Document d WHERE d.id = :documentId AND d.patient.patientId = :patientId")
    Optional<Document> findByIdAndPatientId(@Param("documentId") Integer documentId, @Param("patientId") Integer patientId);

	void deleteByPatientPatientId(int patientId);

	List<Document> findByPatientPatientId(int patientId);
	boolean existsByPatientPatientId(int patientId);
    Optional<Document> findByUrl(String url);

    List<Document> getDocumentsByPrescription(Prescription prescription);

}
