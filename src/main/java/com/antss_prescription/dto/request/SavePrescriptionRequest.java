package com.antss_prescription.dto.request;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavePrescriptionRequest {

    // --- Vitals ---
    private int height;
    private double weight;
    private double temperature;
    private double pulse;
    private double spo2;
    private String bp;
    private double respiratoryRate;

    // --- Chief Complaint ---
    private String complaintName;
    private String complaintFrequency;
    private String severity;
    private String complaintDuration;

    // --- General Examination ---
    private String generalExamination;

    // --- Past Medical History ---
    private String allergies;
    private String currentMedicine;
    private String medicalHistory;

    // --- Diagnosis ---
    private String diagnosisName;
    private String diagnosisCode;
    private String diagnosisDuration;

    // --- Consultation ---
    private int registrationId;      // links to existing PatientRegistration
    private String advice;
    private LocalDateTime followUpDate;

    // --- Prescription ---
    private String notes;

    // --- Medicines ---
    private List<MedicineRequest> medicines;

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
}