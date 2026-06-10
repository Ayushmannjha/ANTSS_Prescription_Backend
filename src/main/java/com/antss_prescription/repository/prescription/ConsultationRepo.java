package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.Consultation;

public interface ConsultationRepo extends JpaRepository<Consultation, Integer>{

}
