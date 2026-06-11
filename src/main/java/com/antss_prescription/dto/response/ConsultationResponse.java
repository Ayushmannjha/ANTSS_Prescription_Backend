package com.antss_prescription.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
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

    // Patient Registration
    private int registrationId;
    private String registrationNumber;

    // Patient
    private int patientId;
    private String patientName;
    private String mobileNumber;
    private String gender;
    private int age;

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

    // Consultation
    private String advice;
    private LocalDateTime followUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}