package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.prescription.CheifComplaints;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Diagnosis;
import com.antss_prescription.entity.prescription.GeneralExamination;
import com.antss_prescription.entity.prescription.PastMedicalHistory;
import com.antss_prescription.entity.prescription.Vitals;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.prescription.CheifComplaintsRepo;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.repository.prescription.DaignosisRepo;
import com.antss_prescription.repository.prescription.GeneralExaminationRepo;
import com.antss_prescription.repository.prescription.PastMedicalHistoryRepo;
import com.antss_prescription.repository.prescription.VitalsRepo;
import com.antss_prescription.service.ConsultationService;

import jakarta.transaction.Transactional;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    @Autowired
    private ConsultationRepo consultationRepo;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private CheifComplaintsRepo cheifComplaintsRepository;

    @Autowired
    private GeneralExaminationRepo generalExaminationRepository;

    @Autowired
    private DaignosisRepo diagnosisRepository;

    @Autowired
    private PastMedicalHistoryRepo pastMedicalHistoryRepository;

    @Autowired
    private VitalsRepo vitalsRepository;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ConsultationResponse saveConsultation(Consultation consultation) {

        // Save Chief Complaints
        if (consultation.getCheifComplaints() != null) {
            CheifComplaints complaint = consultation.getCheifComplaints();
            complaint.setComplaintDate(LocalDateTime.now());
            complaint.setCreatedAt(LocalDateTime.now());
            complaint.setUpdatedAt(LocalDateTime.now());
            complaint = cheifComplaintsRepository.save(complaint);
            consultation.setCheifComplaints(complaint);
        }

        // Save General Examination
        if (consultation.getGeneralExamination() != null) {
            GeneralExamination examination = consultation.getGeneralExamination();
            examination = generalExaminationRepository.save(examination);
            consultation.setGeneralExamination(examination);
        }

        // Save Diagnosis
        if (consultation.getDiagnosis() != null) {
            Diagnosis diagnosis = consultation.getDiagnosis();
            diagnosis.setDiagnosisDate(LocalDateTime.now());
            diagnosis.setCreatedAt(LocalDateTime.now());
            diagnosis.setUpdatedAt(LocalDateTime.now());
            diagnosis = diagnosisRepository.save(diagnosis);
            consultation.setDiagnosis(diagnosis);
        }

        // Save Past Medical History
        if (consultation.getPastMedicalHistory() != null) {
            PastMedicalHistory history = consultation.getPastMedicalHistory();
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            history = pastMedicalHistoryRepository.save(history);
            consultation.setPastMedicalHistory(history);
        }

        // Save Vitals
        if (consultation.getVitals() != null) {
            Vitals vitals = consultation.getVitals();
            vitals.setCreatedAt(LocalDateTime.now());
            vitals = vitalsRepository.save(vitals);
            consultation.setVitals(vitals);
        }

        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(consultationRepo.save(consultation));
    }

    // ─────────────────────────────────────────────
    // READ — single
    // ─────────────────────────────────────────────
    @Override
    public ConsultationResponse getConsultationById(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));
        return mapToResponse(consultation);
    }

    // ─────────────────────────────────────────────
    // READ — all
    // ─────────────────────────────────────────────
    @Override
    public List<ConsultationResponse> getAllConsultations() {
        List<ConsultationResponse> responses = new ArrayList<>();
        for (Consultation c : consultationRepo.findAll()) {
            responses.add(mapToResponse(c));
        }
        return responses;
    }

    // ─────────────────────────────────────────────
    // READ — by doctor
    // ─────────────────────────────────────────────
    @Override
    public List<ConsultationResponse> getConsultationsByDoctor(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException(
                        "Doctor not found with id : " + doctorId));
        List<ConsultationResponse> responses = new ArrayList<>();
        for (Consultation c : consultationRepo.findByDoctor(doctor)) {
            responses.add(mapToResponse(c));
        }
        return responses;
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public ConsultationResponse updateConsultation(Integer consultationId,
                                                    Consultation consultation) {
        Consultation existing = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));

        // Update Chief Complaints
        if (consultation.getCheifComplaints() != null) {
            CheifComplaints complaint = consultation.getCheifComplaints();
            complaint.setUpdatedAt(LocalDateTime.now());
            complaint = cheifComplaintsRepository.save(complaint);
            existing.setCheifComplaints(complaint);
        }

        // Update General Examination
        if (consultation.getGeneralExamination() != null) {
            GeneralExamination examination = generalExaminationRepository
                    .save(consultation.getGeneralExamination());
            existing.setGeneralExamination(examination);
        }

        // Update Diagnosis
        if (consultation.getDiagnosis() != null) {
            Diagnosis diagnosis = consultation.getDiagnosis();
            diagnosis.setUpdatedAt(LocalDateTime.now());
            diagnosis = diagnosisRepository.save(diagnosis);
            existing.setDiagnosis(diagnosis);
        }

        // Update Past Medical History
        if (consultation.getPastMedicalHistory() != null) {
            PastMedicalHistory history = consultation.getPastMedicalHistory();
            history.setUpdatedAt(LocalDateTime.now());
            history = pastMedicalHistoryRepository.save(history);
            existing.setPastMedicalHistory(history);
        }

        // Update Vitals
        if (consultation.getVitals() != null) {
            Vitals vitals = vitalsRepository.save(consultation.getVitals());
            existing.setVitals(vitals);
        }

        existing.setConsultationNumber(consultation.getConsultationNumber());
        existing.setDoctor(consultation.getDoctor());
        existing.setPatientRegistration(consultation.getPatientRegistration());
        existing.setPatient(consultation.getPatient());
        existing.setAdvice(consultation.getAdvice());
        existing.setFollowUpDate(consultation.getFollowUpDate());
        existing.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(consultationRepo.save(existing));
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────
    @Override
    public void deleteConsultation(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));
        consultationRepo.delete(consultation);
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private ConsultationResponse mapToResponse(Consultation c) {

        ConsultationResponse response = new ConsultationResponse();
        response.setConsultationId(c.getConsultationId());
        response.setConsultationNumber(c.getConsultationNumber());
        response.setAdvice(c.getAdvice());
        response.setFollowUpDate(c.getFollowUpDate());
        response.setCreatedAt(c.getCreatedAt());
        response.setUpdatedAt(c.getUpdatedAt());

        if (c.getDoctor() != null) {
            response.setDoctorId(c.getDoctor().getId());
            response.setDoctorName(c.getDoctor().getDoctorName());
            response.setDoctorCode(c.getDoctor().getDoctorCode());
            response.setSpecialization(c.getDoctor().getSpecialization());
            response.setQualification(c.getDoctor().getQualification());
        }

        if (c.getPatientRegistration() != null) {
            response.setRegistrationId(c.getPatientRegistration().getRegistrationId());
            response.setRegistrationNumber(c.getPatientRegistration().getRegistrationNumber());
        }

        if (c.getPatient() != null) {
            response.setPatientId(c.getPatient().getPatientId());
            response.setPatientName(c.getPatient().getPatientName());
            response.setMobileNumber(c.getPatient().getMobileNumber());
            response.setGender(c.getPatient().getGender());
            response.setAge(c.getPatient().getAge());
        }

        if (c.getCheifComplaints() != null) {
            response.setCheifComplaintId(c.getCheifComplaints().getCheifComplaintId());
            response.setComplaintName(c.getCheifComplaints().getComplaintName());
            response.setComplaintFrequency(c.getCheifComplaints().getFrequency());
            response.setSeverity(c.getCheifComplaints().getSev());
            response.setComplaintDuration(c.getCheifComplaints().getDuration());
        }

        if (c.getGeneralExamination() != null) {
            response.setGeneralExaminationId(c.getGeneralExamination().getGeneralExaminationId());
            response.setGeneralExamination(c.getGeneralExamination().getGeneralExamination());
        }

        if (c.getDiagnosis() != null) {
            response.setDiagnosisId(c.getDiagnosis().getDiagnosisId());
            response.setDiagnosisName(c.getDiagnosis().getDiagnosisName());
            response.setDiagnosisCode(c.getDiagnosis().getDiagnosisCode());
            response.setDiagnosisDuration(c.getDiagnosis().getDuration());
        }

        if (c.getPastMedicalHistory() != null) {
            response.setHistoryId(c.getPastMedicalHistory().getHistoryId());
            response.setAllergies(c.getPastMedicalHistory().getAllergeies());
            response.setCurrentMedicine(c.getPastMedicalHistory().getCurrentMedicine());
            response.setMedicalHistory(c.getPastMedicalHistory().getMedicalHistory());
        }

        if (c.getVitals() != null) {
            response.setVitalId(c.getVitals().getVitalId());
            response.setHeight(c.getVitals().getHeight());
            response.setWeight(c.getVitals().getWeight());
            response.setTemperature(c.getVitals().getTemprature());
            response.setPulse(c.getVitals().getPulse());
            response.setSpo2(c.getVitals().getSpo2());
            response.setBp(c.getVitals().getBp());
            response.setRespiratoryRate(c.getVitals().getRespiratoryRate());
        }

        return response;
    }
}