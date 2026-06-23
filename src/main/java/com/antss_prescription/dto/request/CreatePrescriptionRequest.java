package com.antss_prescription.dto.request;

import com.antss_prescription.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePrescriptionRequest {

    @NotBlank
    @Size(max = 100)
    private String patientName;

    @Min(0)
    @Max(150)
    private Integer patientAge;

    @NotNull
    private Gender patientGender;

    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String patientMobile;

    @Size(max = 1000)
    private String diagnosis;
    @Size(max = 2000)
    private String symptoms;
    
    @NotBlank
    private String medicines;

    @Size(max = 2000)
    private String advice;
    @FutureOrPresent
    private LocalDate followUpDate;

    @NotNull
    private UUID doctorId;

    @Positive
    private Long hospitalId;
    @Positive
    private Long clinicId;
}
