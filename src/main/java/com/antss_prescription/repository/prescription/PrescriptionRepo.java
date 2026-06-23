package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.Consultation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PrescriptionRepo extends JpaRepository<Prescription, Integer>{
	List<Prescription> findByConsultation_Patient_PatientId(int consultation_Patient_PatientId);

	List<Prescription> findByConsultation_PatientRegistration_RegistrationId(int consultation_PatientRegistration_RegistrationId);;
	boolean existsByConsultation(Consultation consultation);

	@Query("""
			SELECT p FROM Prescription p JOIN p.consultation c JOIN c.patientRegistration r
			WHERE r.hospital.id IN :hospitalIds OR r.clinic.id IN :clinicIds
			""")
	List<Prescription> findAccessible(@Param("hospitalIds") List<Long> hospitalIds,
			@Param("clinicIds") List<Long> clinicIds);
}
