package com.antss_prescription.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.antss_prescription.enums.ConsultationPriority;
import com.antss_prescription.enums.ConsultationStatus;

import lombok.Data;

@Data
public class ConsultationResponse {

    private int consultationId;
    private String consultationNumber;

    // Doctor
    private UUID doctorId;
    private String doctorName;
    private String doctorCode;
    private String specialization;
    private String qualification;
    private String doctorRegistrationNo;
    private String doctorSignatureUrl;

    // Clinic
    private Long clinicId;
    private String clinicName;
    private String clinicAddress;
    private String clinicPhone;
    
    // Hospital
    private Long hospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private String hospitalPhone;

    // Patient Registration
    private int registrationId;
    private String registrationNumber;

    // Patient
    private int patientId;
    private String patientName;
    private String mobileNumber;
    private String gender;
    private int age;
    private String patientAddress;

    // Chief Complaint
    private int cheifComplaintId;
    private String complaintName;
    private String complaintFrequency;
    private String severity;
    private String complaintDuration;

    // General Examination
    private int generalExaminationId;
    private String generalExamination;

    // Diagnosis
    private int diagnosisId;
    private String diagnosisName;
    private String diagnosisCode;
    private String diagnosisDuration;

    // Past Medical History
    private int historyId;
    private String allergies;
    private String currentMedicine;
    private String medicalHistory;

    // Vitals
    private int vitalId;
    private int height;
    private double weight;
    private double temperature;
    private double pulse;
    private double spo2;
    private String bp;
    private double respiratoryRate;

    // Request workflow
    private ConsultationStatus status;
    private ConsultationPriority priority;
    private String consultReason;
    private UUID requestedByRmoId;
    private String requestedByRmoName;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // Consultation
    private String advice;
    private LocalDateTime followUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ConsultationBillResponse bill;

    // Lists for array representations
    private java.util.List<ChiefComplaintResponse> complaints;
    private java.util.List<String> generalExaminations;
    private java.util.List<DiagnosisResponse> diagnoses;
    private java.util.List<PastMedicalHistoryResponse> pastMedicalHistories;

    @lombok.Data
    public static class ChiefComplaintResponse {
        private int cheifComplaintId;
        private String complaintName;
        private String complaintFrequency;
        private String severity;
        private String complaintDuration;
    }

    @lombok.Data
    public static class DiagnosisResponse {
        private int diagnosisId;
        private String diagnosisName;
        private String diagnosisCode;
        private String diagnosisDuration;
    }

    @lombok.Data
    public static class PastMedicalHistoryResponse {
        private int historyId;
        private String allergies;
        private String currentMedicine;
        private String medicalHistory;
    }
}
