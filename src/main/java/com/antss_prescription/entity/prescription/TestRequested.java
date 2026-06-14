package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class TestRequested {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String testName;

    @ManyToOne
    @JoinColumn(name = "registration_number")
    private PatientRegistration patientRegistration;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    private LocalDateTime createAt;
    private LocalDateTime updatedAt;
}