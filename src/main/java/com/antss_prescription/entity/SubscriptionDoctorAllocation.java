package com.antss_prescription.entity;

import com.antss_prescription.enums.AllocationStatus;
import com.antss_prescription.enums.AllocationType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_doctor_allocations")
@Data
public class SubscriptionDoctorAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private UserSubscription userSubscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationType allocationType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime allocatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        allocatedAt = LocalDateTime.now();
    }
}
