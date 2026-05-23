package com.antss_prescription.dto.request;

import com.antss_prescription.enums.EntityStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateDoctorRequest {

    @NotBlank
    private String doctorName;

    @NotBlank
    private String specialization;

    @NotBlank
    private String qualification;

    @NotNull
    @Min(0)
    private Integer experienceYears;

    @Email
    private String email;

    private String mobileNumber;

    @NotBlank
    private String registrationNumber;

    private String signatureUrl;

    @NotNull
    private EntityStatus status;
}
