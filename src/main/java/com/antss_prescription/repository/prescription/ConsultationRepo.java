package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Consultation;
import java.util.List;
import java.util.UUID;

import com.antss_prescription.entity.Doctor;

@Repository
public interface ConsultationRepo extends JpaRepository<Consultation, Integer> {
    List<Consultation> findByDoctor(Doctor doctor);

    List<Consultation> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
}
