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
import com.antss_prescription.enums.FacilityType;
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
    private final LinkedAccountStatusService            linkedAccountStatusService;
 
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
        int usedDoctors = allocations.size();
 
        // ── Facilities ───────────────────────────────────────────────────
        List<Hospital> hospitals = hospitalRepository.findByOwner(user);
        List<Clinic>   clinics   = clinicRepository.findByOwner(user);
 
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
                .usedDoctors(usedDoctors)
                .availableDoctorSlots(Math.max(0, effectiveAllowed - usedDoctors))
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
                    int activeAllocations = Math.toIntExact(allocationRepository
                            .countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE));
                    return Math.max(0, ceiling - activeAllocations);
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
                        .facilityId(a.getFacilityId())
                        .facilityType(a.getFacilityType() != null ? a.getFacilityType().name() : null)
                        .rejectionReason(a.getRejectionReason())
                        .paymentTransactionRef(a.getPaymentTransactionRef())
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
				.map(sub -> Math.toIntExact(allocationRepository
						.countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE)))
				.orElse(0);
	}

	@Override
	@Transactional
	public void incrementUsedDoctors(UUID userId) {
		syncUsedDoctorCount(userId);
	}

	@Override
	@Transactional
	public void decrementUsedDoctors(UUID userId) {
		syncUsedDoctorCount(userId);
	}

	@Override
	@Transactional
	public UUID createSubscription(UUID userId, Long packageId) {
		User user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		
		boolean hasCurrentSubscription = subscriptionRepository.findByUserIdForUpdate(userId).stream()
				.anyMatch(sub -> sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
						|| sub.getSubscriptionStatus() == SubscriptionStatus.PENDING);
		if (hasCurrentSubscription) {
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
		sub.setSubscriptionStatus(SubscriptionStatus.PENDING);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return saved.getId();
	}

	@Override
	@Transactional
	public UserSubscriptionSummaryDto renewSubscription(UUID subscriptionId) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
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
		sub.setSubscriptionStatus(SubscriptionStatus.PENDING);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return mapSubscriptionToSummary(saved, saved.getUser());
	}

	@Override
	@Transactional
	public UserSubscriptionSummaryDto upgradeSubscription(UUID userId, Long newPackageId) {
		userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		UserSubscription sub = subscriptionRepository.findValidByUserIdForUpdate(userId, LocalDate.now())
				.stream().findFirst().orElseThrow(() -> new NoActiveSubscriptionException(userId));
		
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
		int newAllowedDoctors = newPkg.getBaseDoctorLimit() + addonDoctors;
		int activeAllocations = Math.toIntExact(allocationRepository
				.countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE));
		if (newAllowedDoctors < activeAllocations) {
			throw new RuntimeException("Package allows fewer doctors than are currently allocated");
		}
		sub.setAllowedDoctors(newAllowedDoctors);
		sub.setPaymentStatus(PaymentStatus.PENDING);
		sub.setSubscriptionStatus(SubscriptionStatus.PENDING);
		
		UserSubscription saved = subscriptionRepository.save(sub);
		return mapSubscriptionToSummary(saved, saved.getUser());
	}

	@Override
	@Transactional
	public void cancelSubscription(UUID userId, UUID cancelledBy) {
		UserSubscription sub = subscriptionRepository.findValidByUserIdForUpdate(userId, LocalDate.now())
				.stream().findFirst().orElseThrow(() -> new NoActiveSubscriptionException(userId));
		sub.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
		sub.setCancelledBy(userRepository.findById(cancelledBy)
				.orElseThrow(() -> new UserNotFoundException(cancelledBy)));
		sub.setCancelledAt(LocalDateTime.now());
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void suspendSubscription(UUID subscriptionId, UUID suspendedBy) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
		sub.setSuspendedBy(userRepository.findById(suspendedBy)
				.orElseThrow(() -> new UserNotFoundException(suspendedBy)));
		sub.setSuspendedAt(LocalDateTime.now());
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void reactivateSubscription(UUID subscriptionId, UUID reactivatedBy) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		sub.setReactivatedBy(userRepository.findById(reactivatedBy)
				.orElseThrow(() -> new UserNotFoundException(reactivatedBy)));
		sub.setReactivatedAt(LocalDateTime.now());
		subscriptionRepository.save(sub);
		linkedAccountStatusService.reactivateOwnerAndSubscriptionExpiredAccounts(sub.getUser());
	}

	@Override
	@Transactional
	public void expireSubscription(UUID subscriptionId) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
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
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setPaymentStatus(PaymentStatus.PAID);
		sub.setPaymentTransactionRef(transactionRef);
		sub.setPaymentFailureReason(null);
		if (sub.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED
				|| sub.getSubscriptionStatus() == SubscriptionStatus.PENDING) {
			sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
		}
		subscriptionRepository.save(sub);
		linkedAccountStatusService.reactivateOwnerAndSubscriptionExpiredAccounts(sub.getUser());
	}

	@Override
	@Transactional
	public void markPaymentFailed(UUID subscriptionId, String reason) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		sub.setPaymentStatus(PaymentStatus.FAILED);
		sub.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
		sub.setPaymentFailureReason(reason);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public Long requestDoctorAddon(UUID subscriptionId, int additionalDoctors,
			Long facilityId, FacilityType facilityType) {
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
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
		validateAndSetFacility(addon, sub, facilityId, facilityType);

		DoctorAddon saved = addonRepository.save(addon);
		return saved.getId();
	}

	@Override
	@Transactional
	public void approveDoctorAddon(Long addonId, UUID approvedBy) {
		DoctorAddon addon = addonRepository.findByIdForUpdate(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		if (addon.getApprovalStatus() != AddonApprovalStatus.PENDING) {
			throw new RuntimeException("Doctor addon has already been processed");
		}
		User admin = userRepository.findById(approvedBy)
				.orElseThrow(() -> new UserNotFoundException(approvedBy));
		
		addon.setApprovalStatus(AddonApprovalStatus.APPROVED);
		addon.setApprovedBy(admin);
		addon.setApprovedAt(LocalDateTime.now());
		addonRepository.save(addon);
		syncAllowedDoctorEntitlement(addon.getUserSubscription().getId());
	}

	@Override
	@Transactional
	public void rejectDoctorAddon(Long addonId, UUID rejectedBy, String reason) {
		DoctorAddon addon = addonRepository.findByIdForUpdate(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		if (addon.getApprovalStatus() != AddonApprovalStatus.PENDING) {
			throw new RuntimeException("Doctor addon has already been processed");
		}
		addon.setApprovalStatus(AddonApprovalStatus.REJECTED);
		User rejectingUser = userRepository.findById(rejectedBy)
				.orElseThrow(() -> new UserNotFoundException(rejectedBy));
		addon.setRejectedBy(rejectingUser);
		addon.setRejectedAt(LocalDateTime.now());
		addon.setRejectionReason(reason);
		addonRepository.save(addon);
	}

	@Override
	@Transactional
	public void markAddonPaymentPaid(Long addonId, String transactionRef) {
		DoctorAddon addon = addonRepository.findByIdForUpdate(addonId)
				.orElseThrow(() -> new RuntimeException("Doctor addon not found: " + addonId));
		if (addon.getPaymentStatus() == PaymentStatus.PAID) {
			return;
		}
		addon.setPaymentStatus(PaymentStatus.PAID);
		addon.setPaymentTransactionRef(transactionRef);
		addonRepository.save(addon);
		syncAllowedDoctorEntitlement(addon.getUserSubscription().getId());
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
		UserSubscription sub = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
		if (!doctorBelongsToOwner(doctor, sub.getUser())) {
			throw new RuntimeException("Doctor does not belong to the subscription owner");
		}
		
		Optional<SubscriptionDoctorAllocation> existing = allocationRepository
				.findByUserSubscriptionIdAndDoctorIdAndStatus(subscriptionId, doctorId, AllocationStatus.ACTIVE);
		if (existing.isPresent()) {
			throw new DoctorAlreadyAllocatedException(doctorId, subscriptionId);
		}
		
		int allowed = getEffectiveAllowedDoctors(sub.getUser().getId());
		int activeAllocations = Math.toIntExact(allocationRepository
				.countByUserSubscriptionIdAndStatus(subscriptionId, AllocationStatus.ACTIVE));
		if (activeAllocations >= allowed) {
			throw new DoctorQuotaExceededException(sub.getUser().getId());
		}
		
		SubscriptionDoctorAllocation allocation = new SubscriptionDoctorAllocation();
		allocation.setUserSubscription(sub);
		allocation.setDoctor(doctor);
		allocation.setAllocationType(AllocationType.valueOf(allocationType.toUpperCase()));
		allocation.setStatus(AllocationStatus.ACTIVE);
		
		allocationRepository.save(allocation);
		
		sub.setUsedDoctors(activeAllocations + 1);
		subscriptionRepository.save(sub);
	}

	@Override
	@Transactional
	public void deallocateDoctor(UUID subscriptionId, UUID doctorId) {
		UserSubscription lockedSubscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		SubscriptionDoctorAllocation allocation = allocationRepository
				.findByUserSubscriptionIdAndDoctorIdAndStatus(subscriptionId, doctorId, AllocationStatus.ACTIVE)
				.orElseThrow(() -> new RuntimeException("Active allocation not found for doctor: " + doctorId + " under subscription: " + subscriptionId));
		
		allocation.setStatus(AllocationStatus.INACTIVE);
		allocationRepository.save(allocation);
		
		int activeAllocations = Math.toIntExact(allocationRepository
				.countByUserSubscriptionIdAndStatus(subscriptionId, AllocationStatus.ACTIVE));
		lockedSubscription.setUsedDoctors(activeAllocations);
		subscriptionRepository.save(lockedSubscription);
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
		List<Hospital> hospitals = hospitalRepository.findByOwner(user);
		return mapHospitals(hospitals);
	}

	@Override
	public List<FacilityDto> getLinkedClinics(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		List<Clinic> clinics = clinicRepository.findByOwner(user);
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

	private boolean doctorBelongsToOwner(Doctor doctor, User owner) {
		if (doctor.getHospital() != null) {
			Hospital hospital = doctor.getHospital();
			return hospital.getOwner() != null && hospital.getOwner().getId().equals(owner.getId())
					|| hospital.getUser().getId().equals(owner.getId());
		}
		if (doctor.getClinic() != null) {
			Clinic clinic = doctor.getClinic();
			return clinic.getOwner() != null && clinic.getOwner().getId().equals(owner.getId())
					|| clinic.getUser().getId().equals(owner.getId());
		}
		return false;
	}

	private void validateAndSetFacility(DoctorAddon addon, UserSubscription subscription,
			Long facilityId, FacilityType facilityType) {
		if (facilityId == null || facilityType == null) {
			throw new IllegalArgumentException("Facility id and type are required");
		}
		UUID ownerId = subscription.getUser().getId();
		if (facilityType == FacilityType.HOSPITAL) {
			Hospital hospital = hospitalRepository.findById(facilityId)
					.orElseThrow(() -> new RuntimeException("Hospital not found: " + facilityId));
			if (!facilityBelongsToOwner(hospital.getUser(), hospital.getOwner(), ownerId)) {
				throw new RuntimeException("Hospital does not belong to the subscription owner");
			}
		} else {
			Clinic clinic = clinicRepository.findById(facilityId)
					.orElseThrow(() -> new RuntimeException("Clinic not found: " + facilityId));
			if (!facilityBelongsToOwner(clinic.getUser(), clinic.getOwner(), ownerId)) {
				throw new RuntimeException("Clinic does not belong to the subscription owner");
			}
		}
		addon.setFacilityId(facilityId);
		addon.setFacilityType(facilityType);
	}

	private boolean facilityBelongsToOwner(User facilityUser, User owner, UUID ownerId) {
		return facilityUser != null && ownerId.equals(facilityUser.getId())
				|| owner != null && ownerId.equals(owner.getId());
	}

	private void syncAllowedDoctorEntitlement(UUID subscriptionId) {
		UserSubscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
				.orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
		int addonDoctors = addonRepository.findApprovedAndPaidBySubscriptionId(subscriptionId).stream()
				.filter(addon -> !addon.getEndDate().isBefore(LocalDate.now()))
				.mapToInt(DoctorAddon::getAdditionalDoctors)
				.sum();
		subscription.setAllowedDoctors(
				subscription.getSubscriptionPackage().getBaseDoctorLimit() + addonDoctors);
		subscriptionRepository.save(subscription);
	}

	private void syncUsedDoctorCount(UUID userId) {
		UserSubscription subscription = subscriptionRepository
				.findValidByUserIdForUpdate(userId, LocalDate.now()).stream().findFirst()
				.orElseThrow(() -> new NoActiveSubscriptionException(userId));
		int activeAllocations = Math.toIntExact(allocationRepository
				.countByUserSubscriptionIdAndStatus(subscription.getId(), AllocationStatus.ACTIVE));
		subscription.setUsedDoctors(activeAllocations);
		subscriptionRepository.save(subscription);
	}

}
