package com.antss_prescription.service;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.prescription.ClinicalAttribution;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ClinicalAttributionService {

    public void apply(ClinicalAttribution target, Doctor doctor, PatientRegistration registration) {
        target.setDoctorReference(doctor);

        if (registration == null) {
            target.setEntityType(null);
            target.setEntityId(null);
            return;
        }
        if (registration.getClinic() != null && registration.getHospital() == null) {
            target.setEntityType("CLINIC");
            target.setEntityId(registration.getClinic().getId());
            return;
        }
        if (registration.getHospital() != null && registration.getClinic() == null) {
            target.setEntityType("HOSPITAL");
            target.setEntityId(registration.getHospital().getId());
            return;
        }
        throw new BusinessException("Clinical record must belong to exactly one clinic or hospital");
    }
}
