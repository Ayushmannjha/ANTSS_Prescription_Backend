package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreateHospitalRequest;
import com.antss_prescription.dto.response.HospitalResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.*;
import com.antss_prescription.service.EmailService;
import com.antss_prescription.service.HospitalService;
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
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final DoctorRepository doctorRepository;
    private final PasswordResetTokenService passwordResetTokenService;


    @Override
    public HospitalResponse createHospital(CreateHospitalRequest request, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        if (owner.getUserType() != UserType.HOSPITAL) {
            throw new BusinessException("Only HOSPITAL type users can create hospitals.");
        }

        List<UserSubscription> activeSubs = userSubscriptionRepository
                .findValidByUserIdForUpdate(ownerId, java.time.LocalDate.now());
        if (activeSubs.isEmpty()) {
            throw new BusinessException("An active paid subscription is required to create a hospital");
        }
        int totalAllowedHospitals = activeSubs.stream()
                .mapToInt(UserSubscription::getAllowedHospitals).sum();
        int currentHospitals = hospitalRepository.findByOwnerId(ownerId).size();

        if (currentHospitals >= totalAllowedHospitals) {
            throw new BusinessException("Hospital limit reached (" + totalAllowedHospitals +
                    "). Upgrade your plan to add more hospitals.");
        }

        java.time.LocalDate subEndDate = activeSubs.get(0).getEndDate();


        int allowedDoctors = activeSubs.stream()
                .mapToInt(UserSubscription::getAllowedDoctors).sum();


        String plainPassword = generateSecurePassword(12);
        User hospitalUser = new User();
        hospitalUser.setFullName(request.getHospitalName());
        hospitalUser.setEmail(request.getEmail());
        hospitalUser.setMobileNumber(request.getMobileNumber());
      //  hospitalUser.setPassword(passwordEncoder.encode(plainPassword));
        hospitalUser.setUserType(UserType.HOSPITAL);
        hospitalUser.setStatus(RegistrationStatus.APPROVED);
        hospitalUser.setRole(Role.ROLE_USER);
        User savedHospitalUser = userRepository.save(hospitalUser);

        LoginCredential credential = new LoginCredential();
        credential.setUser(savedHospitalUser);
        credential.setUsername(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(plainPassword));
        loginCredentialRepository.save(credential);

        Hospital hospital = new Hospital();
        hospital.setHospitalName(request.getHospitalName());
        hospital.setHospitalCode(generateUniqueHospitalCode());
        hospital.setAddressLine1(request.getAddressLine1());
        hospital.setCity(request.getCity());
        hospital.setState(request.getState());
        hospital.setPincode(request.getPincode());
        hospital.setEmail(request.getEmail());
        hospital.setMobileNumber(request.getMobileNumber());
        hospital.setOwner(owner);
        hospital.setUser(savedHospitalUser);
        hospital.setMaxDoctorLimit(allowedDoctors);
        hospital.setStatus(EntityStatus.ACTIVE);
        Hospital saved = hospitalRepository.save(hospital);
        String setupToken = passwordResetTokenService.issue(savedHospitalUser);

        emailService.sendCredentialsEmail(
                request.getEmail(),
                request.getHospitalName(),
                request.getEmail(),
                "Hospital",
                subEndDate,
                UserType.HOSPITAL,
                setupToken
        );

        log.info("Hospital created: {} (code: {})", saved.getHospitalName(), saved.getHospitalCode());
        return modelMapper.map(saved, HospitalResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> listHospitals(UUID userId) {
        return hospitalRepository.findByUserIdOrOwnerId(userId, userId)
                .stream()
                .map(hospital -> modelMapper.map(hospital, HospitalResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long id, UUID userId) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", id));
        
        boolean hasAccess = hospital.getUser().getId().equals(userId)
                || (hospital.getOwner() != null && hospital.getOwner().getId().equals(userId));
        if (!hasAccess) {
            // Check if user is a doctor of this hospital
            hasAccess = doctorRepository.findByUserId(userId)
                    .map(doctor -> doctor.getHospital() != null && doctor.getHospital().getId().equals(id))
                    .orElse(false);
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to hospital resource");
        }
        return modelMapper.map(hospital, HospitalResponse.class);
    }

    private String generateUniqueHospitalCode() {
        String code;
        do {
            code = "HOSP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (hospitalRepository.findByHospitalCode(code).isPresent());
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
