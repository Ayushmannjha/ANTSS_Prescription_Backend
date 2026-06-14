package com.antss_prescription.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdatePrescriptionRequest {

    // --- Vitals ---
    private int height;
    private double weight;
    private double temperature;
    private double pulse;
    private double spo2;
    private String bp;
    private double respiratoryRate;

    // --- Chief Complaint ---
    private List<SavePrescriptionRequest.ChiefComplaintRequest> complaints;

    // --- General Examination ---
    private List<String> generalExaminations;

    // --- Past Medical History ---
    private List<SavePrescriptionRequest.PastMedicalHistoryRequest> pastMedicalHistories;

    // --- Diagnosis ---
    private List<SavePrescriptionRequest.DiagnosisRequest> diagnoses;

    // --- Investigation ---
    private List<SavePrescriptionRequest.InvestigationRequest> investigations;

    // --- Test Requested ---
    private List<SavePrescriptionRequest.TestRequestedRequest> testRequested;

    // --- Documents ---
    private List<SavePrescriptionRequest.DocumentRequest> documents;

    // --- Consultation ---
    private String advice;
    private LocalDateTime followUpDate;

    // --- Prescription ---
    private String notes;

    // --- Medicines ---
    private List<SavePrescriptionRequest.MedicineRequest> medicines;
}