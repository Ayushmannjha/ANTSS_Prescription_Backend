package com.antss_prescription.service;

import com.antss_prescription.dto.response.ClinicalRecordResponse;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.prescription.*;
import com.antss_prescription.exception.ForbiddenException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.prescription.*;
import com.antss_prescription.security.AccessControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorHospitalClinicalRecordService {
    private static final String HOSPITAL = "HOSPITAL";

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final AccessControlService accessControl;
    private final DaignosisRepo diagnosisRepo;
    private final CheifComplaintsRepo complaintsRepo;
    private final GeneralExaminationRepo examinationRepo;
    private final PastMedicalHistoryRepo historyRepo;
    private final InvestigationsRepo investigationsRepo;
    private final TestRequestedRepo testRequestedRepo;

    public List<ClinicalRecordResponse> diagnoses(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return diagnosisRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    public List<ClinicalRecordResponse> complaints(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return complaintsRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    public List<ClinicalRecordResponse> examinations(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return examinationRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    public List<ClinicalRecordResponse> histories(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return historyRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    public List<ClinicalRecordResponse> investigations(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return investigationsRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    public List<ClinicalRecordResponse> tests(UUID doctorId, String facilityType, Long facilityId) {
        String type = authorize(doctorId, facilityType, facilityId);
        return testRequestedRepo.findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(doctorId, type, facilityId)
                .stream().map(this::map).toList();
    }

    private String authorize(UUID doctorId, String facilityType, Long facilityId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        var scope = accessControl.currentClinicalScope();
        String type;
        if ("hospitals".equalsIgnoreCase(facilityType)) {
            type = HOSPITAL;
            if (!hospitalRepository.existsById(facilityId)) {
                throw new ResourceNotFoundException("Hospital", facilityId);
            }
            if (!scope.admin() && !scope.hospitalIds().contains(facilityId)) {
                throw new ForbiddenException("You do not have access to this hospital");
            }
        } else {
            type = "CLINIC";
            if (!clinicRepository.existsById(facilityId)) {
                throw new ResourceNotFoundException("Clinic", facilityId);
            }
            if (!scope.admin() && !scope.clinicIds().contains(facilityId)) {
                throw new ForbiddenException("You do not have access to this clinic");
            }
        }
        accessControl.requireDoctorAccess(doctor);
        return type;
    }

    private ClinicalRecordResponse.ClinicalRecordResponseBuilder base(ClinicalAttribution record, int id) {
        Doctor doctor = record.getDoctorReference();
        return ClinicalRecordResponse.builder().id(id)
                .doctorId(doctor == null ? null : doctor.getId())
                .entityType(record.getEntityType()).entityId(record.getEntityId());
    }

    private ClinicalRecordResponse map(Diagnosis value) {
        return base(value, value.getDiagnosisId()).diagnosisName(value.getDiagnosisName())
                .diagnosisCode(value.getDiagnosisCode()).duration(value.getDuration())
                .createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).build();
    }

    private ClinicalRecordResponse map(CheifComplaints value) {
        return base(value, value.getCheifComplaintId()).complaintName(value.getComplaintName())
                .frequency(value.getFrequency()).severity(value.getSev()).duration(value.getDuration())
                .createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).build();
    }

    private ClinicalRecordResponse map(GeneralExamination value) {
        return base(value, value.getGeneralExaminationId())
                .generalExamination(value.getGeneralExamination()).build();
    }

    private ClinicalRecordResponse map(PastMedicalHistory value) {
        return base(value, value.getHistoryId()).allergies(value.getAllergeies())
                .currentMedicine(value.getCurrentMedicine()).medicalHistory(value.getMedicalHistory())
                .createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).build();
    }

    private ClinicalRecordResponse map(Investigations value) {
        return base(value, value.getId()).investigationName(value.getInestigationName()).notes(value.getNotes())
                .createdAt(value.getCreateAt()).updatedAt(value.getUpdatedAt()).build();
    }

    private ClinicalRecordResponse map(TestRequested value) {
        return base(value, value.getId()).testName(value.getTestName()).notes(value.getNotes())
                .createdAt(value.getCreateAt()).updatedAt(value.getUpdatedAt()).build();
    }

}
