package com.antss_prescription.entity;

import com.antss_prescription.enums.DurationType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_packages")
@Data
public class SubscriptionPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DurationType durationType;

    @Column(nullable = false)
    private Integer baseDoctorLimit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal packagePrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraDoctorPrice;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false)
    private boolean isActive;

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
