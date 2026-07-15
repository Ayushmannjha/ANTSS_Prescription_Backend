package com.antss_prescription.dto.response;

import java.util.UUID;

import com.antss_prescription.enums.EntityStatus;

import lombok.Data;

@Data
public class DoctorOptionResponseDto {
    private UUID doctorId;
    private String doctorName;
    private String doctorCode;
    private String specialization;
    private String qualification;
    private String mobileNumber;
    private EntityStatus status;
}
