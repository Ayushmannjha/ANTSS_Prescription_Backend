package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.CreatePrescriptionRequest;
import com.antss_prescription.dto.response.PrescriptionResponse;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.*;
import com.antss_prescription.service.PrescriptionService;
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
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   DoctorRepository doctorRepository,
                                   HospitalRepository hospitalRepository,
                                   ClinicRepository clinicRepository,
                                   UserRepository userRepository,
                                   ModelMapper modelMapper) {
        this.prescriptionRepository = prescriptionRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.clinicRepository = clinicRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", request.getDoctorId()));

        if (doctor.getStatus() != EntityStatus.ACTIVE) {
            throw new BusinessException("Cannot create prescription for an inactive doctor");
        }

        Prescription prescription = modelMapper.map(request, Prescription.class);
        prescription.setPrescriptionNumber(generateUniquePrescriptionNumber());
        prescription.setDoctor(doctor);

        if (user.getUserType() == UserType.HOSPITAL) {
            Hospital hospital;
            if (request.getHospitalId() == null) {
                List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
                if (hospitals.isEmpty()) throw new BusinessException("Hospital not found for user");
                hospital = hospitals.get(0);
            } else {
                hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hospital", request.getHospitalId()));
            }
            if (!hospital.getUser().getId().equals(userId)) {
                throw new BusinessException("Unauthorized hospital access");
            }

            if (doctor.getHospital() == null || !doctor.getHospital().getId().equals(hospital.getId())) {
                throw new BusinessException("Doctor is not associated with this hospital");
            }
            prescription.setHospital(hospital);
        } else {
            Clinic clinic;
            if (request.getClinicId() == null) {
                List<Clinic> clinics = clinicRepository.findByUserId(userId);
                if (clinics.isEmpty()) throw new BusinessException("Clinic not found for user");
                clinic = clinics.get(0);
            } else {
                clinic = clinicRepository.findById(request.getClinicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));
            }
            if (!clinic.getUser().getId().equals(userId)) {
                throw new BusinessException("Unauthorized clinic access");
            }
            if (doctor.getClinic() == null || !doctor.getClinic().getId().equals(clinic.getId())) {
                throw new BusinessException("Doctor is not associated with this clinic");
            }
            prescription.setClinic(clinic);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription created: {} for patient {}", saved.getPrescriptionNumber(), saved.getPatientName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Prescription> prescriptions = new ArrayList<>();
        if (user.getUserType() == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserId(userId);
            for (Hospital h : hospitals) {
                prescriptions.addAll(prescriptionRepository.findByHospital(h));
            }
        } else {
            List<Clinic> clinics = clinicRepository.findByUserId(userId);
            for (Clinic c : clinics) {
                prescriptions.addAll(prescriptionRepository.findByClinic(c));
            }
        }

        return prescriptions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long id, UUID userId) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", id));

        boolean hasAccess = false;
        if (prescription.getHospital() != null && prescription.getHospital().getUser().getId().equals(userId)) {
            hasAccess = true;
        } else if (prescription.getClinic() != null && prescription.getClinic().getUser().getId().equals(userId)) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new BusinessException("Unauthorized access to prescription resource");
        }

        return mapToResponse(prescription);
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        PrescriptionResponse res = modelMapper.map(p, PrescriptionResponse.class);
        res.setDoctorName(p.getDoctor().getDoctorName());
        res.setDoctorId(p.getDoctor().getId());
        if (p.getHospital() != null) {
            res.setHospitalId(p.getHospital().getId());
        }
        if (p.getClinic() != null) {
            res.setClinicId(p.getClinic().getId());
        }
        return res;
    }

    private String generateUniquePrescriptionNumber() {
        String number;
        do {
            number = "PRES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (prescriptionRepository.findByPrescriptionNumber(number).isPresent());
        return number;
    }
}
