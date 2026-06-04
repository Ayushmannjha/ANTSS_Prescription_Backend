package com.antss_prescription.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateDoctorRequest {

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

    //@NotBlank
    private String registrationNumber;

    private String signatureUrl;

    private Long hospitalId;
    private Long clinicId;
}
