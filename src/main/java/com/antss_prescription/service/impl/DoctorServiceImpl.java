package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreateDoctorRequest;
import com.antss_prescription.dto.request.UpdateDoctorRequest;
import com.antss_prescription.dto.response.DoctorResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.*;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.*;
import com.antss_prescription.service.DoctorService;
import com.antss_prescription.service.EmailService;
import com.antss_prescription.security.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final RmoRepository rmoRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionDoctorAllocationRepository allocationRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final PasswordResetTokenService passwordResetTokenService;

    @Override
    public DoctorResponse addDoctor(CreateDoctorRequest request, UUID userId) {
        User caller = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        Hospital hospital = null;
        Clinic clinic = null;
        User owner = null;

        if (caller.getUserType() == UserType.HOSPITAL) {
            if (request.getHospitalId() == null) {
                List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
                if (hospitals.isEmpty()) {
                    hospitals = hospitalRepository.findByOwnerId(userId);
                }
                if (hospitals.isEmpty()) {
                    throw new BusinessException("Hospital not found for user");
                }
                hospital = hospitals.get(0);
            } else {
                hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hospital", request.getHospitalId()));
                if (!hospital.getUser().getId().equals(userId) && !hospital.getOwner().getId().equals(userId)) {
                    throw new BusinessException("Unauthorized hospital access");
                }
            }
            owner = hospital.getOwner();
        } else {
            if (request.getClinicId() == null) {
                List<Clinic> clinics = clinicRepository.findByUserId(userId);
                if (clinics.isEmpty()) {
                    clinics = clinicRepository.findByOwnerId(userId);
                }
                if (clinics.isEmpty()) {
                    throw new BusinessException("Clinic not found for user");
                }
                clinic = clinics.get(0);
            } else {
                clinic = clinicRepository.findById(request.getClinicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));
                if (!clinic.getUser().getId().equals(userId) && !clinic.getOwner().getId().equals(userId)) {
                    throw new BusinessException("Unauthorized clinic access");
                }
            }
            owner = clinic.getOwner();
        }

        UUID ownerId = owner.getId();

        List<UserSubscription> activeSubs = userSubscriptionRepository.findValidByUserIdForUpdate(
                ownerId, java.time.LocalDate.now());
        if (activeSubs.isEmpty()) {
            throw new BusinessException("An active paid subscription is required to add a doctor");
        }
        int totalAllowedDoctors = activeSubs.stream().mapToInt(UserSubscription::getAllowedDoctors).sum();

        int activeDoctors = getActiveDoctorCountForUser(ownerId, owner.getUserType());

        if (activeDoctors >= totalAllowedDoctors) {
            throw new BusinessException("Doctor limit reached (" + totalAllowedDoctors
                    + "). Please purchase doctor addons to add more doctors.");
        }

        java.time.LocalDate subEndDate = activeSubs.get(0).getEndDate();

        String plainPassword = generateSecurePassword(12);
        User doctorUser = new User();
        doctorUser.setFullName(request.getDoctorName());
        doctorUser.setEmail(request.getEmail());
        doctorUser.setMobileNumber(request.getMobileNumber());

        doctorUser.setUserType(UserType.DOCTOR);
        doctorUser.setStatus(RegistrationStatus.APPROVED);
        doctorUser.setRole(Role.ROLE_USER);
        User savedDoctorUser = userRepository.save(doctorUser);

        LoginCredential credential = new LoginCredential();
        credential.setUser(savedDoctorUser);
        credential.setUsername(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(plainPassword));

        loginCredentialRepository.save(credential);

        Doctor doctor = new Doctor();
        doctor.setDoctorName(request.getDoctorName());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setEmail(request.getEmail());
        doctor.setMobileNumber(request.getMobileNumber());
        doctor.setRegistrationNumber(request.getRegistrationNumber());
        doctor.setSignatureUrl(request.getSignatureUrl());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setDoctorCode(generateUniqueDoctorCode());
        doctor.setStatus(EntityStatus.ACTIVE);
        doctor.setUser(savedDoctorUser);

        if (hospital != null) {
            doctor.setHospital(hospital);
        } else {
            doctor.setClinic(clinic);
        }

        Doctor savedDoctor = doctorRepository.save(doctor);
        syncActiveDoctorCount(savedDoctor);

        allocateDoctorToSubscription(savedDoctor, activeSubs);
        String setupToken = passwordResetTokenService.issue(savedDoctorUser);

        emailService.sendCredentialsEmail(
                request.getEmail(),
                request.getDoctorName(),
                request.getEmail(),
                "Doctor",
                subEndDate,
                UserType.DOCTOR,
                setupToken);

        log.info("Doctor added: {} with code {}", savedDoctor.getDoctorName(), savedDoctor.getDoctorCode());
        return mapToResponse(savedDoctor);
    }

    @Override
    public DoctorResponse getDoctorByUserId(UUID id) {
        Doctor doctor = doctorRepository.findByUserId(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found for userId: " + id));

        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setDoctorName(doctor.getDoctorName());
        response.setDoctorCode(doctor.getDoctorCode());
        response.setSpecialization(doctor.getSpecialization());
        response.setQualification(doctor.getQualification());
        response.setExperienceYears(doctor.getExperienceYears());
        response.setEmail(doctor.getEmail());
        response.setMobileNumber(doctor.getMobileNumber());
        response.setRegistrationNumber(doctor.getRegistrationNumber());
        response.setSignatureUrl(doctor.getSignatureUrl());
        response.setConsultationFee(doctor.getConsultationFee());
        response.setStatus(doctor.getStatus());

        if (doctor.getHospital() != null) {
            response.setHospitalId(doctor.getHospital().getId());
            response.setHospitalName(doctor.getHospital().getHospitalName());
            response.setHospitalAddress(formatAddress(doctor.getHospital().getAddressLine1(), doctor.getHospital().getCity(), doctor.getHospital().getState(), doctor.getHospital().getPincode()));
        }
        if (doctor.getClinic() != null) {
            response.setClinicId(doctor.getClinic().getId());
            response.setClinicName(doctor.getClinic().getClinicName());
            response.setClinicAddress(formatAddress(doctor.getClinic().getAddressLine1(), doctor.getClinic().getCity(), doctor.getClinic().getState(), doctor.getClinic().getPincode()));
        }

        return response;
    }

    @Override
    public DoctorResponse updateDoctor(UUID id, UpdateDoctorRequest request, UUID userId) {
        Doctor doctor = getDoctorAndVerifyAccess(id, userId);

        if (doctor.getUser() != null && !doctor.getUser().getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        EntityStatus oldStatus = doctor.getStatus();
        String oldEmail = doctor.getEmail();
        doctor.setDoctorName(request.getDoctorName());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setEmail(request.getEmail());
        doctor.setMobileNumber(request.getMobileNumber());
        doctor.setRegistrationNumber(request.getRegistrationNumber());
        doctor.setSignatureUrl(request.getSignatureUrl());
        doctor.setConsultationFee(request.getConsultationFee());
        EntityStatus newStatus = request.getStatus();
        doctor.setStatus(newStatus);
        Doctor saved = doctorRepository.save(doctor);

        syncDoctorIdentity(doctor, request, oldEmail);

        if (oldStatus == EntityStatus.ACTIVE && newStatus == EntityStatus.INACTIVE) {
            deallocateDoctor(doctor);
            decrementActiveDoctorCount(doctor);
            setLinkedLoginState(doctor, false);
        } else if (oldStatus == EntityStatus.INACTIVE && newStatus == EntityStatus.ACTIVE) {
            User owner = (doctor.getHospital() != null) ? doctor.getHospital().getOwner()
                    : doctor.getClinic().getOwner();
            UUID ownerId = (owner != null) ? owner.getId() : userId;
            UserType ownerType = (owner != null) ? owner.getUserType() : UserType.HOSPITAL;
            List<UserSubscription> activeSubs = userSubscriptionRepository.findValidByUserIdForUpdate(
                    ownerId, java.time.LocalDate.now());
            int totalAllowedDoctors = activeSubs.stream().mapToInt(UserSubscription::getAllowedDoctors).sum();
            int activeDoctors = getActiveDoctorCountForUser(ownerId, ownerType);

            if (activeDoctors > totalAllowedDoctors) {
                doctor.setStatus(EntityStatus.INACTIVE);
                doctorRepository.save(doctor);
                throw new BusinessException(
                        "Cannot activate doctor: Doctor limit reached (" + totalAllowedDoctors + ").");
            }

            incrementActiveDoctorCount(doctor);
            allocateDoctorToSubscription(doctor, activeSubs);
            setLinkedLoginState(doctor, true);
        }

        log.info("Doctor updated: {}", saved.getDoctorName());
        return mapToResponse(saved);
    }

    @Override
    public void deleteDoctor(UUID id, UUID userId) {
        Doctor doctor = getDoctorAndVerifyAccess(id, userId);
        if (doctor.getStatus() == EntityStatus.ACTIVE) {
            deallocateDoctor(doctor);
        }
        doctor.setStatus(EntityStatus.INACTIVE);
        doctorRepository.save(doctor);
        syncActiveDoctorCount(doctor);
        setLinkedLoginState(doctor, false);
        log.info("Doctor deleted (marked inactive): {}", doctor.getDoctorName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> listDoctors(UUID userId, Long hospitalId, Long clinicId) {
        if (hospitalId != null && clinicId != null) {
            throw new BusinessException("hospitalId and clinicId cannot both be provided");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Doctor> doctors = new ArrayList<>();

        if (hospitalId != null) {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
            verifyHospitalAccess(user, hospital);
            doctors.addAll(doctorRepository.findByHospital(hospital));
        } else if (clinicId != null) {
            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinic", clinicId));
            verifyClinicAccess(user, clinic);
            doctors.addAll(doctorRepository.findByClinic(clinic));
        } else if (user.getUserType() == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserIdOrOwnerId(userId, userId);
            for (Hospital h : hospitals) {
                doctors.addAll(doctorRepository.findByHospital(h));
            }
        } else if (user.getUserType() == UserType.CLINIC) {
            List<Clinic> clinics = clinicRepository.findByUserIdOrOwnerId(userId, userId);
            for (Clinic c : clinics) {
                doctors.addAll(doctorRepository.findByClinic(c));
            }
        } else if (user.getUserType() == UserType.RMO) {
            Rmo rmo = rmoRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("RMO", userId));
            if (rmo.getHospital() != null) {
                doctors.addAll(doctorRepository.findByHospital(rmo.getHospital()));
            } else if (rmo.getClinic() != null) {
                doctors.addAll(doctorRepository.findByClinic(rmo.getClinic()));
            }
        }

        return doctors.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void verifyHospitalAccess(User user, Hospital hospital) {
        UUID userId = user.getId();
        boolean hasAccess = user.getUserType() == UserType.HOSPITAL
                && (userId.equals(hospital.getUser().getId())
                || (hospital.getOwner() != null && userId.equals(hospital.getOwner().getId())));

        if (user.getUserType() == UserType.RMO) {
            hasAccess = rmoRepository.findByUserId(userId)
                    .map(rmo -> rmo.getHospital() != null && hospital.getId().equals(rmo.getHospital().getId()))
                    .orElse(false);
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized hospital access");
        }
    }

    private void verifyClinicAccess(User user, Clinic clinic) {
        UUID userId = user.getId();
        boolean hasAccess = user.getUserType() == UserType.CLINIC
                && (userId.equals(clinic.getUser().getId())
                || (clinic.getOwner() != null && userId.equals(clinic.getOwner().getId())));

        if (user.getUserType() == UserType.RMO) {
            hasAccess = rmoRepository.findByUserId(userId)
                    .map(rmo -> rmo.getClinic() != null && clinic.getId().equals(rmo.getClinic().getId()))
                    .orElse(false);
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized clinic access");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(UUID id, UUID userId) {
        Doctor doctor = getDoctorAndVerifyAccess(id, userId);
        return mapToResponse(doctor);
    }

    private Doctor getDoctorAndVerifyAccess(UUID id, UUID userId) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", id));

        boolean hasAccess = false;
        if (doctor.getUser() != null && doctor.getUser().getId().equals(userId)) {
            hasAccess = true;
        } else if (doctor.getHospital() != null &&
                (doctor.getHospital().getUser().getId().equals(userId)
                        || doctor.getHospital().getOwner().getId().equals(userId))) {
            hasAccess = true;
        } else if (doctor.getClinic() != null &&
                (doctor.getClinic().getUser().getId().equals(userId)
                        || doctor.getClinic().getOwner().getId().equals(userId))) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to doctor resource");
        }
        return doctor;
    }

    private int getActiveDoctorCountForUser(UUID userId, UserType type) {
        if (type == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserIdOrOwnerId(userId, userId);
            return Math.toIntExact(hospitals.stream()
                    .mapToLong(h -> doctorRepository.countByHospitalAndStatus(h, EntityStatus.ACTIVE)).sum());
        } else {
            List<Clinic> clinics = clinicRepository.findByUserIdOrOwnerId(userId, userId);
            return Math.toIntExact(clinics.stream()
                    .mapToLong(c -> doctorRepository.countByClinicAndStatus(c, EntityStatus.ACTIVE)).sum());
        }
    }

    private void allocateDoctorToSubscription(Doctor doctor, List<UserSubscription> activeSubs) {
        for (UserSubscription sub : activeSubs) {
            int usedDoctors = Math.toIntExact(allocationRepository
                    .countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE));
            if (usedDoctors < sub.getAllowedDoctors()) {

                SubscriptionDoctorAllocation allocation = new SubscriptionDoctorAllocation();
                allocation.setUserSubscription(sub);
                allocation.setDoctor(doctor);
                allocation.setAllocationType(
                        usedDoctors < sub.getSubscriptionPackage().getBaseDoctorLimit() ? AllocationType.BASE
                                : AllocationType.ADDON);
                allocation.setStatus(AllocationStatus.ACTIVE);
                allocationRepository.save(allocation);
                sub.setUsedDoctors(usedDoctors + 1);
                userSubscriptionRepository.save(sub);
                return;
            }
        }
        throw new BusinessException("No available subscription slots to allocate the doctor");
    }

    private void deallocateDoctor(Doctor doctor) {
        List<SubscriptionDoctorAllocation> allocations = allocationRepository.findByDoctorAndStatus(doctor,
                AllocationStatus.ACTIVE);
        for (SubscriptionDoctorAllocation alloc : allocations) {
            alloc.setStatus(AllocationStatus.INACTIVE);
            allocationRepository.save(alloc);

            UserSubscription sub = alloc.getUserSubscription();
            int activeAllocations = Math.toIntExact(allocationRepository
                    .countByUserSubscriptionIdAndStatus(sub.getId(), AllocationStatus.ACTIVE));
            sub.setUsedDoctors(activeAllocations);
            userSubscriptionRepository.save(sub);
        }
    }

    private void decrementActiveDoctorCount(Doctor doctor) {
        syncActiveDoctorCount(doctor);
    }

    private void incrementActiveDoctorCount(Doctor doctor) {
        syncActiveDoctorCount(doctor);
    }

    private void syncActiveDoctorCount(Doctor doctor) {
        if (doctor.getHospital() != null) {
            Hospital h = doctor.getHospital();
            h.setActiveDoctorCount(Math.toIntExact(
                    doctorRepository.countByHospitalAndStatus(h, EntityStatus.ACTIVE)));
            hospitalRepository.save(h);
        } else if (doctor.getClinic() != null) {
            Clinic c = doctor.getClinic();
            c.setActiveDoctorCount(Math.toIntExact(
                    doctorRepository.countByClinicAndStatus(c, EntityStatus.ACTIVE)));
            clinicRepository.save(c);
        }
    }

    private void setLinkedLoginState(Doctor doctor, boolean active) {
        if (doctor.getUser() == null) return;
        User linkedUser = doctor.getUser();
        linkedUser.setStatus(active ? RegistrationStatus.APPROVED : RegistrationStatus.INACTIVE);
        userRepository.save(linkedUser);
        loginCredentialRepository.findByUserId(linkedUser.getId()).ifPresent(credential -> {
            credential.setLoginStatus(active ? LoginStatus.ACTIVE : LoginStatus.BLOCKED);
            loginCredentialRepository.save(credential);
        });
        if (!active) loginSessionRepository.expireAllSessionsForUser(linkedUser);
    }

    private void syncDoctorIdentity(Doctor doctor, UpdateDoctorRequest request, String oldEmail) {
        if (doctor.getUser() == null) return;
        User linkedUser = doctor.getUser();
        linkedUser.setFullName(request.getDoctorName());
        linkedUser.setEmail(request.getEmail());
        linkedUser.setMobileNumber(request.getMobileNumber());
        userRepository.save(linkedUser);
        loginCredentialRepository.findByUserId(linkedUser.getId()).ifPresent(credential -> {
            credential.setUsername(request.getEmail());
            loginCredentialRepository.save(credential);
        });
        if (oldEmail != null && !oldEmail.equalsIgnoreCase(request.getEmail())) {
            loginSessionRepository.expireAllSessionsForUser(linkedUser);
        }
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        DoctorResponse res = modelMapper.map(doctor, DoctorResponse.class);
        if (doctor.getHospital() != null) {
            res.setHospitalId(doctor.getHospital().getId());
            res.setHospitalName(doctor.getHospital().getHospitalName());
            res.setHospitalAddress(formatAddress(doctor.getHospital().getAddressLine1(), doctor.getHospital().getCity(), doctor.getHospital().getState(), doctor.getHospital().getPincode()));
        }
        if (doctor.getClinic() != null) {
            res.setClinicId(doctor.getClinic().getId());
            res.setClinicName(doctor.getClinic().getClinicName());
            res.setClinicAddress(formatAddress(doctor.getClinic().getAddressLine1(), doctor.getClinic().getCity(), doctor.getClinic().getState(), doctor.getClinic().getPincode()));
        }
        return res;
    }

    private String formatAddress(String line1, String city, String state, String pin) {
        return java.util.stream.Stream.of(line1, city, state, pin)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String generateUniqueDoctorCode() {
        String code;
        do {
            code = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (doctorRepository.findByDoctorCode(code).isPresent());
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
