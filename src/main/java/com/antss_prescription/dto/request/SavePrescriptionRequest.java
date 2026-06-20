package com.antss_prescription.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavePrescriptionRequest {

    private Integer consultationId;

    // --- Vitals ---
    private int height;
    private double weight;
    private double temperature;
    private double pulse;
    private double spo2;
    private String bp;
    private double respiratoryRate;

    // --- Chief Complaint ---
    private List<ChiefComplaintRequest> complaints;

    // --- General Examination ---
    private List<String> generalExaminations;

    // --- Past Medical History ---
    private List<PastMedicalHistoryRequest> pastMedicalHistories;

    // --- Diagnosis ---
    private List<DiagnosisRequest> diagnoses;

    // --- Investigation ---
    private List<InvestigationRequest> investigations;

    // --- Test Requested ---
    private List<TestRequestedRequest> testRequested;

    // --- Consultation ---
    private int registrationId;
    private String advice;
    private LocalDateTime followUpDate;

    // --- Prescription ---
    private String notes;

    // --- Medicines ---
    private List<MedicineRequest> medicines;

    // =========================
    // Chief Complaint
    // =========================

    @Data
    public static class ChiefComplaintRequest {
        private String complaintName;
        private String complaintFrequency;
        private String severity;
        private String complaintDuration;
    }

    // =========================
    // Past Medical History
    // =========================

    @Data
    public static class PastMedicalHistoryRequest {
        private String allergies;
        private String currentMedicine;
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
        private String diagnosisName;
        private String diagnosisCode;
        private String diagnosisDuration;
    }

    // =========================
    // Investigation
    // =========================

    @Data
    public static class InvestigationRequest {
        private String investigationName;
        private String notes;
        private String documentUrl;
        private String documentFileName;
    }

    // =========================
    // Test Requested
    // =========================

    @Data
    public static class TestRequestedRequest {
        private String testName;
        private String notes;
    }

    // =========================
    // Medicine
    // =========================

    @Data
    public static class MedicineRequest {
        private String medicineName;
        private String strength;
        private String dosage;
        private String frequency;
        private String duration;
        private String instruction;
        private String quantity;
    }
    @Data
    public static class DocumentRequest {
        private String fileName;
        private String url;
    }

    // And the field
    private List<DocumentRequest> documents;
}