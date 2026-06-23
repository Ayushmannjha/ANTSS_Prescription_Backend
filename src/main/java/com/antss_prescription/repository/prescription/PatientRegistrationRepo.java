package com.antss_prescription.repository.prescription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;



public interface PatientRegistrationRepo extends JpaRepository<PatientRegistration, Integer>{

	@Query(value = "SELECT nextval('patient_registration_number_seq')", nativeQuery = true)
	Long nextRegistrationSequenceValue();

	List<PatientRegistration> findByClinic(Clinic clinic);
	List<PatientRegistration> findByHospital(Hospital hospital);
	List<PatientRegistration> findByPatient(Patient patient);
	boolean existsByPatient(Patient patient);
	List<PatientRegistration> findByRegistrationNumberStartingWith(String prefix);
	Optional<PatientRegistration> findByRegistrationNumber(String registrationNumber);
	@Query("""
			SELECT r FROM PatientRegistration r
			WHERE r.hospital.id IN :hospitalIds OR r.clinic.id IN :clinicIds
			""")
	List<PatientRegistration> findAccessible(@Param("hospitalIds") List<Long> hospitalIds,
			@Param("clinicIds") List<Long> clinicIds);
	 boolean existsByPatientAndClinic(Patient patient, Clinic clinic);
	    boolean existsByPatientAndHospital(Patient patient, Hospital hospital);

	    Optional<PatientRegistration> findByPatientAndClinic(Patient patient, Clinic clinic);
	    Optional<PatientRegistration> findByPatientAndHospital(Patient patient, Hospital hospital);
}
