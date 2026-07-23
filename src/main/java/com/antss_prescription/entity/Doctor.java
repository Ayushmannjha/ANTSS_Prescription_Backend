package com.antss_prescription.entity;

import com.antss_prescription.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Check;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Check(constraints = "(hospital_id IS NOT NULL AND clinic_id IS NULL) OR (hospital_id IS NULL AND clinic_id IS NOT NULL)")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false, unique = true)
    private String doctorCode;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private String qualification;

    private Integer experienceYears;

    @Column(unique = true)
    private String email;

    @Column(nullable = true, unique = true)
    private String hfrId;

    @Column(nullable = false, unique = true)
    private String mobileNumber;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = true, unique = false)
    private String signatureUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityStatus status = EntityStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
