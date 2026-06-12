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
}