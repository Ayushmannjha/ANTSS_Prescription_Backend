package com.antss_prescription.entity;

import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.FacilityType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class DoctorAddon {

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_subscription_id", nullable = false)
    private UserSubscription userSubscription;

    private Long facilityId;

    @Enumerated(EnumType.STRING)
    private FacilityType facilityType;

    @Column(nullable = false)
    private Integer additionalDoctors;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal yearlyPricePerDoctor;

    @Column(nullable = false)
    private Integer remainingMonths;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prorataAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddonApprovalStatus approvalStatus = AddonApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    private LocalDateTime rejectedAt;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 255)
    private String paymentTransactionRef;

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
