package com.antss_prescription.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrescriptionResponse {
    private int prescriptionId;
    private int consultationId;
    private String consultationNumber;
    private String patientName;
    private String notes;
    private LocalDateTime createdAt;
    private List<String> medicines;
}