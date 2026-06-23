package com.antss_prescription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRegistrationRequest {
    @Positive
    private Integer patientId;
    @Valid
    private PatientRequest patient;
    @Positive
    private Long clinicId;
    @Positive
    private Long hospitalId;
    @Size(max = 50)
    private String status;

    @AssertTrue(message = "Provide either patientId or patient details")
    public boolean isPatientSelectionValid() {
        return (patientId == null) != (patient == null);
    }

    @AssertTrue(message = "Exactly one hospital or clinic is required")
    public boolean isFacilitySelectionValid() {
        return (hospitalId == null) != (clinicId == null);
    }
}
