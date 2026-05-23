package com.antss_prescription.repository;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);
    List<Prescription> findByDoctor(Doctor doctor);
    List<Prescription> findByDoctorId(UUID doctorId);
    List<Prescription> findByHospital(Hospital hospital);
    List<Prescription> findByHospitalId(Long hospitalId);
    List<Prescription> findByClinic(Clinic clinic);
    List<Prescription> findByClinicId(Long clinicId);
}
