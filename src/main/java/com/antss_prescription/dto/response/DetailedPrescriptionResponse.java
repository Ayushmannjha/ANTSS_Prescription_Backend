package com.antss_prescription.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<InvestigationDetailResponse> investigations;
    private List<TestRequestedDetailResponse> testRequested;

    @Data
    @Builder
    public static class InvestigationDetailResponse {
        private int id;
        private String investigationName;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class TestRequestedDetailResponse {
        private int id;
        private String testName;
        private String notes;
        private LocalDateTime createdAt;
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