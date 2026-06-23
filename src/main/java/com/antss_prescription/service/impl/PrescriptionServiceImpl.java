package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.antss_prescription.dto.request.SavePrescriptionRequest;
import com.antss_prescription.dto.request.UpdatePrescriptionRequest;
import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.response.DetailedPrescriptionResponse;
import com.antss_prescription.dto.response.PrescriptionResponse;
import com.antss_prescription.entity.prescription.CheifComplaints;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Diagnosis;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.GeneralExamination;
import com.antss_prescription.entity.prescription.DiagnosticOrder;
import com.antss_prescription.entity.prescription.PastMedicalHistory;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.PrescriptionMedicines;
import com.antss_prescription.enums.DiagnosticStatus;
import com.antss_prescription.entity.prescription.Vitals;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.repository.prescription.CheifComplaintsRepo;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.repository.prescription.DaignosisRepo;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.GeneralExaminationRepo;
import com.antss_prescription.repository.prescription.DiagnosticOrderRepository;
import com.antss_prescription.repository.prescription.PastMedicalHistoryRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionMedicinesRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.repository.prescription.VitalsRepo;
import com.antss_prescription.service.PrescriptionService;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.exception.ForbiddenException;

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
    private final DiagnosticOrderRepository diagnosticOrderRepository;
    private final DocumentRepo documentRepo;
    private final AccessControlService accessControl;


    @Override
    @Transactional
    public PrescriptionResponse savePrescription(SavePrescriptionRequest req) {

        Consultation consultation;

        if (req.getConsultationId() != null) {

            System.out.println(
                    "==== CONSULTATION IS PRESENT ====");

            consultation = consultationRepository
                    .findById(req.getConsultationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Consultation not found: "
                                    + req.getConsultationId()));

            accessControl.requireConsultationAccess(consultation);
            Doctor currentDoctor = accessControl.requireCurrentDoctorFor(
                    consultation.getPatientRegistration());
            if (consultation.getDoctor() == null
                    || !consultation.getDoctor().getId().equals(currentDoctor.getId())) {
                throw new ForbiddenException("A consultation can only be reused by its assigned doctor");
            }

        } else {

            System.out.println(
                    "==== CONSULTATION IS NOT PRESENT ====");


            PatientRegistration registration = registrationRepository
                    .findById(req.getRegistrationId())
                    .orElseThrow(() -> new RuntimeException(
                            "PatientRegistration not found: "
                                    + req.getRegistrationId()));

            Doctor currentDoctor = accessControl.requireCurrentDoctorFor(registration);

            // =========================
            // Vitals
            // =========================

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

            // =========================
            // =========================
            // Consultation
            // =========================

            consultation = new Consultation();
            consultation.setConsultationNumber(
                    "CONS-" + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase());
            consultation.setPatientRegistration(registration);
            consultation.setPatient(registration.getPatient());
            consultation.setVitals(vitals);
            consultation.setDoctor(currentDoctor);
            consultation.setAdvice(req.getAdvice());
            consultation.setFollowUpDate(req.getFollowUpDate());
            consultation.setCreatedAt(LocalDateTime.now());
            consultation.setUpdatedAt(LocalDateTime.now());

            // =========================
            // Chief Complaints (List)
            // =========================

            if (req.getComplaints() != null) {
                for (SavePrescriptionRequest.ChiefComplaintRequest c : req.getComplaints()) {
                    CheifComplaints complaint = new CheifComplaints();
                    complaint.setComplaintName(c.getComplaintName());
                    complaint.setFrequency(c.getComplaintFrequency());
                    complaint.setSev(c.getSeverity());
                    complaint.setDuration(c.getComplaintDuration());
                    complaint.setComplaintDate(LocalDateTime.now());
                    complaint.setCreatedAt(LocalDateTime.now());
                    complaint.setUpdatedAt(LocalDateTime.now());
                    complaint.setConsultation(consultation);
                    consultation.getCheifComplaints().add(complaint);
                }
            }

            // =========================
            // General Examination (List)
            // =========================

            if (req.getGeneralExaminations() != null) {
                for (String exam : req.getGeneralExaminations()) {
                    GeneralExamination examination = new GeneralExamination();
                    examination.setGeneralExamination(exam);
                    examination.setConsultation(consultation);
                    consultation.getGeneralExaminations().add(examination);
                }
            }

            // =========================
            // Past Medical History (List)
            // =========================

            if (req.getPastMedicalHistories() != null) {
                for (SavePrescriptionRequest.PastMedicalHistoryRequest h : req.getPastMedicalHistories()) {
                    PastMedicalHistory history = new PastMedicalHistory();
                    history.setAllergeies(h.getAllergies());
                    history.setCurrentMedicine(h.getCurrentMedicine());
                    history.setMedicalHistory(h.getMedicalHistory());
                    history.setCreatedAt(LocalDateTime.now());
                    history.setUpdatedAt(LocalDateTime.now());
                    history.setConsultation(consultation);
                    consultation.getPastMedicalHistories().add(history);
                }
            }

            // =========================
            // Diagnosis (List)
            // =========================

            if (req.getDiagnoses() != null) {
                for (SavePrescriptionRequest.DiagnosisRequest d : req.getDiagnoses()) {
                    Diagnosis diagnosis = new Diagnosis();
                    diagnosis.setDiagnosisName(d.getDiagnosisName());
                    diagnosis.setDiagnosisCode(d.getDiagnosisCode());
                    diagnosis.setDuration(d.getDiagnosisDuration());
                    diagnosis.setDiagnosisDate(LocalDateTime.now());
                    diagnosis.setCreatedAt(LocalDateTime.now());
                    diagnosis.setUpdatedAt(LocalDateTime.now());
                    diagnosis.setConsultation(consultation);
                    consultation.getDiagnoses().add(diagnosis);
                }
            }

            // =========================
            // Set Doctor
            // =========================

            consultation = consultationRepository.save(consultation);
        }

        // =========================
        // Save Prescription
        // =========================

        Prescription prescription = new Prescription();
        prescription.setConsultation(consultation);
        prescription.setNotes(req.getNotes());
        prescription.setCreatedAt(LocalDateTime.now());
        prescription = prescriptionRepository.save(prescription);

        // =========================
        // Save Medicines
        // =========================

        List<PrescriptionMedicines> medicines = new ArrayList<>();

        if (req.getMedicines() != null) {

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
        }

        // =========================
        // Save Documents
        // =========================

        if (req.getDocuments() != null) {

            Patient patient = consultation.getPatient();

            for (SavePrescriptionRequest.DocumentRequest d
                    : req.getDocuments()) {

                Document document = new Document();
                document.setFileName(d.getFileName());
                document.setUrl(d.getUrl());
                document.setPatient(patient);
                documentRepo.save(document);
            }
        }

        // =========================
        // Save diagnostic orders
        // =========================

        if (req.getDiagnostics() != null) {
            for (SavePrescriptionRequest.DiagnosticRequest item : req.getDiagnostics()) {
                DiagnosticOrder order = new DiagnosticOrder();
                order.setTestName(item.getTestName());
                order.setNotes(item.getNotes());
                order.setStatus(DiagnosticStatus.REQUESTED);
                order.setPrescription(prescription);
                order.setPatientRegistration(consultation.getPatientRegistration());
                diagnosticOrderRepository.save(order);
            }
        }



        // =========================
        // Build Response
        // =========================

        PatientRegistration registration =
                consultation.getPatientRegistration();

        return buildResponse(
                prescription,
                consultation,
                registration,
                medicines);
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

        accessControl.requirePrescriptionAccess(prescription);

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

        var scope = accessControl.currentClinicalScope();
        List<Prescription> prescriptions = scope.admin()
                ? prescriptionRepository.findAll()
                : prescriptionRepository.findAccessible(scope.hospitalIds(), scope.clinicIds());
        return prescriptions
                .stream()
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration =
                            consultation.getPatientRegistration();
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

        var user = accessControl.currentUser();
        return prescriptionRepository
                .findByConsultation_Patient_PatientId(patientId)
                .stream()
                .filter(prescription -> accessControl.canAccess(prescription, user))
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration =
                            consultation.getPatientRegistration();
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

        PatientRegistration requestedRegistration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Patient registration not found: " + registrationId));
        accessControl.requireRegistrationAccess(requestedRegistration);

        return prescriptionRepository
                .findByConsultation_PatientRegistration_RegistrationId(registrationId)
                .stream()
                .map(prescription -> {
                    Consultation consultation = prescription.getConsultation();
                    PatientRegistration registration =
                            consultation.getPatientRegistration();
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

    accessControl.requirePrescriptionAccess(prescription);

    Consultation consultation = prescription.getConsultation();

    // =========================
    // Update Vitals
    // =========================

    Vitals vitals = consultation.getVitals();
    vitals.setHeight(req.getHeight());
    vitals.setWeight(req.getWeight());
    vitals.setTemprature(req.getTemperature());
    vitals.setPulse(req.getPulse());
    vitals.setSpo2(req.getSpo2());
    vitals.setBp(req.getBp());
    vitals.setRespiratoryRate(req.getRespiratoryRate());
    vitalsRepository.save(vitals);

    // =========================
    // Replace Chief Complaints
    // =========================

    if (req.getComplaints() != null) {
        consultation.getCheifComplaints().clear();
        for (SavePrescriptionRequest.ChiefComplaintRequest c
                : req.getComplaints()) {
            CheifComplaints complaint = new CheifComplaints();
            complaint.setComplaintName(c.getComplaintName());
            complaint.setFrequency(c.getComplaintFrequency());
            complaint.setSev(c.getSeverity());
            complaint.setDuration(c.getComplaintDuration());
            complaint.setComplaintDate(LocalDateTime.now());
            complaint.setCreatedAt(LocalDateTime.now());
            complaint.setUpdatedAt(LocalDateTime.now());
            complaint.setConsultation(consultation);
            consultation.getCheifComplaints().add(complaint);
        }
    }

    // =========================
    // Replace General Examinations
    // =========================

    if (req.getGeneralExaminations() != null) {
        consultation.getGeneralExaminations().clear();
        for (String exam : req.getGeneralExaminations()) {
            GeneralExamination examination = new GeneralExamination();
            examination.setGeneralExamination(exam);
            examination.setConsultation(consultation);
            consultation.getGeneralExaminations().add(examination);
        }
    }

    // =========================
    // Replace Past Medical History
    // =========================

    if (req.getPastMedicalHistories() != null) {
        consultation.getPastMedicalHistories().clear();
        for (SavePrescriptionRequest.PastMedicalHistoryRequest h
                : req.getPastMedicalHistories()) {
            PastMedicalHistory history = new PastMedicalHistory();
            history.setAllergeies(h.getAllergies());
            history.setCurrentMedicine(h.getCurrentMedicine());
            history.setMedicalHistory(h.getMedicalHistory());
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            history.setConsultation(consultation);
            consultation.getPastMedicalHistories().add(history);
        }
    }

    // =========================
    // Replace Diagnosis
    // =========================

    if (req.getDiagnoses() != null) {
        consultation.getDiagnoses().clear();
        for (SavePrescriptionRequest.DiagnosisRequest d
                : req.getDiagnoses()) {
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setDiagnosisName(d.getDiagnosisName());
            diagnosis.setDiagnosisCode(d.getDiagnosisCode());
            diagnosis.setDuration(d.getDiagnosisDuration());
            diagnosis.setDiagnosisDate(LocalDateTime.now());
            diagnosis.setCreatedAt(LocalDateTime.now());
            diagnosis.setUpdatedAt(LocalDateTime.now());
            diagnosis.setConsultation(consultation);
            consultation.getDiagnoses().add(diagnosis);
        }
    }

    // =========================
    // Update Consultation
    // =========================

    consultation.setAdvice(req.getAdvice());
    consultation.setFollowUpDate(req.getFollowUpDate());
    consultation.setUpdatedAt(LocalDateTime.now());
    consultationRepository.save(consultation);

    // =========================
    // Update Prescription Notes
    // =========================

    prescription.setNotes(req.getNotes());
    prescriptionRepository.save(prescription);

    // =========================
    // Replace Medicines
    // =========================

    prescriptionMedicinesRepository.deleteByPrescription(prescription);

    List<PrescriptionMedicines> medicines = new ArrayList<>();

    if (req.getMedicines() != null) {

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
    }

    // =========================
    // Replace Documents
    // =========================

    if (req.getDocuments() != null) {

        Patient patient = consultation.getPatient();

        documentRepo.deleteByPatientPatientId(patient.getPatientId());

        for (SavePrescriptionRequest.DocumentRequest d
                : req.getDocuments()) {

            Document document = new Document();
            document.setFileName(d.getFileName());
            document.setUrl(d.getUrl());
            document.setPatient(patient);
            documentRepo.save(document);
        }
    }

    // =========================
    // Replace diagnostic orders
    // =========================

    diagnosticOrderRepository.deleteByPrescription(prescription);

    if (req.getDiagnostics() != null) {
        for (SavePrescriptionRequest.DiagnosticRequest item : req.getDiagnostics()) {
            DiagnosticOrder order = new DiagnosticOrder();
            order.setTestName(item.getTestName());
            order.setNotes(item.getNotes());
            order.setStatus(DiagnosticStatus.REQUESTED);
            order.setPrescription(prescription);
            order.setPatientRegistration(consultation.getPatientRegistration());
            diagnosticOrderRepository.save(order);
        }
    }



    // =========================
    // Build Response
    // =========================

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

        accessControl.requirePrescriptionAccess(prescription);

        // =========================
        // Delete Children First
        // =========================

        prescriptionMedicinesRepository.deleteByPrescription(prescription);
        diagnosticOrderRepository.deleteByPrescription(prescription);

        // =========================
        // Delete Prescription
        // =========================

        prescriptionRepository.delete(prescription);

        // =========================
        // Delete Consultation Children
        // =========================

        Consultation consultation = prescription.getConsultation();

        if (consultation.getVitals() != null)
            vitalsRepository.delete(consultation.getVitals());

        if (consultation.getCheifComplaints() != null)
            cheifComplaintsRepository.deleteAll(consultation.getCheifComplaints());

        if (consultation.getGeneralExaminations() != null)
            generalExaminationRepository.deleteAll(consultation.getGeneralExaminations());

        if (consultation.getPastMedicalHistories() != null)
            pastMedicalHistoryRepository.deleteAll(consultation.getPastMedicalHistories());

        if (consultation.getDiagnoses() != null)
            diagnosisRepository.deleteAll(consultation.getDiagnoses());

        consultationRepository.delete(consultation);
    }

    // ─────────────────────────────────────────────
    // READ — detailed by prescription ID
    // ─────────────────────────────────────────────

    @Override
    public DetailedPrescriptionResponse getDetailedPrescriptionById(int prescriptionId) {

        Prescription prescription = prescriptionRepository
                .findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException(
                        "Prescription not found: " + prescriptionId));

        accessControl.requirePrescriptionAccess(prescription);

        return buildDetailedResponse(prescription);
    }

    // ─────────────────────────────────────────────
    // READ — detailed by patient ID
    // ─────────────────────────────────────────────

    @Override
    public List<DetailedPrescriptionResponse> getDetailedPrescriptionsByPatientId(
            int patientId) {

        var user = accessControl.currentUser();
        return prescriptionRepository
                .findByConsultation_Patient_PatientId(patientId)
                .stream()
                .filter(prescription -> accessControl.canAccess(prescription, user))
                .map(this::buildDetailedResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // HELPER — build simple response
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

    // ─────────────────────────────────────────────
    // HELPER — build detailed response
    // ─────────────────────────────────────────────

    private DetailedPrescriptionResponse buildDetailedResponse(
            Prescription prescription) {

        Consultation consultation = prescription.getConsultation();
        PatientRegistration registration = consultation.getPatientRegistration();

        // =========================
        // Fetch Medicines
        // =========================

        List<PrescriptionMedicines> medicines =
                prescriptionMedicinesRepository.findByPrescription(prescription);

        // =========================
        // Fetch diagnostic orders
        // =========================

        List<DiagnosticOrder> diagnostics =
                diagnosticOrderRepository.findByPrescription(prescription);

        // =========================
        // Fetch Documents
        // =========================

        List<Document> documents =
                consultation.getPatient() != null
                        ? documentRepo.findByPatientPatientId(
                                consultation.getPatient().getPatientId())
                        : new ArrayList<>();

        // =========================
        // Consultation Response
        // =========================

        ConsultationResponse consultationResponse = new ConsultationResponse();
        consultationResponse.setConsultationId(consultation.getConsultationId());
        consultationResponse.setConsultationNumber(consultation.getConsultationNumber());
        consultationResponse.setAdvice(consultation.getAdvice());
        consultationResponse.setFollowUpDate(consultation.getFollowUpDate());
        consultationResponse.setCreatedAt(consultation.getCreatedAt());
        consultationResponse.setUpdatedAt(consultation.getUpdatedAt());

        // =========================
        // Doctor
        // =========================

        if (consultation.getDoctor() != null) {
            consultationResponse.setDoctorId(consultation.getDoctor().getId());
            consultationResponse.setDoctorName(consultation.getDoctor().getDoctorName());
            consultationResponse.setDoctorCode(consultation.getDoctor().getDoctorCode());
            consultationResponse.setSpecialization(consultation.getDoctor().getSpecialization());
            consultationResponse.setQualification(consultation.getDoctor().getQualification());
        }

        // =========================
        // Registration
        // =========================

        if (registration != null) {
            consultationResponse.setRegistrationId(registration.getRegistrationId());
            consultationResponse.setRegistrationNumber(registration.getRegistrationNumber());
        }

        // =========================
        // Patient
        // =========================

        if (consultation.getPatient() != null) {
            consultationResponse.setPatientId(consultation.getPatient().getPatientId());
            consultationResponse.setPatientName(consultation.getPatient().getPatientName());
            consultationResponse.setMobileNumber(consultation.getPatient().getMobileNumber());
            consultationResponse.setGender(consultation.getPatient().getGender());
            consultationResponse.setAge(consultation.getPatient().getAge());
        }

        // =========================
        // Chief Complaints
        // =========================

        if (consultation.getCheifComplaints() != null && !consultation.getCheifComplaints().isEmpty()) {
            CheifComplaints first = consultation.getCheifComplaints().get(0);
            consultationResponse.setCheifComplaintId(first.getCheifComplaintId());
            consultationResponse.setComplaintName(first.getComplaintName());
            consultationResponse.setComplaintFrequency(first.getFrequency());
            consultationResponse.setSeverity(first.getSev());
            consultationResponse.setComplaintDuration(first.getDuration());

            List<ConsultationResponse.ChiefComplaintResponse> list = consultation.getCheifComplaints().stream()
                .map(comp -> {
                    ConsultationResponse.ChiefComplaintResponse r = new ConsultationResponse.ChiefComplaintResponse();
                    r.setCheifComplaintId(comp.getCheifComplaintId());
                    r.setComplaintName(comp.getComplaintName());
                    r.setComplaintFrequency(comp.getFrequency());
                    r.setSeverity(comp.getSev());
                    r.setComplaintDuration(comp.getDuration());
                    return r;
                }).collect(Collectors.toList());
            consultationResponse.setComplaints(list);
        }

        // =========================
        // General Examination
        // =========================

        if (consultation.getGeneralExaminations() != null && !consultation.getGeneralExaminations().isEmpty()) {
            GeneralExamination first = consultation.getGeneralExaminations().get(0);
            consultationResponse.setGeneralExaminationId(first.getGeneralExaminationId());
            consultationResponse.setGeneralExamination(first.getGeneralExamination());

            List<String> list = consultation.getGeneralExaminations().stream()
                .map(GeneralExamination::getGeneralExamination)
                .collect(Collectors.toList());
            consultationResponse.setGeneralExaminations(list);
        }

        // =========================
        // Diagnosis
        // =========================

        if (consultation.getDiagnoses() != null && !consultation.getDiagnoses().isEmpty()) {
            Diagnosis first = consultation.getDiagnoses().get(0);
            consultationResponse.setDiagnosisId(first.getDiagnosisId());
            consultationResponse.setDiagnosisName(first.getDiagnosisName());
            consultationResponse.setDiagnosisCode(first.getDiagnosisCode());
            consultationResponse.setDiagnosisDuration(first.getDuration());

            List<ConsultationResponse.DiagnosisResponse> list = consultation.getDiagnoses().stream()
                .map(diag -> {
                    ConsultationResponse.DiagnosisResponse r = new ConsultationResponse.DiagnosisResponse();
                    r.setDiagnosisId(diag.getDiagnosisId());
                    r.setDiagnosisName(diag.getDiagnosisName());
                    r.setDiagnosisCode(diag.getDiagnosisCode());
                    r.setDiagnosisDuration(diag.getDuration());
                    return r;
                }).collect(Collectors.toList());
            consultationResponse.setDiagnoses(list);
        }

        // =========================
        // Past Medical History
        // =========================

        if (consultation.getPastMedicalHistories() != null && !consultation.getPastMedicalHistories().isEmpty()) {
            PastMedicalHistory first = consultation.getPastMedicalHistories().get(0);
            consultationResponse.setHistoryId(first.getHistoryId());
            consultationResponse.setAllergies(first.getAllergeies());
            consultationResponse.setCurrentMedicine(first.getCurrentMedicine());
            consultationResponse.setMedicalHistory(first.getMedicalHistory());

            List<ConsultationResponse.PastMedicalHistoryResponse> list = consultation.getPastMedicalHistories().stream()
                .map(hist -> {
                    ConsultationResponse.PastMedicalHistoryResponse r = new ConsultationResponse.PastMedicalHistoryResponse();
                    r.setHistoryId(hist.getHistoryId());
                    r.setAllergies(hist.getAllergeies());
                    r.setCurrentMedicine(hist.getCurrentMedicine());
                    r.setMedicalHistory(hist.getMedicalHistory());
                    return r;
                }).collect(Collectors.toList());
            consultationResponse.setPastMedicalHistories(list);
        }

        // =========================
        // Vitals
        // =========================

        if (consultation.getVitals() != null) {
            consultationResponse.setVitalId(consultation.getVitals().getVitalId());
            consultationResponse.setHeight(consultation.getVitals().getHeight());
            consultationResponse.setWeight(consultation.getVitals().getWeight());
            consultationResponse.setTemperature(consultation.getVitals().getTemprature());
            consultationResponse.setPulse(consultation.getVitals().getPulse());
            consultationResponse.setSpo2(consultation.getVitals().getSpo2());
            consultationResponse.setBp(consultation.getVitals().getBp());
            consultationResponse.setRespiratoryRate(
                    consultation.getVitals().getRespiratoryRate());
        }

        // =========================
        // Medicines
        // =========================

        List<DetailedPrescriptionResponse.MedicineDetailResponse> medicineDetails =

                medicines.stream()
                        .map(m -> DetailedPrescriptionResponse.MedicineDetailResponse
                                .builder()
                                .prescriptionMedicineId(m.getPrescriptionMedicineId())
                                .medicineName(m.getMedicineName())
                                .strength(m.getStrength())
                                .dosage(m.getDosage())
                                .frequency(m.getFrequency())
                                .duration(m.getDuration())
                                .instruction(m.getInstruction())
                                .quantity(m.getQuantity())
                                .build())
                        .collect(Collectors.toList());

        // =========================
        // Diagnostic orders
        // =========================

        List<DetailedPrescriptionResponse.DiagnosticDetailResponse> diagnosticDetails =

                diagnostics.stream()
                        .map(item -> DetailedPrescriptionResponse.DiagnosticDetailResponse
                                .builder()
                                .id(item.getId())
                                .testName(item.getTestName())
                                .notes(item.getNotes())
                                .resultSummary(item.getResultSummary())
                                .status(item.getStatus())
                                .requestedAt(item.getRequestedAt())
                                .completedAt(item.getCompletedAt())
                                .reportDocumentId(item.getReportDocument() == null
                                        ? null : item.getReportDocument().getId())
                                .build())
                        .collect(Collectors.toList());

        // =========================
        // Documents
        // =========================

        List<DetailedPrescriptionResponse.DocumentDetailResponse> documentDetails =

                documents.stream()
                        .map(d -> DetailedPrescriptionResponse.DocumentDetailResponse
                                .builder()
                                .id(d.getId())
                                .fileName(d.getFileName())
                                .url(d.getUrl())
                                .build())
                        .collect(Collectors.toList());

        // =========================
        // Build Final Response
        // =========================

        return DetailedPrescriptionResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .notes(prescription.getNotes())
                .createdAt(prescription.getCreatedAt())
                .consultation(consultationResponse)
                .medicines(medicineDetails)
                .diagnostics(diagnosticDetails)
                .documents(documentDetails)
                .build();
    }
}
