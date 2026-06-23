package com.antss_prescription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdatePrescriptionRequest {

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
    @Valid
    private List<SavePrescriptionRequest.ChiefComplaintRequest> complaints;

    // --- General Examination ---
    private List<@NotBlank @Size(max = 255) String> generalExaminations;

    // --- Past Medical History ---
    @Valid
    private List<SavePrescriptionRequest.PastMedicalHistoryRequest> pastMedicalHistories;

    // --- Diagnosis ---
    @Valid
    private List<SavePrescriptionRequest.DiagnosisRequest> diagnoses;

    // --- Diagnostic orders ---
    @Valid
    private List<SavePrescriptionRequest.DiagnosticRequest> diagnostics;

    // --- Documents ---
    @Valid
    private List<SavePrescriptionRequest.DocumentRequest> documents;

    // --- Consultation ---
    @Size(max = 2000)
    private String advice;
    @FutureOrPresent
    private LocalDateTime followUpDate;

    // --- Prescription ---
    @Size(max = 2000)
    private String notes;

    // --- Medicines ---
    @Valid
    private List<SavePrescriptionRequest.MedicineRequest> medicines;
}
