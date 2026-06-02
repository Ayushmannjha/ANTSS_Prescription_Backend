package com.antss_prescription.dto.request;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
 
@Data
@Builder
public class UserSubscriptionSummaryDto {
 
    // ── User info ──────────────────────────────────────────────────────────
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userRole;
 
    // ── Active subscription ────────────────────────────────────────────────
    private UUID subscriptionId;
    private String packageName;
    private String durationType;            // MONTHLY / YEARLY
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private long daysRemaining;             // negative = expired
    private boolean subscriptionExpired;
    private SubscriptionStatus subscriptionStatus;
    private PaymentStatus paymentStatus;
 
    // ── Doctor quota ───────────────────────────────────────────────────────
    private int baseDoctorLimit;            // from package
    private int allowedDoctors;            // base + approved addons
    private int usedDoctors;
    private int availableDoctorSlots;       // allowedDoctors - usedDoctors
 
    // ── Facility quota ─────────────────────────────────────────────────────
    private int allowedHospitals;
    private int allowedClinics;
 
    // ── Pricing ────────────────────────────────────────────────────────────
    private BigDecimal basePackagePrice;
    private BigDecimal extraDoctorPrice;    // per doctor per year
 
    // ── Package features ───────────────────────────────────────────────────
    private String packageFeatures;
 
    // ── Addons ────────────────────────────────────────────────────────────
    private List<DoctorAddonDto> addons;
    private int totalApprovedAddonDoctors;
    private BigDecimal totalAddonCost;
 
    // ── Doctor allocations ─────────────────────────────────────────────────
    private List<DoctorAllocationDto> allocatedDoctors;
 
    // ── Hospital / Clinic attached to this user ────────────────────────────
    private List<FacilityDto> hospitals;
    private List<FacilityDto> clinics;
 
    // ── Nested: addon detail ───────────────────────────────────────────────
    @Data
    @Builder
    public static class DoctorAddonDto {
        private Long addonId;
        private int additionalDoctors;
        private BigDecimal prorataAmount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String approvalStatus;  // PENDING / APPROVED / REJECTED
        private String paymentStatus;
        private LocalDateTime approvedAt;
        private String approvedByName;
    }
 
    // ── Nested: allocated doctor detail ───────────────────────────────────
    @Data
    @Builder
    public static class DoctorAllocationDto {
        private UUID doctorId;
        private String doctorName;
        private String specialization;
        private String qualification;
        private String allocationType;  // BASE / ADDON
        private String allocationStatus;
        private LocalDateTime allocatedAt;
    }
 
    // ── Nested: hospital / clinic ──────────────────────────────────────────
    @Data
    @Builder
    public static class FacilityDto {
        private Long facilityId;
        private String facilityName;
        private String facilityCode;
        private String city;
        private String state;
        private String status;
        private int maxDoctorLimit;
        private int activeDoctorCount;
    }
}