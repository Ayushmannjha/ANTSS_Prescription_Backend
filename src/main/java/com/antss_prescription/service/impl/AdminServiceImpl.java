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
import com.antss_prescription.enums.DurationType;
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
import com.antss_prescription.service.AdminService;
import com.antss_prescription.service.EmailService;

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


    @Override
    public List<UserResponse> getPendingRegistrations() {
        return userRepository.findByStatus(RegistrationStatus.PENDING)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse approveUser(UUID userId) {
        User user = getUserOrThrow(userId);

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

        emailService.sendApprovalEmail(user.getEmail(), user.getFullName(), plainPassword);
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

        List<UserSubscription> activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(userId, SubscriptionStatus.ACTIVE);
        if (activeSubs.isEmpty()) {
            throw new BusinessException("User has no active subscription to modify");
        }

        UserSubscription sub = activeSubs.get(0);
        sub.setSubscriptionPackage(newPkg);
        sub.setAllowedDoctors(newPkg.getBaseDoctorLimit()); // Reset allowed doctor count to new package base limit
        
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

        List<UserSubscription> activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(userId, SubscriptionStatus.ACTIVE);
        if (activeSubs.isEmpty()) {
            throw new BusinessException("User does not have an active subscription to extend");
        }

        UserSubscription sub = activeSubs.get(0);
        LocalDate currentEnd = sub.getEndDate();
        LocalDate base = currentEnd.isBefore(LocalDate.now()) ? LocalDate.now() : currentEnd;
        sub.setEndDate(base.plusDays(request.getDays()));
        userSubscriptionRepository.save(sub);

        if (user.getStatus() == RegistrationStatus.EXPIRED) {
            user.setStatus(RegistrationStatus.APPROVED);
            userRepository.save(user);
        }

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
    	List<DoctorAddon> addons =
    		    doctorAddonRepository.findByApprovalStatus(AddonApprovalStatus.PENDING);
    	
    	List<DoctorAddonResponse> responses = addons.stream()
    	        .map(this::mapToAddonResponse)
    	        .collect(Collectors.toList());
    	
    	for(DoctorAddonResponse addon:responses) {
    		User user  =(userSubscriptionRepository.findById(addon.getUserSubscriptionId())).get().getUser();
    	UserSubscription usersubscription=	userSubscriptionRepository.findById(addon.getUserSubscriptionId()).get();
    	Clinic clinic = clinicRepository.findById(usersubscription.getEntityId()).orElse(null);
    	Hospital hospital = hospitalRepository.findById(usersubscription.getEntityId()).orElse(null);
    	addon.setUsername(user.getFullName());
    		addon.setUserEmail(user.getEmail());
    		addon.setEntityName(
    			    clinic != null
    			        ? clinic.getClinicName()
    			        : hospital != null
    			            ? hospital.getHospitalName()
    			            : null
    			);
    		addon.setState(clinic != null
    			        ? clinic.getState()
    			        : hospital != null
    			            ? hospital.getState()
    			            : null);
    		addon.setCity(clinic != null
    			        ? clinic.getCity()
    			        : hospital != null
    			            ? hospital.getCity()
    			            : null);
    		addon.setEntityType(
    			    clinic != null
    			        ? "CLINIC"
    			        : hospital != null
    			            ? "HOSPITAL"
    			            : null
    			);
    		addon.setCity(clinic != null
			        ? clinic.getAddressLine1()
			        : hospital != null
			            ? hospital.getAddressLine1()
			            : null);
    		
    	}
    	
	

    	
    	return responses;
    }

    @Override
    public DoctorAddonResponse approveDoctorAddon(Long addonId, UUID adminUserId) {
        DoctorAddon addon = doctorAddonRepository.findById(addonId)
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

        UserSubscription sub = addon.getUserSubscription();
        sub.setAllowedDoctors(sub.getAllowedDoctors() + addon.getAdditionalDoctors());
        userSubscriptionRepository.save(sub);

        updateEntityMaxDoctorLimit(sub.getUser(), sub.getAllowedDoctors());

        log.info("Doctor addon request approved by admin {}: subscription {} increased by {} doctors", 
                adminUserId, sub.getId(), addon.getAdditionalDoctors());
        return mapToAddonResponse(savedAddon);
    }

    @Override
    public DoctorAddonResponse rejectDoctorAddon(Long addonId, UUID adminUserId) {
        DoctorAddon addon = doctorAddonRepository.findById(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor Addon", addonId));

        if (addon.getApprovalStatus() != AddonApprovalStatus.PENDING) {
            throw new BusinessException("Addon request is already processed");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminUserId));

        addon.setApprovalStatus(AddonApprovalStatus.REJECTED);
        addon.setApprovedBy(admin);
        addon.setApprovedAt(LocalDateTime.now());
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
        return res;
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
}
