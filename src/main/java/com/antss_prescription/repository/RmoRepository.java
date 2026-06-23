package com.antss_prescription.repository;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.Rmo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RmoRepository extends JpaRepository<Rmo, UUID> {
    Optional<Rmo> findByEmployeeCode(String employeeCode);
    Optional<Rmo> findByUserId(UUID userId);
    List<Rmo> findByHospital(Hospital hospital);
    List<Rmo> findByClinic(Clinic clinic);
}
