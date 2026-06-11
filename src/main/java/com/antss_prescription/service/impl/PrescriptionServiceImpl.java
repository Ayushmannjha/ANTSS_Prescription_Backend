package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.antss_prescription.dto.request.SavePrescriptionRequest;
import com.antss_prescription.dto.request.UpdatePrescriptionRequest;
import com.antss_prescription.dto.response.PrescriptionResponse;
import com.antss_prescription.entity.prescription.CheifComplaints;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Diagnosis;
import com.antss_prescription.entity.prescription.GeneralExamination;
import com.antss_prescription.entity.prescription.PastMedicalHistory;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.PrescriptionMedicines;
import com.antss_prescription.entity.prescription.Vitals;
import com.antss_prescription.repository.prescription.CheifComplaintsRepo;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.repository.prescription.DaignosisRepo;
import com.antss_prescription.repository.prescription.GeneralExaminationRepo;
import com.antss_prescription.repository.prescription.PastMedicalHistoryRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionMedicinesRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.repository.prescription.VitalsRepo;
import com.antss_prescription.service.PrescriptionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PatientRegistrationRepo registrationRepository;
    private final VitalsRepo vitalsRepository;
    private final CheifComplaintsRepo cheifComplaintsRepository;
    private final GeneralExaminationRepo generalExaminationRepository;
    private final PastMedicalHistoryRepo pastMedicalHistoryRepository;
    private final DaignosisRepo diagnosisRepository;
    private final ConsultationRepo consultationRepository;
    private final PrescriptionRepo prescriptionRepository;
    private final PrescriptionMedicinesRepo prescriptionMedicinesRepository;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────
    @Override
	@Transactional
	public PrescriptionResponse savePrescription(SavePrescriptionRequest req) {

    Consultation consultation;

    if (req.getConsultationId() != null) {
    	System.out.println("Consultation is present");
        // ── Use existing consultation ──
        consultation = consultationRepository.findById(req.getConsultationId())
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found: " + req.getConsultationId()));
    } else {
    	System.out.println("Consultation is not present");
        // ── Create new consultation with all child entities ──
        PatientRegistration registration = registrationRepository
                .findById(req.getRegistrationId())
                .orElseThrow(() -> new RuntimeException(
                        "PatientRegistration not found: " + req.getRegistrationId()));

        Vitals vitals = new Vitals();
        vitals.setHeight(req.getHeight());
        vitals.setWeight(req.getWeight());
        vitals.setTemprature(req.getTemperature());
        vitals.setPulse(req.getPulse());
        vitals.setSpo2(req.getSpo2());
        vitals.setBp(req.getBp());
        vitals.setRespiratoryRate(req.getRespiratoryRate());
        vitals.setCreatedAt(LocalDateTime.now());
        vitals = vitalsRepository.save(vitals);

        CheifComplaints complaint = new CheifComplaints();
        complaint.setComplaintName(req.getComplaintName());
        complaint.setFrequency(req.getComplaintFrequency());
        complaint.setSev(req.getSeverity());
        complaint.setDuration(req.getComplaintDuration());
        complaint.setComplaintDate(LocalDateTime.now());
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        complaint = cheifComplaintsRepository.save(complaint);

        GeneralExamination examination = new GeneralExamination();
        examination.setGeneralExamination(req.getGeneralExamination());
        examination = generalExaminationRepository.save(examination);

        PastMedicalHistory history = new PastMedicalHistory();
        history.setAllergeies(req.getAllergies());
        history.setCurrentMedicine(req.getCurrentMedicine());
        history.setMedicalHistory(req.getMedicalHistory());
        history.setCreatedAt(LocalDateTime.now());
        history.setUpdatedAt(LocalDateTime.now());
        history = pastMedicalHistoryRepository.save(history);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setDiagnosisName(req.getDiagnosisName());
        diagnosis.setDiagnosisCode(req.getDiagnosisCode());
        diagnosis.setDuration(req.getDiagnosisDuration());
        diagnosis.setDiagnosisDate(LocalDateTime.now());
        diagnosis.setCreatedAt(LocalDateTime.now());
        diagnosis.setUpdatedAt(LocalDateTime.now());
        diagnosis = diagnosisRepository.save(diagnosis);

        consultation = new Consultation();
        consultation.setConsultationNumber("CONS-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());
        consultation.setPatientRegistration(registration);
        consultation.setPatient(registration.getPatient());
        consultation.setVitals(vitals);
        consultation.setCheifComplaints(complaint);
        consultation.setGeneralExamination(examination);
        consultation.setPastMedicalHistory(history);
        consultation.setDiagnosis(diagnosis);
        consultation.setAdvice(req.getAdvice());
        consultation.setFollowUpDate(req.getFollowUpDate());
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());
        consultation = consultationRepository.save(consultation);
    }

    // ── Save Prescription ──
    Prescription prescription = new Prescription();
    prescription.setConsultation(consultation);
    prescription.setNotes(req.getNotes());
    prescription.setCreatedAt(LocalDateTime.now());
    prescription = prescriptionRepository.save(prescription);

    // ── Save Medicines ──
    List<PrescriptionMedicines> medicines = new ArrayList<>();
    for (SavePrescriptionRequest.MedicineRequest m : req.getMedicines()) {
        PrescriptionMedicines med = new PrescriptionMedicines();
        med.setPrescription(prescription);
        med.setMedicineName(m.getMedicineName());
        med.setStrength(m.getStrength());
        med.setDosage(m.getDosage());
        med.setFrequency(m.getFrequency());
        med.setDuration(m.getDuration());
        med.setInstruction(m.getInstruction());
        med.setQuantity(m.getQuantity());
        med.setCreatedAt(LocalDateTime.now());
        prescriptionMedicinesRepository.save(med);
        medicines.add(med);
    }

    PatientRegistration registration = consultation.getPatientRegistration();
    return buildResponse(prescription, consultation, registration, medicines);
}

    // ─────────────────────────────────────────────
    // READ — single by prescription ID
    // ─────────────────────────────────────────────
    @Override
    public PrescriptionResponse getPrescriptionById(int prescriptionId) {

        Prescription prescription = prescriptionRepository
                .findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException(
                        "Prescription not found: " + prescriptionId));

        Consultation consultation = prescription.getConsultation();
        PatientRegistration registration = consultation.getPatientRegistration();
        List<PrescriptionMedicines> medicines =
                prescriptionMedicinesRepository.findByPrescription(prescription);

        return buildResponse(prescription, consultation, registration, medicines);
    }

    // ─────────────────────────────────────────────
    // READ — all prescriptions
    // ─────────────────────────────────────────────
    @Override
    public List<PrescriptionResponse> getAllPrescriptions() {

        return prescriptionRepository.findAll()
                .stream()
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration = consultation.getPatientRegistration();
                    List<PrescriptionMedicines> medicines =
                            prescriptionMedicinesRepository.findByPrescription(prescription);
                    return buildResponse(prescription, consultation, registration, medicines);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // READ — by patient ID
    // ─────────────────────────────────────────────
    @Override
    public List<PrescriptionResponse> getPrescriptionsByPatientId(int patientId) {

        return prescriptionRepository.findByConsultation_Patient_PatientId(patientId)
                .stream()
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration = consultation.getPatientRegistration();
                    List<PrescriptionMedicines> medicines =
                            prescriptionMedicinesRepository.findByPrescription(prescription);
                    return buildResponse(prescription, consultation, registration, medicines);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // READ — by registration ID
    // ─────────────────────────────────────────────
    @Override
    public List<PrescriptionResponse> getPrescriptionsByRegistrationId(int registrationId) {

        return prescriptionRepository
                .findByConsultation_PatientRegistration_RegistrationId(registrationId)
                .stream()
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration = consultation.getPatientRegistration();
                    List<PrescriptionMedicines> medicines =
                            prescriptionMedicinesRepository.findByPrescription(prescription);
                    return buildResponse(prescription, consultation, registration, medicines);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public PrescriptionResponse updatePrescription(int prescriptionId,
                                                    UpdatePrescriptionRequest req) {

        Prescription prescription = prescriptionRepository
                .findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException(
                        "Prescription not found: " + prescriptionId));

        Consultation consultation = prescription.getConsultation();

        // Update Vitals
        Vitals vitals = consultation.getVitals();
        vitals.setHeight(req.getHeight());
        vitals.setWeight(req.getWeight());
        vitals.setTemprature(req.getTemperature());
        vitals.setPulse(req.getPulse());
        vitals.setSpo2(req.getSpo2());
        vitals.setBp(req.getBp());
        vitals.setRespiratoryRate(req.getRespiratoryRate());
        vitalsRepository.save(vitals);

        // Update Chief Complaint
        CheifComplaints complaint = consultation.getCheifComplaints();
        complaint.setComplaintName(req.getComplaintName());
        complaint.setFrequency(req.getComplaintFrequency());
        complaint.setSev(req.getSeverity());
        complaint.setDuration(req.getComplaintDuration());
        complaint.setUpdatedAt(LocalDateTime.now());
        cheifComplaintsRepository.save(complaint);

        // Update General Examination
        GeneralExamination examination = consultation.getGeneralExamination();
        examination.setGeneralExamination(req.getGeneralExamination());
        generalExaminationRepository.save(examination);

        // Update Past Medical History
        PastMedicalHistory history = consultation.getPastMedicalHistory();
        history.setAllergeies(req.getAllergies());
        history.setCurrentMedicine(req.getCurrentMedicine());
        history.setMedicalHistory(req.getMedicalHistory());
        history.setUpdatedAt(LocalDateTime.now());
        pastMedicalHistoryRepository.save(history);

        // Update Diagnosis
        Diagnosis diagnosis = consultation.getDiagnosis();
        diagnosis.setDiagnosisName(req.getDiagnosisName());
        diagnosis.setDiagnosisCode(req.getDiagnosisCode());
        diagnosis.setDuration(req.getDiagnosisDuration());
        diagnosis.setUpdatedAt(LocalDateTime.now());
        diagnosisRepository.save(diagnosis);

        // Update Consultation
        consultation.setAdvice(req.getAdvice());
        consultation.setFollowUpDate(req.getFollowUpDate());
        consultation.setUpdatedAt(LocalDateTime.now());
        consultationRepository.save(consultation);

        // Update Prescription notes
        prescription.setNotes(req.getNotes());
        prescriptionRepository.save(prescription);

        // Replace medicines — delete old, insert new
        prescriptionMedicinesRepository.deleteByPrescription(prescription);

        List<PrescriptionMedicines> medicines = new ArrayList<>();
        for (SavePrescriptionRequest.MedicineRequest m : req.getMedicines()) {
            PrescriptionMedicines med = new PrescriptionMedicines();
            med.setPrescription(prescription);
            med.setMedicineName(m.getMedicineName());
            med.setStrength(m.getStrength());
            med.setDosage(m.getDosage());
            med.setFrequency(m.getFrequency());
            med.setDuration(m.getDuration());
            med.setInstruction(m.getInstruction());
            med.setQuantity(m.getQuantity());
            med.setCreatedAt(LocalDateTime.now());
            prescriptionMedicinesRepository.save(med);
            medicines.add(med);
        }

        PatientRegistration registration = consultation.getPatientRegistration();
        return buildResponse(prescription, consultation, registration, medicines);
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public void deletePrescription(int prescriptionId) {

        Prescription prescription = prescriptionRepository
                .findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException(
                        "Prescription not found: " + prescriptionId));

        // Delete medicines first (FK constraint)
        prescriptionMedicinesRepository.deleteByPrescription(prescription);

        // Delete prescription
        prescriptionRepository.delete(prescription);

        // Optionally delete the consultation and its children
        Consultation consultation = prescription.getConsultation();
        vitalsRepository.delete(consultation.getVitals());
        cheifComplaintsRepository.delete(consultation.getCheifComplaints());
        generalExaminationRepository.delete(consultation.getGeneralExamination());
        pastMedicalHistoryRepository.delete(consultation.getPastMedicalHistory());
        diagnosisRepository.delete(consultation.getDiagnosis());
        consultationRepository.delete(consultation);
    }

    // ─────────────────────────────────────────────
    // HELPER — build response
    // ─────────────────────────────────────────────
    private PrescriptionResponse buildResponse(Prescription prescription,
                                                Consultation consultation,
                                                PatientRegistration registration,
                                                List<PrescriptionMedicines> medicines) {
        return PrescriptionResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .consultationId(consultation.getConsultationId())
                .consultationNumber(consultation.getConsultationNumber())
                .patientName(registration.getPatient().getPatientName())
                .notes(prescription.getNotes())
                .createdAt(prescription.getCreatedAt())
                .medicines(medicines.stream()
                        .map(m -> m.getMedicineName() + " " + m.getStrength())
                        .collect(Collectors.toList()))
                .build();
    }
}