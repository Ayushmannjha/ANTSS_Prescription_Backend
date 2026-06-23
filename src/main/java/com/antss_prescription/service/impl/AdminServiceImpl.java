package com.antss_prescription.service.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antss_prescription.dto.request.ExtendValidityRequest;
import com.antss_prescription.dto.request.ModifyPackageRequest;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.AllocationStatus;
import com.antss_prescription.enums.DurationType;
import com.antss_prescription.enums.FacilityType;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorAddonRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.LoginCredentialRepository;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.repository.SubscriptionDoctorAllocationRepository;
import com.antss_prescription.service.AdminService;
import com.antss_prescription.service.EmailService;
import com.antss_prescription.security.PasswordResetTokenService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorAddonRepository doctorAddonRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginCredentialRepository loginCredentialRepository;
    private final SubscriptionDoctorAllocationRepository allocationRepository;
    private final LinkedAccountStatusService linkedAccountStatusService;
    private final PasswordResetTokenService passwordResetTokenService;


    @Override
    public List<UserResponse> getPendingRegistrations() {
        return userRepository.findByStatus(RegistrationStatus.PENDING)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse approveUser(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException("Only PENDING registrations can be approved");
        }

        String plainPassword = generateSecurePassword(12);
        String encodedPassword = passwordEncoder.encode(plainPassword);

        user.setStatus(RegistrationStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());
      //  user.setPassword(encodedPassword);
        User saved = userRepository.save(user);

        LoginCredential credential = loginCredentialRepository.findByUserId(userId)
                .orElse(new LoginCredential());
        credential.setUser(saved);
        credential.setUsername(saved.getEmail());
        credential.setPasswordHash(encodedPassword);
        loginCredentialRepository.save(credential);
        String setupToken = passwordResetTokenService.issue(saved);

        LocalDate subEndDate = LocalDate.now().plusYears(1); // fallback
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(userId);
        if (!subscriptions.isEmpty()) {
            UserSubscription sub = subscriptions.get(0);
            sub.setStartDate(LocalDate.now());

            LocalDate endDate = LocalDate.now();
            SubscriptionPackage pkg = sub.getSubscriptionPackage();
            if (pkg.getDurationType() == DurationType.SIX_MONTH) {
                endDate = endDate.plusMonths(6);
            } else if (pkg.getDurationType() == DurationType.ONE_YEAR) {
                endDate = endDate.plusYears(1);
            } else if (pkg.getDurationType() == DurationType.TWO_YEAR) {
                endDate = endDate.plusYears(2);
            }
            sub.setEndDate(endDate);
            sub.setPaymentStatus(PaymentStatus.PAID);
            sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            userSubscriptionRepository.save(sub);
            subEndDate = endDate;

            updateEntityMaxDoctorLimit(user, sub.getAllowedDoctors());
        }

        emailService.sendApprovalEmail(user.getEmail(), user.getFullName(), setupToken);
        log.info("User approved and credentials emailed: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse rejectUser(UUID userId) {
        User user = getUserOrThrow(userId);

        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException("Only PENDING registrations can be rejected");
        }

        user.setStatus(RegistrationStatus.REJECTED);
        User saved = userRepository.save(user);

        emailService.sendRejectionEmail(user.getEmail(), user.getFullName());
        log.info("User rejected: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse modifyUserPackage(UUID userId, ModifyPackageRequest request) {
        User user = getUserOrThrow(userId);

        SubscriptionPackage newPkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));

        if (!newPkg.isActive()) {
            throw new BusinessException("Selected package is not active");
        }

        List<UserSubscription> activeSubs = userSubscriptionRepository.findValidByUserIdForUpdate(
                userId, LocalDate.now());
        if (activeSubs.isEmpty()) {
            throw new BusinessException("User has no active subscription to modify");
        }

        UserSubscription sub = activeSubs.get(0);
        int activeAllocations = Math.toIntExact(allocationRepository
                .countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE));
        int activeAddonDoctors = doctorAddonRepository.findApprovedAndPaidBySubscriptionId(sub.getId()).stream()
                .filter(addon -> !addon.getEndDate().isBefore(LocalDate.now()))
                .mapToInt(DoctorAddon::getAdditionalDoctors)
                .sum();
        int newAllowedDoctors = newPkg.getBaseDoctorLimit() + activeAddonDoctors;
        if (newAllowedDoctors < activeAllocations) {
            throw new BusinessException("Package allows fewer doctors than are currently allocated");
        }
        sub.setSubscriptionPackage(newPkg);
        sub.setAllowedDoctors(newAllowedDoctors);
        
        LocalDate endDate = sub.getStartDate();
        if (newPkg.getDurationType() == DurationType.SIX_MONTH) {
            endDate = endDate.plusMonths(6);
        } else if (newPkg.getDurationType() == DurationType.ONE_YEAR) {
            endDate = endDate.plusYears(1);
        } else if (newPkg.getDurationType() == DurationType.TWO_YEAR) {
            endDate = endDate.plusYears(2);
        }
        sub.setEndDate(endDate);
        userSubscriptionRepository.save(sub);

        updateEntityMaxDoctorLimit(user, sub.getAllowedDoctors());

        log.info("Package modified for user: {}", user.getEmail());
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse extendValidity(UUID userId, ExtendValidityRequest request) {
        User user = getUserOrThrow(userId);

        UserSubscription sub = userSubscriptionRepository.findByUserIdForUpdate(userId).stream()
                .max(java.util.Comparator.comparing(UserSubscription::getEndDate))
                .orElseThrow(() -> new BusinessException("User does not have a subscription to extend"));
        LocalDate currentEnd = sub.getEndDate();
        LocalDate base = currentEnd.isBefore(LocalDate.now()) ? LocalDate.now() : currentEnd;
        sub.setEndDate(base.plusDays(request.getDays()));
        sub.setPaymentStatus(PaymentStatus.PAID);
        sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        userSubscriptionRepository.save(sub);

        linkedAccountStatusService.reactivateOwnerAndSubscriptionExpiredAccounts(user);

        log.info("Validity extended for user: {} by {} days", user.getEmail(), request.getDays());
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse blockUser(UUID userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(RegistrationStatus.INACTIVE);
        loginSessionRepository.expireAllSessionsForUser(user);
        User saved = userRepository.save(user);
        log.info("User blocked: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse unblockUser(UUID userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(RegistrationStatus.APPROVED);
        User saved = userRepository.save(user);
        log.info("User unblocked: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public List<DoctorAddonResponse> getPendingAddons() {
        return doctorAddonRepository.findByApprovalStatus(AddonApprovalStatus.PENDING)
                .stream().map(this::mapToAddonResponse).collect(Collectors.toList());
    }

    @Override
    public DoctorAddonResponse approveDoctorAddon(Long addonId, UUID adminUserId) {
        DoctorAddon addon = doctorAddonRepository.findByIdForUpdate(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor Addon", addonId));

        if (addon.getApprovalStatus() != AddonApprovalStatus.PENDING) {
            throw new BusinessException("Addon request is already processed");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminUserId));

        addon.setApprovalStatus(AddonApprovalStatus.APPROVED);
        addon.setPaymentStatus(PaymentStatus.PAID);
        addon.setApprovedBy(admin);
        addon.setApprovedAt(LocalDateTime.now());
        DoctorAddon savedAddon = doctorAddonRepository.save(addon);

        UserSubscription sub = userSubscriptionRepository.findByIdForUpdate(addon.getUserSubscription().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", addon.getUserSubscription().getId()));
        syncAllowedDoctorEntitlement(sub);

        updateEntityMaxDoctorLimit(sub.getUser(), sub.getAllowedDoctors());

        log.info("Doctor addon request approved by admin {}: subscription {} increased by {} doctors", 
                adminUserId, sub.getId(), addon.getAdditionalDoctors());
        return mapToAddonResponse(savedAddon);
    }

    @Override
    public DoctorAddonResponse rejectDoctorAddon(Long addonId, UUID adminUserId) {
        DoctorAddon addon = doctorAddonRepository.findByIdForUpdate(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor Addon", addonId));

        if (addon.getApprovalStatus() != AddonApprovalStatus.PENDING) {
            throw new BusinessException("Addon request is already processed");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminUserId));

        addon.setApprovalStatus(AddonApprovalStatus.REJECTED);
        addon.setRejectedBy(admin);
        addon.setRejectedAt(LocalDateTime.now());
        DoctorAddon savedAddon = doctorAddonRepository.save(addon);

        log.info("Doctor addon request rejected by admin {}", adminUserId);
        return mapToAddonResponse(savedAddon);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private UserResponse mapToUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    private DoctorAddonResponse mapToAddonResponse(DoctorAddon addon) {
        DoctorAddonResponse res = modelMapper.map(addon, DoctorAddonResponse.class);
        if (addon.getApprovedBy() != null) {
            res.setApprovedByUserId(addon.getApprovedBy().getId());
        }
        if (addon.getRejectedBy() != null) {
            res.setRejectedByUserId(addon.getRejectedBy().getId());
        }
        UserSubscription subscription = addon.getUserSubscription();
        res.setUserSubscriptionId(subscription.getId());
        res.setEntityId(addon.getFacilityId());
        res.setUsername(subscription.getUser().getFullName());
        res.setUserEmail(subscription.getUser().getEmail());
        mapAddonFacility(addon, res);
        return res;
    }

    private void mapAddonFacility(DoctorAddon addon, DoctorAddonResponse response) {
        Long facilityId = addon.getFacilityId();
        if (facilityId == null) return;
        if (addon.getFacilityType() == FacilityType.HOSPITAL) {
            hospitalRepository.findById(facilityId).ifPresent(hospital -> {
                response.setEntityType(FacilityType.HOSPITAL.name());
                response.setEntityName(hospital.getHospitalName());
                response.setState(hospital.getState());
                response.setCity(hospital.getCity());
                response.setAddress(hospital.getAddressLine1());
            });
        } else if (addon.getFacilityType() == FacilityType.CLINIC) {
            clinicRepository.findById(facilityId).ifPresent(clinic -> {
                response.setEntityType(FacilityType.CLINIC.name());
                response.setEntityName(clinic.getClinicName());
                response.setState(clinic.getState());
                response.setCity(clinic.getCity());
                response.setAddress(clinic.getAddressLine1());
            });
        }
    }

    private String generateSecurePassword(int length) {
        final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private void updateEntityMaxDoctorLimit(User user, int newLimit) {
        if (user.getUserType() == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserId(user.getId());
            for (Hospital h : hospitals) {
                h.setMaxDoctorLimit(newLimit);
                hospitalRepository.save(h);
            }
        } else {
            List<Clinic> clinics = clinicRepository.findByUserId(user.getId());
            for (Clinic c : clinics) {
                c.setMaxDoctorLimit(newLimit);
                clinicRepository.save(c);
            }
        }
    }

    private void syncAllowedDoctorEntitlement(UserSubscription subscription) {
        int addonDoctors = doctorAddonRepository.findApprovedAndPaidBySubscriptionId(subscription.getId()).stream()
                .filter(addon -> !addon.getEndDate().isBefore(LocalDate.now()))
                .mapToInt(DoctorAddon::getAdditionalDoctors)
                .sum();
        subscription.setAllowedDoctors(
                subscription.getSubscriptionPackage().getBaseDoctorLimit() + addonDoctors);
        userSubscriptionRepository.save(subscription);
    }
}
