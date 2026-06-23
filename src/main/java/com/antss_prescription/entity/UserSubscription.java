package com.antss_prescription.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.*;

@Entity
@Data
@Builder
@ToString(exclude = {"user"})
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription {

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "active_owner_key", unique = true, length = 36)
    private String activeOwnerKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false)
    private SubscriptionPackage subscriptionPackage;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer allowedDoctors;

    @Column(nullable = false)
    private Integer usedDoctors = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_by")
    private User suspendedBy;

    private LocalDateTime suspendedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactivated_by")
    private User reactivatedBy;

    private LocalDateTime reactivatedAt;

    @Column(length = 255)
    private String paymentTransactionRef;

    @Column(length = 500)
    private String paymentFailureReason;

    @Column(nullable = false)
    private Integer allowedHospitals = 1;

    @Column(nullable = false)
    private Integer allowedClinics = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        normalizePaymentState();
        syncActiveOwnerKey();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizePaymentState();
        syncActiveOwnerKey();
        updatedAt = LocalDateTime.now();
    }

    private void syncActiveOwnerKey() {
        activeOwnerKey = user != null && user.getId() != null
                && (subscriptionStatus == SubscriptionStatus.ACTIVE
                    || subscriptionStatus == SubscriptionStatus.PENDING)
                ? user.getId().toString()
                : null;
    }

    private void normalizePaymentState() {
        if (paymentStatus == PaymentStatus.PENDING
                && subscriptionStatus == SubscriptionStatus.ACTIVE) {
            subscriptionStatus = SubscriptionStatus.PENDING;
        }
    }
}
