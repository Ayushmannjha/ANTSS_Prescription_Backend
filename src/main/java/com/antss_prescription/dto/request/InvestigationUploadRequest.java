package com.antss_prescription.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvestigationUploadRequest {
    @NotBlank
    @Size(max = 255)
    private String investigationName;

    @Size(max = 1000)
    private String notes;

    @Positive
    private Integer registrationId;

    @Positive
    private Integer prescriptionId;

    @AssertTrue(message = "Registration or prescription is required")
    public boolean isClinicalContextPresent() {
        return registrationId != null || prescriptionId != null;
    }
}
