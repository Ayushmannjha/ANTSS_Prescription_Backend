package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import java.util.List;
import java.util.Optional;


public interface PrescriptionRepo extends JpaRepository<Prescription, Integer>{
	List<Prescription> findByConsultation_Patient_PatientId(int consultation_Patient_PatientId);

	List<Prescription> findByConsultation_PatientRegistration_RegistrationId(int consultation_PatientRegistration_RegistrationId);;
}
