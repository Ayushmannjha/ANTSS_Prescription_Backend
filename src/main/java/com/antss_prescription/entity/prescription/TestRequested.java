package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;
import com.antss_prescription.entity.Doctor;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
public class TestRequested implements ClinicalAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String testName;
    private String notes;

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

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = true)
    private Doctor doctorReference;
    @Column(name = "entity_type", nullable = true, length = 20)
    private String entityType;
    @Column(name = "entity_id", nullable = true)
    private Long entityId;
}
