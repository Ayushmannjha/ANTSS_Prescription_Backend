package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;



public interface PatientRegistrationRepo extends JpaRepository<PatientRegistration, Integer>{

	List<PatientRegistration> findByClinic(Clinic clinic);
	List<PatientRegistration> findByHospital(Hospital hospital);
	List<PatientRegistration> findByPatient(Patient patient);
	List<PatientRegistration> findByRegistrationNumberStartingWith(String prefix);
	 boolean existsByPatientAndClinic(Patient patient, Clinic clinic);
	    boolean existsByPatientAndHospital(Patient patient, Hospital hospital);

	    Optional<PatientRegistration> findByPatientAndClinic(Patient patient, Clinic clinic);
	    Optional<PatientRegistration> findByPatientAndHospital(Patient patient, Hospital hospital);
}
