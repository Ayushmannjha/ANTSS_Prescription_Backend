package com.antss_prescription.entity;

import java.time.LocalDateTime;

import com.antss_prescription.enums.EntityStatus;

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
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString(exclude = {"user"})
public class Hospital {

    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false, unique = true)
    private String hospitalCode;

    private String registrationNumber;

    private String addressLine1;

    private String city;

    private String state;

    private String pincode;

    private String email;

    private String mobileNumber;

    @Column(nullable = false)
    private Integer maxDoctorLimit = 0;

    @Column(nullable = false)
    private Integer activeDoctorCount = 0;

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
