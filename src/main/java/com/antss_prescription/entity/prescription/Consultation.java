package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Rmo;
import com.antss_prescription.enums.ConsultationPriority;
import com.antss_prescription.enums.ConsultationStatus;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "consultation")
@Setter
@Getter
@ToString
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int consultationId;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    @NotNull
    private Doctor doctor;
    
    @Size(max = 100)
    private String consultationNumber;

    @ManyToOne
    @JoinColumn(name = "registration_id")
    @NotNull
    private PatientRegistration patientRegistration;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Valid
    private List<CheifComplaints> cheifComplaints = new ArrayList<>();

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneralExamination> generalExaminations = new ArrayList<>();

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diagnosis> diagnoses = new ArrayList<>();

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PastMedicalHistory> pastMedicalHistories = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "vitals_id")
    private Vitals vitals;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ConsultationStatus status = ConsultationStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ConsultationPriority priority;

    @Size(max = 1000)
    @Column(nullable = true, length = 1000)
    private String consultReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_rmo_id", nullable = true)
    private Rmo requestedBy;

    @Column(nullable = true)
    private LocalDateTime requestedAt;

    @Column(nullable = true)
    private LocalDateTime acceptedAt;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    @Column(nullable = true)
    private LocalDateTime cancelledAt;

    
    @Size(max = 2000)
    private String advice;
    @FutureOrPresent
    private LocalDateTime followUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
