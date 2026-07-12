package com.antss_prescription.entity.prescription;

import com.antss_prescription.entity.Doctor;

/** Common nullable ownership fields for clinical records. */
public interface ClinicalAttribution {
    Doctor getDoctorReference();
    void setDoctorReference(Doctor doctorReference);
    String getEntityType();
    void setEntityType(String entityType);
    Long getEntityId();
    void setEntityId(Long entityId);
}
