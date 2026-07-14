package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreateRmoRequest;
import com.antss_prescription.dto.response.RmoResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.*;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.LoginCredentialRepository;
import com.antss_prescription.repository.RmoRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.service.RmoService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RmoServiceImpl implements RmoService {

    private final RmoRepository rmoRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public RmoResponse addRmo(CreateRmoRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (rmoRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new BusinessException("RMO with employee code '" + request.getEmployeeCode() + "' already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Password is required when creating an RMO");
        }

        Rmo rmo = new Rmo();
        mapRequestFields(request, rmo);
        rmo.setStatus(EntityStatus.ACTIVE);

        User owner;
        if (user.getUserType() == UserType.HOSPITAL) {
            Hospital hospital;
            if (request.getHospitalId() == null) {
                List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
                if (hospitals.isEmpty()) {
                    hospitals = hospitalRepository.findByOwnerId(userId);
                }
                if (hospitals.isEmpty()) throw new BusinessException("Hospital not found for user");
                hospital = hospitals.get(0);
            } else {
                hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hospital", request.getHospitalId()));
                if (!hospital.getUser().getId().equals(userId)
                        && (hospital.getOwner() == null || !hospital.getOwner().getId().equals(userId))) {
                    throw new BusinessException("Unauthorized hospital access");
                }
            }
            rmo.setHospital(hospital);
            rmo.setClinic(null);
            owner = hospital.getOwner() != null ? hospital.getOwner() : hospital.getUser();
        } else {
            Clinic clinic;
            if (request.getClinicId() == null) {
                List<Clinic> clinics = clinicRepository.findByUserId(userId);
                if (clinics.isEmpty()) {
                    clinics = clinicRepository.findByOwnerId(userId);
                }
                if (clinics.isEmpty()) throw new BusinessException("Clinic not found for user");
                clinic = clinics.get(0);
            } else {
                clinic = clinicRepository.findById(request.getClinicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));
                if (!clinic.getUser().getId().equals(userId)
                        && (clinic.getOwner() == null || !clinic.getOwner().getId().equals(userId))) {
                    throw new BusinessException("Unauthorized clinic access");
                }
            }
            rmo.setClinic(clinic);
            rmo.setHospital(null);
            owner = clinic.getOwner() != null ? clinic.getOwner() : clinic.getUser();
        }

        List<UserSubscription> activeSubs = userSubscriptionRepository.findValidByUserId(
                owner.getId(), java.time.LocalDate.now());
        if (activeSubs.isEmpty()) {
            throw new BusinessException("An active paid subscription is required to add an RMO");
        }
        User rmoUser = new User();
        rmoUser.setFullName(request.getRmoName());
        rmoUser.setEmail(request.getEmail());
        rmoUser.setMobileNumber(request.getMobileNumber());
        rmoUser.setUserType(UserType.RMO);
        rmoUser.setStatus(RegistrationStatus.APPROVED);
        rmoUser.setRole(Role.ROLE_USER);
        User savedRmoUser = userRepository.save(rmoUser);

        LoginCredential credential = new LoginCredential();
        credential.setUser(savedRmoUser);
        credential.setUsername(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        loginCredentialRepository.save(credential);

        rmo.setUser(savedRmoUser);

        Rmo saved = rmoRepository.save(rmo);

        log.info("Rmo created: {}", saved.getRmoName());
        return mapToResponse(saved);
    }

    @Override
    public RmoResponse updateRmo(UUID id, CreateRmoRequest request, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);

        if (!rmo.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        mapRequestFields(request, rmo);
        syncRmoUser(rmo, request);

        Rmo saved = rmoRepository.save(rmo);
        log.info("Rmo updated: {}", saved.getRmoName());
        return mapToResponse(saved);
    }

    @Override
    public void deleteRmo(UUID id, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);
        rmo.setStatus(EntityStatus.INACTIVE);
        if (rmo.getUser() != null) {
            rmo.getUser().setStatus(RegistrationStatus.INACTIVE);
            userRepository.save(rmo.getUser());
            loginCredentialRepository.findByUserId(rmo.getUser().getId()).ifPresent(credential -> {
                credential.setLoginStatus(LoginStatus.BLOCKED);
                loginCredentialRepository.save(credential);
            });
            loginSessionRepository.expireAllSessionsForUser(rmo.getUser());
        }
        rmoRepository.save(rmo);
        log.info("Rmo marked inactive: {}", rmo.getRmoName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RmoResponse> listRmos(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Rmo> rmos = new ArrayList<>();
        if (user.getUserType() == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserIdOrOwnerId(userId, userId);
            for (Hospital h : hospitals) {
                rmos.addAll(rmoRepository.findByHospital(h));
            }
        } else if (user.getUserType() == UserType.RMO) {
            rmoRepository.findByUserId(userId).ifPresent(rmos::add);
        } else {
            List<Clinic> clinics = clinicRepository.findByUserIdOrOwnerId(userId, userId);
            for (Clinic c : clinics) {
                rmos.addAll(rmoRepository.findByClinic(c));
            }
        }

        return rmos.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RmoResponse getRmoById(UUID id, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);
        return mapToResponse(rmo);
    }

    private Rmo getRmoAndVerifyAccess(UUID id, UUID userId) {
        Rmo rmo = rmoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rmo", id));

        boolean hasAccess = false;
        if (rmo.getUser() != null && rmo.getUser().getId().equals(userId)) {
            hasAccess = true;
        } else if (rmo.getHospital() != null &&
                (rmo.getHospital().getUser().getId().equals(userId)
                        || (rmo.getHospital().getOwner() != null
                                && rmo.getHospital().getOwner().getId().equals(userId)))) {
            hasAccess = true;
        } else if (rmo.getClinic() != null &&
                (rmo.getClinic().getUser().getId().equals(userId)
                        || (rmo.getClinic().getOwner() != null
                                && rmo.getClinic().getOwner().getId().equals(userId)))) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to RMO resource");
        }
        return rmo;
    }

    private void syncRmoUser(Rmo rmo, CreateRmoRequest request) {
        if (rmo.getUser() == null) {
            return;
        }

        User rmoUser = rmo.getUser();
        rmoUser.setFullName(request.getRmoName());
        rmoUser.setEmail(request.getEmail());
        rmoUser.setMobileNumber(request.getMobileNumber());
        userRepository.save(rmoUser);

        loginCredentialRepository.findByUserId(rmoUser.getId()).ifPresent(credential -> {
            credential.setUsername(request.getEmail());
            loginCredentialRepository.save(credential);
        });
    }

    private void mapRequestFields(CreateRmoRequest request, Rmo rmo) {
        rmo.setRmoName(request.getRmoName());
        rmo.setEmail(request.getEmail());
        rmo.setMobileNumber(request.getMobileNumber());
        rmo.setEmployeeCode(request.getEmployeeCode());
        rmo.setRole(request.getRole());
    }

    private RmoResponse mapToResponse(Rmo rmo) {
        RmoResponse response = modelMapper.map(rmo, RmoResponse.class);
        if (rmo.getUser() != null) {
            response.setUserId(rmo.getUser().getId());
        }
        if (rmo.getHospital() != null) {
            response.setHospitalId(rmo.getHospital().getId());
        }
        if (rmo.getClinic() != null) {
            response.setClinicId(rmo.getClinic().getId());
        }
        return response;
    }

}
