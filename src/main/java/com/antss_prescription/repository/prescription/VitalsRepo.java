package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import com.antss_prescription.entity.prescription.Vitals;
import java.util.List;

public interface VitalsRepo extends JpaRepository<Vitals, Integer>{
    List<Vitals> findByPatientRegistrationRegistrationIdOrderByCreatedAtDesc(Integer registrationId);
}
