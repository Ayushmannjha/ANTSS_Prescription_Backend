package com.antss_prescription.dto.response;

import com.antss_prescription.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorResponse {
    private UUID id;
    private String doctorName;
    private String doctorCode;
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private String email;
    private String mobileNumber;
    private String registrationNumber;
    private String signatureUrl;
    private Long hospitalId;
    private Long clinicId;
    private EntityStatus status;
}
