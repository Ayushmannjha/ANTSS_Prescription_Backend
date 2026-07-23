package com.antss_prescription.entity.prescription;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.enums.DiscountPolicy;
import com.antss_prescription.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_bill")
@Getter
@Setter
public class ConsultationBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billId;

    @Column(nullable = false, unique = true, length = 40)
    private String billNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private PatientRegistration patientRegistration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountPolicy discountPolicy = DiscountPolicy.NONE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

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
