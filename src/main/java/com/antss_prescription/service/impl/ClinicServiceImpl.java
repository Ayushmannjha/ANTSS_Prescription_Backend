package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreateClinicRequest;
import com.antss_prescription.dto.response.ClinicResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.*;
import com.antss_prescription.service.ClinicService;
import com.antss_prescription.service.EmailService;
import com.antss_prescription.security.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final DoctorRepository doctorRepository;
    private final PasswordResetTokenService passwordResetTokenService;


    @Override
    public ClinicResponse createClinic(CreateClinicRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        if (owner.getUserType() != UserType.CLINIC) {
            throw new BusinessException("Only CLINIC type users can create clinics.");
        }

        List<UserSubscription> activeSubs = userSubscriptionRepository
                .findValidByUserIdForUpdate(ownerId, java.time.LocalDate.now());
        if (activeSubs.isEmpty()) {
            throw new BusinessException("An active paid subscription is required to create a clinic");
        }
        int totalAllowedClinics = activeSubs.stream()
                .mapToInt(UserSubscription::getAllowedClinics).sum();
        int currentClinics = clinicRepository.findByOwnerId(ownerId).size();

        if (currentClinics >= totalAllowedClinics) {
            throw new BusinessException("Clinic limit reached (" + totalAllowedClinics +
                    "). Upgrade your plan to add more clinics.");
        }

        java.time.LocalDate subEndDate = activeSubs.get(0).getEndDate();

        int allowedDoctors = activeSubs.stream()
                .mapToInt(UserSubscription::getAllowedDoctors).sum();


        String plainPassword = generateSecurePassword(12);
        User clinicUser = new User();
        clinicUser.setFullName(request.getClinicName());
        clinicUser.setEmail(request.getEmail());
        clinicUser.setMobileNumber(request.getMobileNumber());
       // clinicUser.setPassword(passwordEncoder.encode(plainPassword));
        clinicUser.setUserType(UserType.CLINIC);
        clinicUser.setStatus(RegistrationStatus.APPROVED);
        clinicUser.setRole(Role.ROLE_USER);
        User savedClinicUser = userRepository.save(clinicUser);


        LoginCredential credential = new LoginCredential();
        credential.setUser(savedClinicUser);
        credential.setUsername(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(plainPassword));
        loginCredentialRepository.save(credential);

        Clinic clinic = new Clinic();
        clinic.setClinicName(request.getClinicName());
        clinic.setClinicCode(generateUniqueClinicCode());
        clinic.setAddressLine1(request.getAddressLine1());
        clinic.setCity(request.getCity());
        clinic.setState(request.getState());
        clinic.setPincode(request.getPincode());
        clinic.setEmail(request.getEmail());
        clinic.setMobileNumber(request.getMobileNumber());
        clinic.setOwner(owner);
        clinic.setUser(savedClinicUser);
        clinic.setMaxDoctorLimit(allowedDoctors);
        clinic.setStatus(EntityStatus.ACTIVE);
        Clinic saved = clinicRepository.save(clinic);
        String setupToken = passwordResetTokenService.issue(savedClinicUser);


        emailService.sendCredentialsEmail(
                request.getEmail(),
                request.getClinicName(),
                request.getEmail(),
                "Clinic",
                subEndDate,
                UserType.CLINIC,
                setupToken
        );

        log.info("Clinic created: {} (code: {})", saved.getClinicName(), saved.getClinicCode());
        return modelMapper.map(saved, ClinicResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicResponse> listClinics(UUID userId) {
        return clinicRepository.findByUserIdOrOwnerId(userId, userId)
                .stream()
                .map(clinic -> modelMapper.map(clinic, ClinicResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicResponse getClinicById(Long id, UUID userId) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", id));
        
        boolean hasAccess = clinic.getUser().getId().equals(userId)
                || (clinic.getOwner() != null && clinic.getOwner().getId().equals(userId));
        if (!hasAccess) {
            // Check if user is a doctor of this clinic
            hasAccess = doctorRepository.findByUserId(userId)
                    .map(doctor -> doctor.getClinic() != null && doctor.getClinic().getId().equals(id))
                    .orElse(false);
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to clinic resource");
        }
        return modelMapper.map(clinic, ClinicResponse.class);
    }

    private String generateUniqueClinicCode() {
        String code;
        do {
            code = "CLIN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (clinicRepository.findByClinicCode(code).isPresent());
        return code;
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
}
