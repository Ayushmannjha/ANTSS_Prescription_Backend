package com.antss_prescription.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.antss_prescription.enums.DiscountPolicy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationRequest {
    @NotNull
    private UUID doctorId;
    @NotNull
    private Integer registrationId;
    @Size(max = 100)
    private String consultationNumber;
    @Size(max = 2000)
    private String advice;
    @FutureOrPresent
    private LocalDateTime followUpDate;
    @Valid
    private List<ComplaintRequest> complaints;
    @Valid
    private List<ExaminationRequest> examinations;
    @Valid
    private List<DiagnosisRequest> diagnoses;
    @Valid
    private List<HistoryRequest> medicalHistories;
    @Valid
    private VitalsRequest vitals;
    private DiscountPolicy discountPolicy = DiscountPolicy.NONE;
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Data
    public static class ComplaintRequest {
        @Size(max = 255) private String complaintName;
        @Size(max = 100) private String frequency;
        @Size(max = 100) private String sev;
        @Size(max = 100) private String duration;
    }

    @Data
    public static class ExaminationRequest {
        @Size(max = 500) private String generalExamination;
    }

    @Data
    public static class DiagnosisRequest {
        @Size(max = 255) private String diagnosisName;
        @Size(max = 100) private String diagnosisCode;
        @Size(max = 100) private String duration;
    }

    @Data
    public static class HistoryRequest {
        @Size(max = 500) private String allergeies;
        @Size(max = 500) private String currentMedicine;
        @Size(max = 1000) private String medicalHistory;
    }

    @Data
    public static class VitalsRequest {
        private int height;
        private double weight;
        private double temprature;
        private double pulse;
        private double spo2;
        @Size(max = 20) private String bp;
        private double respiratoryRate;
    }
}
