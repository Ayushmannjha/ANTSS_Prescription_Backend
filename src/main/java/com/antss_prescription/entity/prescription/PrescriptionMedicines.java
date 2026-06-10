package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Entity
@Table(name = "prescription_medicines")
@Setter @Getter @ToString
public class PrescriptionMedicines {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int prescriptionMedicineId;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    private String medicineName;
    private String strength;
    private String dosage;
    private String frequency;
    private String duration;
    private String instruction;
    private String quantity;
    private LocalDateTime createdAt;
}