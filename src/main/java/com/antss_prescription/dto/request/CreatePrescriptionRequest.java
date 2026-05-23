package com.antss_prescription.dto.request;

import com.antss_prescription.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePrescriptionRequest {

    @NotBlank
    private String patientName;

    private Integer patientAge;

    @NotNull
    private Gender patientGender;

    private String patientMobile;

    private String diagnosis;
    private String symptoms;
    
    @NotBlank
    private String medicines;

    private String advice;
    private LocalDate followUpDate;

    @NotNull
    private UUID doctorId;

    private Long hospitalId;
    private Long clinicId;
}
