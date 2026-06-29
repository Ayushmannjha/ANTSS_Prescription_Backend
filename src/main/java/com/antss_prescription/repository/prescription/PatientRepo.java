package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antss_prescription.entity.prescription.Patient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepo extends JpaRepository<Patient, Integer>{

    Optional<Patient> findByMobileNumber(String mobileNumber);

    @Query("""
            SELECT DISTINCT r.patient FROM PatientRegistration r
            WHERE r.hospital.id IN :hospitalIds OR r.clinic.id IN :clinicIds
            """)
    List<Patient> findAccessible(@Param("hospitalIds") List<Long> hospitalIds,
            @Param("clinicIds") List<Long> clinicIds);

}
