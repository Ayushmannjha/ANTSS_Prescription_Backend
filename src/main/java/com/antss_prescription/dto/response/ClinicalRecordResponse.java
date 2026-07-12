package com.antss_prescription.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClinicalRecordResponse {
    private Integer id;
    private UUID doctorId;
    private String entityType;
    private Long entityId;
    private String diagnosisName;
    private String diagnosisCode;
    private String duration;
    private String complaintName;
    private String frequency;
    private String severity;
    private String generalExamination;
    private String allergies;
    private String currentMedicine;
    private String medicalHistory;
    private String investigationName;
    private String testName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
