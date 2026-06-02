package com.antss_prescription.service.impl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antss_prescription.dto.request.UserSubscriptionSummaryDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAddonDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAllocationDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.FacilityDto;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.SubscriptionDoctorAllocation;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorAddonRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.SubscriptionDoctorAllocationRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSubscriptionService implements com.antss_prescription.service.UserSubscriptionService{
 
    private final UserRepository                       userRepository;
    private final UserSubscriptionRepository           subscriptionRepository;
    private final DoctorAddonRepository                addonRepository;
    private final SubscriptionDoctorAllocationRepository allocationRepository;
    private final HospitalRepository                   hospitalRepository;
    private final ClinicRepository                     clinicRepository;
 
    // ──────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────────
 
    /**
     * Returns a complete snapshot of what the user currently has:
     * package, quota, add-ons, allocated doctors, and attached facilities.
     *
     * @param userId UUID of the user
     * @return full summary dto
     * @throws UserNotFoundException if no user with that id exists
     * @throws NoActiveSubscriptionException if the user has no active subscription
     */
    public UserSubscriptionSummaryDto getUserSubscriptionSummary(UUID userId) {
 
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
 
        UserSubscription subscription = subscriptionRepository
                .findActiveByUserId(userId)
                .orElseThrow(() -> new NoActiveSubscriptionException(userId));
 
        SubscriptionPackage pkg = subscription.getSubscriptionPackage();
 
        // ── Addons ──────────────────────────────────────────────────────
        List<DoctorAddon> allAddons    = addonRepository.findByUserSubscriptionId(subscription.getId());
        List<DoctorAddon> activeAddons = allAddons.stream()
                .filter(a -> a.getApprovalStatus() == AddonApprovalStatus.APPROVED
                          && a.getPaymentStatus()  == PaymentStatus.PAID
                          && !a.getEndDate().isBefore(LocalDate.now()))
                .toList();
 
        int totalApprovedAddonDoctors = activeAddons.stream()
                .mapToInt(DoctorAddon::getAdditionalDoctors)
                .sum();
 
        BigDecimal totalAddonCost = activeAddons.stream()
                .map(DoctorAddon::getProrataAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
 
        // ── Effective doctor ceiling ─────────────────────────────────────
        // The subscription's allowedDoctors field is the ground-truth stored
        // value; we also expose it derived (base + addons) for transparency.
        int effectiveAllowed = pkg.getBaseDoctorLimit() + totalApprovedAddonDoctors;
 
        // ── Allocations ──────────────────────────────────────────────────
        List<SubscriptionDoctorAllocation> allocations =
                allocationRepository.findActiveBySubscriptionId(subscription.getId());
 
        // ── Facilities ───────────────────────────────────────────────────
        List<Hospital> hospitals = hospitalRepository.findByOwnerUser(user);
        List<Clinic>   clinics   = clinicRepository.findByOwnerUser(user);
 
        // ── Time calculations ────────────────────────────────────────────
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, subscription.getEndDate());
 
        return UserSubscriptionSummaryDto.builder()
                // user
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .userRole(user.getRole().name())
                // subscription
                .subscriptionId(subscription.getId())
                .packageName(pkg.getPackageName())
                .durationType(pkg.getDurationType().name())
                .subscriptionStartDate(subscription.getStartDate())
                .subscriptionEndDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .subscriptionExpired(daysRemaining < 0)
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .paymentStatus(subscription.getPaymentStatus())
                // doctor quota
                .baseDoctorLimit(pkg.getBaseDoctorLimit())
                .allowedDoctors(effectiveAllowed)
                .usedDoctors(subscription.getUsedDoctors())
                .availableDoctorSlots(effectiveAllowed - subscription.getUsedDoctors())
                // facility quota
                .allowedHospitals(subscription.getAllowedHospitals())
                .allowedClinics(subscription.getAllowedClinics())
                // pricing
                .basePackagePrice(pkg.getPackagePrice())
                .extraDoctorPrice(pkg.getExtraDoctorPrice())
                .packageFeatures(pkg.getFeatures())
                // addons
                .addons(mapAddons(allAddons))
                .totalApprovedAddonDoctors(totalApprovedAddonDoctors)
                .totalAddonCost(totalAddonCost)
                // allocations
                .allocatedDoctors(mapAllocations(allocations))
                // facilities
                .hospitals(mapHospitals(hospitals))
                .clinics(mapClinics(clinics))
                .build();
    }
 
    /**
     * Lightweight check: is this user's subscription currently valid?
     * Useful as a guard before creating prescriptions, adding doctors, etc.
     */
    public boolean hasValidSubscription(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> !sub.getEndDate().isBefore(LocalDate.now())
                         && sub.getPaymentStatus() == PaymentStatus.PAID)
                .orElse(false);
    }
 
    /**
     * How many more doctors can this user add right now?
     * Returns 0 if no active subscription or quota exhausted.
     */
    public int getRemainingDoctorSlots(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> {
                    SubscriptionPackage pkg = sub.getSubscriptionPackage();
                    List<DoctorAddon> activeAddons = addonRepository
                            .findApprovedAndPaidBySubscriptionId(sub.getId());
                    int addonDoctors = activeAddons.stream()
                            .mapToInt(DoctorAddon::getAdditionalDoctors)
                            .sum();
                    int ceiling = pkg.getBaseDoctorLimit() + addonDoctors;
                    return Math.max(0, ceiling - sub.getUsedDoctors());
                })
                .orElse(0);
    }
 
    // ──────────────────────────────────────────────────────────────────────
    // PRIVATE MAPPERS
    // ──────────────────────────────────────────────────────────────────────
 
    private List<DoctorAddonDto> mapAddons(List<DoctorAddon> addons) {
        return addons.stream()
                .map(a -> DoctorAddonDto.builder()
                        .addonId(a.getId())
                        .additionalDoctors(a.getAdditionalDoctors())
                        .prorataAmount(a.getProrataAmount())
                        .startDate(a.getStartDate())
                        .endDate(a.getEndDate())
                        .approvalStatus(a.getApprovalStatus().name())
                        .paymentStatus(a.getPaymentStatus().name())
                        .approvedAt(a.getApprovedAt())
                        .approvedByName(a.getApprovedBy() != null
                                ? a.getApprovedBy().getFullName() : null)
                        .build())
                .collect(Collectors.toList());
    }
 
    private List<DoctorAllocationDto> mapAllocations(
            List<SubscriptionDoctorAllocation> allocations) {
        return allocations.stream()
                .map(sda -> {
                    Doctor d = sda.getDoctor();
                    return DoctorAllocationDto.builder()
                            .doctorId(d.getId())
                            .doctorName(d.getDoctorName())
                            .specialization(d.getSpecialization())
                            .qualification(d.getQualification())
                            .allocationType(sda.getAllocationType().name())
                            .allocationStatus(sda.getStatus().name())
                            .allocatedAt(sda.getAllocatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
 
    private List<FacilityDto> mapHospitals(List<Hospital> hospitals) {
        return hospitals.stream()
                .map(h -> FacilityDto.builder()
                        .facilityId(h.getId())
                        .facilityName(h.getHospitalName())
                        .facilityCode(h.getHospitalCode())
                        .city(h.getCity())
                        .state(h.getState())
                        .status(h.getStatus().name())
                        .maxDoctorLimit(h.getMaxDoctorLimit())
                        .activeDoctorCount(h.getActiveDoctorCount())
                        .build())
                .collect(Collectors.toList());
    }
 
    private List<FacilityDto> mapClinics(List<Clinic> clinics) {
        return clinics.stream()
                .map(c -> FacilityDto.builder()
                        .facilityId(c.getId())
                        .facilityName(c.getClinicName())
                        .facilityCode(c.getClinicCode())
                        .city(c.getCity())
                        .state(c.getState())
                        .status(c.getStatus().name())
                        .maxDoctorLimit(c.getMaxDoctorLimit())
                        .activeDoctorCount(c.getActiveDoctorCount())
                        .build())
                .collect(Collectors.toList());
    }
 
    // ──────────────────────────────────────────────────────────────────────
    // EXCEPTIONS (nest them here or put in a separate exceptions package)
    // ──────────────────────────────────────────────────────────────────────
 
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID id) {
            super("User not found: " + id);
        }
    }
 
    public static class NoActiveSubscriptionException extends RuntimeException {
        public NoActiveSubscriptionException(UUID userId) {
            super("No active subscription found for user: " + userId);
        }
    }

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionHistory(UUID userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsExpiringWithin(int withinDays) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsByStatus(SubscriptionStatus status) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canAddDoctor(UUID userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAddHospital(UUID userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAddClinic(UUID userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canCreatePrescription(UUID userId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getEffectiveAllowedDoctors(UUID userId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getUsedDoctorCount(UUID userId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void incrementUsedDoctors(UUID userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void decrementUsedDoctors(UUID userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UUID createSubscription(UUID userId, Long packageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserSubscriptionSummaryDto renewSubscription(UUID subscriptionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserSubscriptionSummaryDto upgradeSubscription(UUID userId, Long newPackageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancelSubscription(UUID userId, UUID cancelledBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void suspendSubscription(UUID subscriptionId, UUID suspendedBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reactivateSubscription(UUID subscriptionId, UUID reactivatedBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void expireSubscription(UUID subscriptionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int expireAllOverdueSubscriptions(LocalDate asOf) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void markPaymentPaid(UUID subscriptionId, String transactionRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markPaymentFailed(UUID subscriptionId, String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long requestDoctorAddon(UUID subscriptionId, int additionalDoctors) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void approveDoctorAddon(Long addonId, UUID approvedBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectDoctorAddon(Long addonId, UUID rejectedBy, String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markAddonPaymentPaid(Long addonId, String transactionRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DoctorAddonDto> getAddonsForSubscription(UUID subscriptionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DoctorAddonDto> getActiveAddonsForSubscription(UUID subscriptionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void allocateDoctor(UUID subscriptionId, UUID doctorId, String allocationType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deallocateDoctor(UUID subscriptionId, UUID doctorId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DoctorAllocationDto> getActiveDoctorAllocations(UUID subscriptionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DoctorAllocationDto> getAllDoctorAllocations(UUID subscriptionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsForDoctor(UUID doctorId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FacilityDto> getLinkedHospitals(UUID userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FacilityDto> getLinkedClinics(UUID userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getHospitalCount(UUID userId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getClinicCount(UUID userId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getAllActiveSubscriptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsByPackage(Long packageId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsWithPendingAddons() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long countActiveSubscriptions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long countTotalAllocatedDoctors() {
		// TODO Auto-generated method stub
		return 0;
	}
}