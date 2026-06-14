package com.antss_prescription.repository.prescription;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.TestRequested;

@Repository
public interface TestRequestedRepo extends JpaRepository<TestRequested, Integer> {

    List<TestRequested> findByPatientRegistrationRegistrationNumber(String registrationNumber);

    List<TestRequested> findByDocumentId(Integer documentId);

    List<TestRequested> findByPrescription(Prescription prescription);
    
    void deleteByPrescription(Prescription prescription);
}