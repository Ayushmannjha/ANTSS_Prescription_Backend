package com.antss_prescription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavePrescriptionRequest {

    @Positive
    private Integer consultationId;

    // --- Vitals ---
    @PositiveOrZero
    private int height;
    @PositiveOrZero
    private double weight;
    @PositiveOrZero
    private double temperature;
    @PositiveOrZero
    private double pulse;
    @PositiveOrZero
    private double spo2;
    @Size(max = 20)
    private String bp;
    @PositiveOrZero
    private double respiratoryRate;

    // --- Chief Complaint ---
    private List<@Valid ChiefComplaintRequest> complaints;

    // --- General Examination ---
    private List<@NotBlank @Size(max = 255) String> generalExaminations;

    // --- Past Medical History ---
    private List<@Valid PastMedicalHistoryRequest> pastMedicalHistories;

    // --- Diagnosis ---
    private List<@Valid DiagnosisRequest> diagnoses;

    // --- Diagnostic orders ---
    private List<@Valid DiagnosticRequest> diagnostics;

    // --- Consultation ---
    @Positive
    private int registrationId;
    @Size(max = 2000)
    private String advice;
    @FutureOrPresent
    private LocalDateTime followUpDate;

    // --- Prescription ---
    @Size(max = 2000)
    private String notes;

    // --- Medicines ---
    private List<@Valid MedicineRequest> medicines;

    // =========================
    // Chief Complaint
    // =========================

    @Data
    public static class ChiefComplaintRequest {
        @NotBlank
        @Size(max = 255)
        private String complaintName;
        @Size(max = 100)
        private String complaintFrequency;
        @Size(max = 100)
        private String severity;
        @Size(max = 100)
        private String complaintDuration;
    }

    // =========================
    // Past Medical History
    // =========================

    @Data
    public static class PastMedicalHistoryRequest {
        @Size(max = 1000)
        private String allergies;
        @Size(max = 1000)
        private String currentMedicine;
        @Size(max = 2000)
        private String medicalHistory;
    }

    // =========================
    // General Examination
    // =========================

    // using List<String> generalExaminations above
    // if you need structured fields use this instead:

    // @Data
    // public static class GeneralExaminationRequest {
    //     private String finding;
    //     private String notes;
    // }

    // =========================
    // Diagnosis
    // =========================

    @Data
    public static class DiagnosisRequest {
        @NotBlank
        @Size(max = 255)
        private String diagnosisName;
        @Size(max = 100)
        private String diagnosisCode;
        @Size(max = 100)
        private String diagnosisDuration;
    }

    // =========================
    // Diagnostic order
    // =========================

    @Data
    public static class DiagnosticRequest {
        @NotBlank
        @Size(max = 255)
        private String testName;
        @Size(max = 1000)
        private String notes;
    }

    // =========================
    // Medicine
    // =========================

    @Data
    public static class MedicineRequest {
        @NotBlank
        @Size(max = 255)
        private String medicineName;
        @Size(max = 100)
        private String strength;
        @Size(max = 100)
        private String dosage;
        @Size(max = 100)
        private String frequency;
        @Size(max = 100)
        private String duration;
        @Size(max = 500)
        private String instruction;
        @Size(max = 50)
        private String quantity;
    }
    @Data
    public static class DocumentRequest {
        @NotBlank
        @Size(max = 255)
        private String fileName;
        @NotBlank
        @Size(max = 2048)
        private String url;
    }

    // And the field
    private List<@Valid DocumentRequest> documents;
}
