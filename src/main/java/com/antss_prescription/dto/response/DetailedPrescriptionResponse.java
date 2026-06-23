package com.antss_prescription.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.antss_prescription.enums.DiagnosticStatus;

@Data
@Builder
public class DetailedPrescriptionResponse {
    private int prescriptionId;
    private String notes;
    private LocalDateTime createdAt;
    private ConsultationResponse consultation;
    private List<MedicineDetailResponse> medicines;
    @Data
    @Builder
    public static class MedicineDetailResponse {
        private int prescriptionMedicineId;
        private String medicineName;
        private String strength;
        private String dosage;
        private String frequency;
        private String duration;
        private String instruction;
        private String quantity;
    }
 // In DetailedPrescriptionResponse.java — add these fields
    private List<DiagnosticDetailResponse> diagnostics;

    @Data
    @Builder
    public static class DiagnosticDetailResponse {
        private int id;
        private String testName;
        private String notes;
        private String resultSummary;
        private DiagnosticStatus status;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
        private Integer reportDocumentId;
    }
    @Data
    @Builder
    public static class DocumentDetailResponse {
        private int id;
        private String fileName;
        private String url;
    }

    // And the field
    private List<DocumentDetailResponse> documents;
}
