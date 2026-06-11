package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import com.antss_prescription.entity.Doctor;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consultation")
@Setter @Getter @ToString
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int consultationId;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    
    private String consultationNumber;

    @ManyToOne
    @JoinColumn(name = "registration_id")
    private PatientRegistration patientRegistration;

    @ManyToOne
    @JoinColumn(name = "cheif_complaint_id")
    private CheifComplaints cheifComplaints;

    @ManyToOne
    @JoinColumn(name = "general_examination_id")
    private GeneralExamination generalExamination;

    @ManyToOne
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @ManyToOne
    @JoinColumn(name = "past_medical_history_id")
    private PastMedicalHistory pastMedicalHistory;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "vitals_id")
    private Vitals vitals;

    
    private String advice;
    private LocalDateTime followUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}