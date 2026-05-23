package com.antss_prescription.dto.response;

import com.antss_prescription.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionResponse {
    private Long id;
    private String prescriptionNumber;
    private String patientName;
    private Integer patientAge;
    private Gender patientGender;
    private String patientMobile;
    private String diagnosis;
    private String symptoms;
    private String medicines;
    private String advice;
    private LocalDate followUpDate;
    private UUID doctorId;
    private String doctorName;
    private Long hospitalId;
    private Long clinicId;
    private LocalDateTime createdAt;
}
