package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.PastMedicalHistory;

public interface PastMedicalHistoryRepo extends JpaRepository<PastMedicalHistory, Integer> {

}
