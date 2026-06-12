package com.antss_prescription.service.impl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antss_prescription.dto.request.UserSubscriptionSummaryDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAddonDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAllocationDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.FacilityDto;
import com.antss_prescription.dto.response.UserBasicDto;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.SubscriptionDoctorAllocation;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.AllocationStatus;
import com.antss_prescription.enums.AllocationType;
import com.antss_prescription.enums.DurationType;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorAddonRepository;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.PackageRepository;
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
    private final PackageRepository                    packageRepository;
    private final DoctorRepository                     doctorRepository;
 
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
 
        return mapSubscriptionToSummary(subscription, user);
    }

    private UserSubscriptionSummaryDto mapSubscriptionToSummary(UserSubscription subscription, User user) {
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
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		List<UserSubscription> subscriptions = subscriptionRepository.findAllByUserId(userId);
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, user))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsExpiringWithin(int withinDays) {
		LocalDate today = LocalDate.now();
		LocalDate limitDate = today.plusDays(withinDays);
		List<UserSubscription> subscriptions = subscriptionRepository.findExpiringSubscriptions(today, limitDate);
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, sub.getUser()))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsByStatus(SubscriptionStatus status) {
		List<UserSubscription> subscriptions = subscriptionRepository.findBySubscriptionStatusWithRelations(status);
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, sub.getUser()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean canAddDoctor(UUID userId) {
		if (!hasValidSubscription(userId)) {
			return false;
		}
		return getRemainingDoctorSlots(userId) > 0;
	}

	@Override
	public boolean canAddHospital(UUID userId) {
		if (!hasValidSubscription(userId)) {
			return false;
		}
		return subscriptionRepository.findActiveByUserId(userId)
				.map(sub -> getHospitalCount(userId) < sub.getAllowedHospitals())
				.orElse(false);
	}

	@Override
	public boolean canAddClinic(UUID userId) {
		if (!hasValidSubscription(userId)) {
			return false;
		}
		return subscriptionRepository.findActiveByUserId(userId)
				.map(sub -> getClinicCount(userId) < sub.getAllowedClinics())
				.orElse(false);
	}

	@Override
	public boolean canCreatePrescription(UUID userId) {
		return hasValidSubscription(userId);
	}

	@Override
	public int getEffectiveAllowedDoctors(UUID userId) {
		return subscriptionRepository.findActiveByUserId(userId)
				.map(sub -> {
					List<DoctorAddon> activeAddons = addonRepository.findApprovedAndPaidBySubscriptionId(sub.getId());
					int addonDoctors = activeAddons.stream()
							.mapToInt(DoctorAddon::getAdditionalDoctors)
							.sum();
					return sub.getSubscriptionPackage().getBaseDoctorLimit() + addonDoctors;
				})
				.orElse(0);
	}

	@Override
	public int getUsedDoctorCount(UUID userId) {
		return subscriptionRepository.findActiveByUserId(userId)
				.map(UserSubscription::getUsedDoctors)
				.orElse(0);
	}

	@Override
	@Transactional
	public void incrementUsedDoctors(UUID userId) {
		UserSubscription sub = subscriptionRepository.findActiveByUserId(userId)
				.orElseThrow(() -> new NoActiveSubscriptionException(userId));
		int allowed = getEffectiveAllowedDoctors(userId);
		if (sub.getUsedDoctors() >= allowed) {
			throw new DoctorQuotaExceededException(userId);
		}
		sub.setUsedDoctors(sub.getUsedDoctors() + 1);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void decrementUsedDoctors(UUID userId) {
		UserSubscription sub = subscriptionRepository.findActiveByUserId(userId)
				.orElseThrow(() -> new NoActiveSubscriptionException(userId));
		int current = sub.getUsedDoctors();
		sub.setUsedDoctors(Math.max(0, current - 1));
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public UUID createSubscription(UUID userId, Long packageId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		
		if (subscriptionRepository.findActiveByUserId(userId).isPresent()) {
			throw new DuplicateSubscriptionException(userId);
		}
		
		SubscriptionPackage pkg = packageRepository.findById(packageId)
				.orElseThrow(() -> new PackageNotFoundException(packageId));
		if (!pkg.isActive()) {
			throw new PackageNotFoundException(packageId);
		}
		
		UserSubscription sub = new UserSubscription();
		sub.setUser(user);
		sub.setSubscriptionPackage(pkg);
		sub.setStartDate(LocalDate.now());
		
		LocalDate endDate = LocalDate.now();
		if (pkg.getDurationType() == DurationType.SIX_MONTH) {
			endDate = endDate.plusMonths(6);
		} else if (pkg.getDurationType() == DurationType.ONE_YEAR) {
			endDate = endDate.plusYears(1);
		} else if (pkg.getDurationType() == DurationType.TWO_YEAR) {
			endDate = endDate.plusYears(2);
		}
		
		sub.setEndDate(endDate);
		sub.setAllowedDoctors(pkg.getBaseDoctorLimit());
		sub.setUsedDoctors(0);
		sub.setAllowedHospitals(1);
		sub.setAllowedClinics(1);
		sub.setPaymentStatus(PaymentStatus.PENDING);
		sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return saved.getId();
	}

	@Override
	@Transactional
	public UserSubscriptionSummaryDto renewSubscription(UUID subscriptionId) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		
		SubscriptionPackage pkg = sub.getSubscriptionPackage();
		LocalDate referenceDate = sub.getEndDate().isAfter(LocalDate.now()) ? sub.getEndDate() : LocalDate.now();
		
		LocalDate newEndDate = referenceDate;
		if (pkg.getDurationType() == DurationType.SIX_MONTH) {
			newEndDate = newEndDate.plusMonths(6);
		} else if (pkg.getDurationType() == DurationType.ONE_YEAR) {
			newEndDate = newEndDate.plusYears(1);
		} else if (pkg.getDurationType() == DurationType.TWO_YEAR) {
			newEndDate = newEndDate.plusYears(2);
		}
		
		sub.setEndDate(newEndDate);
		sub.setPaymentStatus(PaymentStatus.PENDING);
		sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return mapSubscriptionToSummary(saved, saved.getUser());
	}

	@Override
	@Transactional
	public UserSubscriptionSummaryDto upgradeSubscription(UUID userId, Long newPackageId) {
		UserSubscription sub = subscriptionRepository.findActiveByUserId(userId)
				.orElseThrow(() -> new NoActiveSubscriptionException(userId));
		
		SubscriptionPackage newPkg = packageRepository.findById(newPackageId)
				.orElseThrow(() -> new PackageNotFoundException(newPackageId));
		if (!newPkg.isActive()) {
			throw new PackageNotFoundException(newPackageId);
		}
		
		sub.setSubscriptionPackage(newPkg);
		sub.setStartDate(LocalDate.now());
		
		LocalDate endDate = LocalDate.now();
		if (newPkg.getDurationType() == DurationType.SIX_MONTH) {
			endDate = endDate.plusMonths(6);
		} else if (newPkg.getDurationType() == DurationType.ONE_YEAR) {
			endDate = endDate.plusYears(1);
		} else if (newPkg.getDurationType() == DurationType.TWO_YEAR) {
			endDate = endDate.plusYears(2);
		}
		sub.setEndDate(endDate);
		
		List<DoctorAddon> activeAddons = addonRepository.findApprovedAndPaidBySubscriptionId(sub.getId());
		int addonDoctors = activeAddons.stream()
				.mapToInt(DoctorAddon::getAdditionalDoctors)
				.sum();
		sub.setAllowedDoctors(newPkg.getBaseDoctorLimit() + addonDoctors);
		sub.setPaymentStatus(PaymentStatus.PENDING);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return mapSubscriptionToSummary(saved, saved.getUser());
	}

	@Override
	@Transactional
	public void cancelSubscription(UUID userId, UUID cancelledBy) {
		UserSubscription sub = subscriptionRepository.findActiveByUserId(userId)
				.orElseThrow(() -> new NoActiveSubscriptionException(userId));
		sub.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void suspendSubscription(UUID subscriptionId, UUID suspendedBy) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void reactivateSubscription(UUID subscriptionId, UUID reactivatedBy) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void expireSubscription(UUID subscriptionId) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public int expireAllOverdueSubscriptions(LocalDate asOf) {
		List<UserSubscription> activeSubs = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
		int count = 0;
		for (UserSubscription sub : activeSubs) {
			if (sub.getEndDate().isBefore(asOf)) {
				sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
				subscriptionRepository.save(sub);
				count++;
			}
		}
		return count;
	}

	@Override
	@Transactional
	public void markPaymentPaid(UUID subscriptionId, String transactionRef) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setPaymentStatus(PaymentStatus.PAID);
		if (sub.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
			sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		}
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void markPaymentFailed(UUID subscriptionId, String reason) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setPaymentStatus(PaymentStatus.FAILED);
		sub.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public Long requestDoctorAddon(UUID subscriptionId, int additionalDoctors) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		
		if (sub.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
			throw new RuntimeException("Cannot add doctors to an inactive/expired subscription");
		}

		SubscriptionPackage pkg = sub.getSubscriptionPackage();
		BigDecimal extraPrice = pkg.getExtraDoctorPrice();

		long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
		if (daysRemaining <= 0) {
			throw new RuntimeException("Subscription is already expiring or expired");
		}

		int remainingMonths = (int) Math.max(1, Math.ceil(daysRemaining / 30.4375));

		BigDecimal prorataAmount = extraPrice
				.multiply(BigDecimal.valueOf(additionalDoctors))
				.multiply(BigDecimal.valueOf(remainingMonths))
				.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);

		DoctorAddon addon = new DoctorAddon();
		addon.setUserSubscription(sub);
		addon.setAdditionalDoctors(additionalDoctors);
		addon.setYearlyPricePerDoctor(extraPrice);
		addon.setRemainingMonths(remainingMonths);
		addon.setProrataAmount(prorataAmount);
		addon.setStartDate(LocalDate.now());
		addon.setEndDate(sub.getEndDate());
		addon.setPaymentStatus(PaymentStatus.PENDING);
		addon.setApprovalStatus(AddonApprovalStatus.PENDING);

		DoctorAddon saved = addonRepository.save(addon);
		return saved.getId();
	}

	@Override
	@Transactional
	public void approveDoctorAddon(Long addonId, UUID approvedBy) {
		DoctorAddon addon = addonRepository.findById(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		User admin = userRepository.findById(approvedBy)
				.orElseThrow(() -> new UserNotFoundException(approvedBy));
		
		addon.setApprovalStatus(AddonApprovalStatus.APPROVED);
		addon.setApprovedBy(admin);
		addon.setApprovedAt(LocalDateTime.now());
		addonRepository.save(addon);
	}

	@Override
	@Transactional
	public void rejectDoctorAddon(Long addonId, UUID rejectedBy, String reason) {
		DoctorAddon addon = addonRepository.findById(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		addon.setApprovalStatus(AddonApprovalStatus.REJECTED);
		addonRepository.save(addon);
	}

	@Override
	@Transactional
	public void markAddonPaymentPaid(Long addonId, String transactionRef) {
		DoctorAddon addon = addonRepository.findById(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		addon.setPaymentStatus(PaymentStatus.PAID);
		addonRepository.save(addon);
		
		UserSubscription sub = addon.getUserSubscription();
		int currentAllowed = sub.getAllowedDoctors();
		sub.setAllowedDoctors(currentAllowed + addon.getAdditionalDoctors());
		subscriptionRepository.save(sub);
	}

	@Override
	public List<DoctorAddonDto> getAddonsForSubscription(UUID subscriptionId) {
		List<DoctorAddon> addons = addonRepository.findBySubscriptionId(subscriptionId);
		return mapAddons(addons);
	}

	@Override
	public List<DoctorAddonDto> getActiveAddonsForSubscription(UUID subscriptionId) {
		List<DoctorAddon> addons = addonRepository.findApprovedAndPaidBySubscriptionId(subscriptionId);
		LocalDate today = LocalDate.now();
		List<DoctorAddon> activeAddons = addons.stream()
				.filter(a -> !a.getEndDate().isBefore(today))
				.toList();
		return mapAddons(activeAddons);
	}

	@Override
	@Transactional
	public void allocateDoctor(UUID subscriptionId, UUID doctorId, String allocationType) {
		UserSubscription sub = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
		
		Optional<SubscriptionDoctorAllocation> existing = allocationRepository
				.findByUserSubscriptionIdAndDoctorIdAndStatus(subscriptionId, doctorId, AllocationStatus.ACTIVE);
		if (existing.isPresent()) {
			throw new DoctorAlreadyAllocatedException(doctorId, subscriptionId);
		}
		
		int allowed = getEffectiveAllowedDoctors(sub.getUser().getId());
		if (sub.getUsedDoctors() >= allowed) {
			throw new DoctorQuotaExceededException(sub.getUser().getId());
		}
		
		SubscriptionDoctorAllocation allocation = new SubscriptionDoctorAllocation();
		allocation.setUserSubscription(sub);
		allocation.setDoctor(doctor);
		allocation.setAllocationType(AllocationType.valueOf(allocationType.toUpperCase()));
		allocation.setStatus(AllocationStatus.ACTIVE);
		
		allocationRepository.save(allocation);
		
		sub.setUsedDoctors(sub.getUsedDoctors() + 1);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void deallocateDoctor(UUID subscriptionId, UUID doctorId) {
		SubscriptionDoctorAllocation allocation = allocationRepository
				.findByUserSubscriptionIdAndDoctorIdAndStatus(subscriptionId, doctorId, AllocationStatus.ACTIVE)
				.orElseThrow(() -> new RuntimeException("Active allocation not found for doctor: " + doctorId + " under subscription: " + subscriptionId));
		
		allocation.setStatus(AllocationStatus.INACTIVE);
		allocationRepository.save(allocation);
		
		UserSubscription sub = allocation.getUserSubscription();
		sub.setUsedDoctors(Math.max(0, sub.getUsedDoctors() - 1));
		subscriptionRepository.save(sub);
	}

	@Override
	public List<DoctorAllocationDto> getActiveDoctorAllocations(UUID subscriptionId) {
		List<SubscriptionDoctorAllocation> allocations = allocationRepository.findActiveBySubscriptionId(subscriptionId);
		return mapAllocations(allocations);
	}

	@Override
	public List<DoctorAllocationDto> getAllDoctorAllocations(UUID subscriptionId) {
		List<SubscriptionDoctorAllocation> allocations = allocationRepository.findAllBySubscriptionId(subscriptionId);
		return mapAllocations(allocations);
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsForDoctor(UUID doctorId) {
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
		List<SubscriptionDoctorAllocation> allocations = allocationRepository
				.findByDoctorAndStatus(doctor, AllocationStatus.ACTIVE);
		return allocations.stream()
				.map(sda -> {
					UserSubscription sub = sda.getUserSubscription();
					return mapSubscriptionToSummary(sub, sub.getUser());
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<FacilityDto> getLinkedHospitals(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		List<Hospital> hospitals = hospitalRepository.findByOwnerUser(user);
		return mapHospitals(hospitals);
	}

	@Override
	public List<FacilityDto> getLinkedClinics(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		List<Clinic> clinics = clinicRepository.findByOwnerUser(user);
		return mapClinics(clinics);
	}

	@Override
	public int getHospitalCount(UUID userId) {
		return hospitalRepository.findByOwnerId(userId).size();
	}

	@Override
	public int getClinicCount(UUID userId) {
		return clinicRepository.findByOwnerId(userId).size();
	}

	@Override
	public List<UserSubscriptionSummaryDto> getAllActiveSubscriptions() {
		List<UserSubscription> subscriptions = subscriptionRepository.findAllActiveSubscriptionsWithRelations();
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, sub.getUser()))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsByPackage(Long packageId) {
		List<UserSubscription> subscriptions = subscriptionRepository.findByPackageIdWithRelations(packageId);
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, sub.getUser()))
				.collect(Collectors.toList());
	}

	@Override
	public List<UserSubscriptionSummaryDto> getSubscriptionsWithPendingAddons() {
		List<UserSubscription> subscriptions = subscriptionRepository.findSubscriptionsWithPendingAddons();
		return subscriptions.stream()
				.map(sub -> mapSubscriptionToSummary(sub, sub.getUser()))
				.collect(Collectors.toList());
	}

	@Override
	public long countActiveSubscriptions() {
		return subscriptionRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
	}

	@Override
	public long countTotalAllocatedDoctors() {
		return allocationRepository.countByStatus(AllocationStatus.ACTIVE);
	}

	@Override
	public List<UserBasicDto> getAllUsers() {
	    return userRepository.findAll()
	            .stream()
	            .map(user -> {
	                UserBasicDto.UserBasicDtoBuilder dto = UserBasicDto.builder()
	                        .userId(user.getId())
	                        .fullName(user.getFullName())
	                        .email(user.getEmail())
	                        .role(user.getRole().name());

	                subscriptionRepository.findActiveByUserId(user.getId())
	                        .ifPresent(sub -> dto
	                                .subscriptionId(sub.getId())
	                                .packageName(sub.getSubscriptionPackage().getPackageName())
	                                .startDate(sub.getStartDate())
	                                .endDate(sub.getEndDate())
	                                .daysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate()))
	                                .subscriptionStatus(sub.getSubscriptionStatus())
	                                .paymentStatus(sub.getPaymentStatus())
	                                .allowedDoctors(sub.getAllowedDoctors())
	                                .usedDoctors(sub.getUsedDoctors())
	                                .allowedHospitals(sub.getAllowedHospitals())
	                                .allowedClinics(sub.getAllowedClinics())
	                        );

	                return dto.build();
	            })
	            .collect(Collectors.toList());
	}

}