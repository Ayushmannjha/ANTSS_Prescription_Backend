package com.antss_prescription.repository;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByDoctorCode(String doctorCode);
    List<Doctor> findByHospital(Hospital hospital);
    List<Doctor> findByClinic(Clinic clinic);
    List<Doctor> findByHospitalId(Long hospitalId);
    List<Doctor> findByClinicId(Long clinicId);
    Optional<Doctor> findByUserId(UUID userId);
}
