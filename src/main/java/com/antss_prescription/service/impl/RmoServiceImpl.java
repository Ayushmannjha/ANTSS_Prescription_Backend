package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreateRmoRequest;
import com.antss_prescription.dto.response.RmoResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.*;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.RmoRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.RmoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class RmoServiceImpl implements RmoService {

    private final RmoRepository rmoRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public RmoServiceImpl(RmoRepository rmoRepository,
                          HospitalRepository hospitalRepository,
                          ClinicRepository clinicRepository,
                          UserRepository userRepository,
                          ModelMapper modelMapper) {
        this.rmoRepository = rmoRepository;
        this.hospitalRepository = hospitalRepository;
        this.clinicRepository = clinicRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public RmoResponse addRmo(CreateRmoRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (rmoRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new BusinessException("RMO with employee code '" + request.getEmployeeCode() + "' already exists");
        }

        Rmo rmo = modelMapper.map(request, Rmo.class);
        rmo.setStatus(EntityStatus.ACTIVE);

        if (user.getUserType() == UserType.HOSPITAL) {
            Hospital hospital;
            if (request.getHospitalId() == null) {
                List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
                if (hospitals.isEmpty()) throw new BusinessException("Hospital not found for user");
                hospital = hospitals.get(0);
            } else {
                hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hospital", request.getHospitalId()));
                if (!hospital.getUser().getId().equals(userId)) {
                    throw new BusinessException("Unauthorized hospital access");
                }
            }
            rmo.setHospital(hospital);
        } else {
            Clinic clinic;
            if (request.getClinicId() == null) {
                List<Clinic> clinics = clinicRepository.findByUserId(userId);
                if (clinics.isEmpty()) throw new BusinessException("Clinic not found for user");
                clinic = clinics.get(0);
            } else {
                clinic = clinicRepository.findById(request.getClinicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));
                if (!clinic.getUser().getId().equals(userId)) {
                    throw new BusinessException("Unauthorized clinic access");
                }
            }
            rmo.setClinic(clinic);
        }

        Rmo saved = rmoRepository.save(rmo);
        log.info("Rmo created: {}", saved.getRmoName());
        return modelMapper.map(saved, RmoResponse.class);
    }

    @Override
    public RmoResponse updateRmo(UUID id, CreateRmoRequest request, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);

        modelMapper.map(request, rmo);

        Rmo saved = rmoRepository.save(rmo);
        log.info("Rmo updated: {}", saved.getRmoName());
        return modelMapper.map(saved, RmoResponse.class);
    }

    @Override
    public void deleteRmo(UUID id, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);
        rmo.setStatus(EntityStatus.INACTIVE);
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
            List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
            for (Hospital h : hospitals) {
                rmos.addAll(rmoRepository.findByHospital(h));
            }
        } else {
            List<Clinic> clinics = clinicRepository.findByUserId(userId);
            for (Clinic c : clinics) {
                rmos.addAll(rmoRepository.findByClinic(c));
            }
        }

        return rmos.stream()
                .map(rmo -> modelMapper.map(rmo, RmoResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RmoResponse getRmoById(UUID id, UUID userId) {
        Rmo rmo = getRmoAndVerifyAccess(id, userId);
        return modelMapper.map(rmo, RmoResponse.class);
    }

    private Rmo getRmoAndVerifyAccess(UUID id, UUID userId) {
        Rmo rmo = rmoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rmo", id));

        boolean hasAccess = false;
        if (rmo.getHospital() != null && rmo.getHospital().getUser().getId().equals(userId)) {
            hasAccess = true;
        } else if (rmo.getClinic() != null && rmo.getClinic().getUser().getId().equals(userId)) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to RMO resource");
        }
        return rmo;
    }
}
