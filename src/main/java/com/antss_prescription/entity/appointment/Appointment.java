package com.antss_prescription.entity.appointment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointment")
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @Column(nullable = false)
    private Long hospitalId;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false, length = 150)
    private String patientName;

    @Column(nullable = false, length = 15)
    private String mobileNumber;

    private Integer age;

    private String gender;

    private LocalDate appointmentDate;

    private LocalTime appointmentTime;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private String status = "REQUESTED";
    // REQUESTED, CONFIRMED, REJECTED, CANCELLED, COMPLETED, NO_SHOW

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null || status.isBlank()) {
            status = "REQUESTED";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}