package com.antss_prescription.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.antss_prescription.dto.request.CreateConsultRequestDto;
import com.antss_prescription.dto.request.VitalsRequestDto;
import com.antss_prescription.dto.response.ConsultationBillResponse;
import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.response.DoctorOptionResponseDto;
import com.antss_prescription.dto.response.VitalsResponseDto;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Rmo;
import com.antss_prescription.entity.prescription.CheifComplaints;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Diagnosis;
import com.antss_prescription.entity.prescription.GeneralExamination;
import com.antss_prescription.entity.prescription.PastMedicalHistory;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Vitals;
import com.antss_prescription.enums.ConsultationPriority;
import com.antss_prescription.enums.ConsultationStatus;
import com.antss_prescription.enums.DiscountPolicy;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.prescription.ConsultationBillRepository;
import com.antss_prescription.repository.prescription.CheifComplaintsRepo;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.repository.prescription.DaignosisRepo;
import com.antss_prescription.repository.prescription.GeneralExaminationRepo;
import com.antss_prescription.repository.prescription.PastMedicalHistoryRepo;
import com.antss_prescription.repository.prescription.VitalsRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.exception.ConflictException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.ConsultationBillService;
import com.antss_prescription.service.ConsultationService;
import com.antss_prescription.service.ClinicalAttributionService;
import com.antss_prescription.websocket.ConsultationRequestWebSocketHandler;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepo consultationRepo;
    private final DoctorRepository doctorRepository;
    private final CheifComplaintsRepo cheifComplaintsRepository;
    private final GeneralExaminationRepo generalExaminationRepository;
    private final DaignosisRepo diagnosisRepository;
    private final PastMedicalHistoryRepo pastMedicalHistoryRepository;
    private final VitalsRepo vitalsRepository;
    private final PatientRegistrationRepo registrationRepository;
    private final AccessControlService accessControl;
    private final PrescriptionRepo prescriptionRepository;
    private final ClinicalAttributionService clinicalAttributionService;
    private final ConsultationRequestWebSocketHandler consultationRequestWebSocketHandler;
    private final ConsultationBillService consultationBillService;
    private final ConsultationBillRepository consultationBillRepository;


    @Override
    @Transactional
    public ConsultationResponse saveConsultation(
            Consultation consultation,
            DiscountPolicy discountPolicy,
            BigDecimal discountValue) {

        resolveAndAuthorizeRelationships(consultation);

        // Save Chief Complaints
        if (consultation.getCheifComplaints() != null) {
            for (CheifComplaints complaint : consultation.getCheifComplaints()) {
                clinicalAttributionService.apply(complaint, consultation.getDoctor(), consultation.getPatientRegistration());
                complaint.setComplaintDate(LocalDateTime.now());
                complaint.setCreatedAt(LocalDateTime.now());
                complaint.setUpdatedAt(LocalDateTime.now());
                complaint.setConsultation(consultation);
            }
        }

        // Save General Examination
        if (consultation.getGeneralExaminations() != null) {
            for (GeneralExamination examination : consultation.getGeneralExaminations()) {
                clinicalAttributionService.apply(examination, consultation.getDoctor(), consultation.getPatientRegistration());
                examination.setConsultation(consultation);
            }
        }

        // Save Diagnosis
        if (consultation.getDiagnoses() != null) {
            for (Diagnosis diagnosis : consultation.getDiagnoses()) {
                clinicalAttributionService.apply(diagnosis, consultation.getDoctor(), consultation.getPatientRegistration());
                diagnosis.setDiagnosisDate(LocalDateTime.now());
                diagnosis.setCreatedAt(LocalDateTime.now());
                diagnosis.setUpdatedAt(LocalDateTime.now());
                diagnosis.setConsultation(consultation);
            }
        }

        // Save Past Medical History
        if (consultation.getPastMedicalHistories() != null) {
            for (PastMedicalHistory history : consultation.getPastMedicalHistories()) {
                clinicalAttributionService.apply(history, consultation.getDoctor(), consultation.getPatientRegistration());
                history.setCreatedAt(LocalDateTime.now());
                history.setUpdatedAt(LocalDateTime.now());
                history.setConsultation(consultation);
            }
        }

        if (consultation.getVitals() != null) {
            Vitals vitals = consultation.getVitals();
            vitals.setCreatedAt(LocalDateTime.now());
            vitals = vitalsRepository.save(vitals);
            consultation.setVitals(vitals);
        }

        if (consultation.getStatus() == null) {
            consultation.setStatus(ConsultationStatus.IN_PROGRESS);
        }
        if (consultation.getAcceptedAt() == null && consultation.getStatus() == ConsultationStatus.IN_PROGRESS) {
            consultation.setAcceptedAt(LocalDateTime.now());
        }
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());

        Consultation saved = consultationRepo.save(consultation);
        consultationBillService.generateBill(saved, discountPolicy, discountValue);
        return mapToResponse(saved);
    }

    @Override
    public ConsultationResponse getConsultationById(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));
        accessControl.requireConsultationAccess(consultation);
        return mapToResponse(consultation);
    }

    @Override
    public List<ConsultationResponse> getAllConsultations() {
        var scope = accessControl.currentClinicalScope();
        List<Consultation> consultations = scope.admin()
                ? consultationRepo.findAll()
                : consultationRepo.findAccessible(scope.hospitalIds(), scope.clinicIds());
        return consultations.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<ConsultationResponse> getConsultationsByDoctor(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
        accessControl.requireDoctorAccess(doctor);
        List<ConsultationResponse> all = consultationRepo
                .findByDoctorIdOrderByCreatedAtDesc(doctorId) // or your existing fetch method
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Keep only the latest consultation per patient
        Map<Long, ConsultationResponse> latestPerPatient = new LinkedHashMap<>();
        for (ConsultationResponse response : all) {
            latestPerPatient.putIfAbsent((long) response.getPatientId(), response);
        }

        return new ArrayList<>(latestPerPatient.values());
    }

    @Override
    public List<ConsultationResponse> getMyConsultationRequests() {
        Doctor doctor = accessControl.requireCurrentDoctor();
        return consultationRepo.findDoctorDashboardConsultations(
                        doctor.getId(),
                        List.of(ConsultationStatus.REQUESTED, ConsultationStatus.IN_PROGRESS))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<DoctorOptionResponseDto> getAvailableDoctorsForRegistration(Integer registrationId) {
        PatientRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Patient registration not found"));
        accessControl.requireRegistrationAccess(registration);

        List<Doctor> doctors = registration.getHospital() != null
                ? doctorRepository.findByHospitalAndStatus(registration.getHospital(), EntityStatus.ACTIVE)
                : doctorRepository.findByClinicAndStatus(registration.getClinic(), EntityStatus.ACTIVE);
        return doctors.stream().map(this::mapDoctorOption).toList();
    }

    @Override
    @Transactional
    public VitalsResponseDto saveVitals(Integer registrationId, VitalsRequestDto request) {
        Rmo rmo = accessControl.requireCurrentRmo();
        PatientRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Patient registration not found"));
        accessControl.requireRegistrationAccess(registration);
        requireRmoForRegistration(rmo, registration);

        Vitals vitals = new Vitals();
        applyVitals(vitals, request);
        vitals.setPatientRegistration(registration);
        vitals.setRecordedBy(rmo);
        return mapVitals(vitalsRepository.save(vitals));
    }

    @Override
    @Transactional
    public ConsultationResponse createConsultRequest(CreateConsultRequestDto request) {
        Rmo rmo = accessControl.requireCurrentRmo();
       
        PatientRegistration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new RuntimeException("Patient registration not found"));
        accessControl.requireRegistrationAccess(registration);
        requireRmoForRegistration(rmo, registration);

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        accessControl.requireDoctorForRegistration(doctor, registration);

        Consultation consultation = new Consultation();
        consultation.setPatientRegistration(registration);
        consultation.setDoctor(doctor);
        consultation.setStatus(ConsultationStatus.REQUESTED);
        consultation.setPriority(request.getPriority() == null ? ConsultationPriority.ROUTINE : request.getPriority());
        consultation.setConsultReason(request.getConsultReason());
        consultation.setRequestedBy(rmo);
        consultation.setRequestedAt(LocalDateTime.now());
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());

        if (request.getVitalId() != null) {
            Vitals vitals = vitalsRepository.findById(request.getVitalId())
                    .orElseThrow(() -> new RuntimeException("Vitals not found"));
            if (vitals.getPatientRegistration() == null
                    || vitals.getPatientRegistration().getRegistrationId() != registration.getRegistrationId()) {
                throw new ConflictException("Vitals do not belong to this patient registration");
            }
            consultation.setVitals(vitals);
        } else {
            vitalsRepository.findByPatientRegistrationRegistrationIdOrderByCreatedAtDesc(registration.getRegistrationId())
                    .stream()
                    .findFirst()
                    .ifPresent(consultation::setVitals);
        }

        Consultation saved = consultationRepo.save(consultation);
        consultationBillService.generateBill(saved, request.getDiscountPolicy(), request.getDiscountValue());
        consultationRequestWebSocketHandler.publishConsultationRequestCreated(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ConsultationResponse startConsultation(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("Consultation not found with id : " + consultationId));
        Doctor currentDoctor = accessControl.requireCurrentDoctor();
        requireAssignedDoctor(consultation, currentDoctor);

        if (consultation.getStatus() == null || consultation.getStatus() == ConsultationStatus.REQUESTED) {
            consultation.setStatus(ConsultationStatus.IN_PROGRESS);
            consultation.setAcceptedAt(LocalDateTime.now());
            consultation.setUpdatedAt(LocalDateTime.now());
        }
        return mapToResponse(consultationRepo.save(consultation));
    }

    @Override
    @Transactional
    public ConsultationResponse completeConsultation(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("Consultation not found with id : " + consultationId));
        Doctor currentDoctor = accessControl.requireCurrentDoctor();
        requireAssignedDoctor(consultation, currentDoctor);

        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setCompletedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(consultationRepo.save(consultation));
    }

    @Override
    @Transactional
    public ConsultationResponse updateConsultation(Integer consultationId,
            Consultation consultation) {
        Consultation existing = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));

        accessControl.requireConsultationAccess(existing);
        resolveAndAuthorizeRelationships(consultation);

        // Update Chief Complaints
        if (consultation.getCheifComplaints() != null) {
            existing.getCheifComplaints().clear();
            for (CheifComplaints complaint : consultation.getCheifComplaints()) {
                clinicalAttributionService.apply(complaint, consultation.getDoctor(), consultation.getPatientRegistration());
                if (complaint.getCreatedAt() == null) {
                    complaint.setCreatedAt(LocalDateTime.now());
                    complaint.setComplaintDate(LocalDateTime.now());
                }
                complaint.setUpdatedAt(LocalDateTime.now());
                complaint.setConsultation(existing);
                existing.getCheifComplaints().add(complaint);
            }
        }

        // Update General Examination
        if (consultation.getGeneralExaminations() != null) {
            existing.getGeneralExaminations().clear();
            for (GeneralExamination exam : consultation.getGeneralExaminations()) {
                clinicalAttributionService.apply(exam, consultation.getDoctor(), consultation.getPatientRegistration());
                exam.setConsultation(existing);
                existing.getGeneralExaminations().add(exam);
            }
        }

        // Update Diagnosis
        if (consultation.getDiagnoses() != null) {
            existing.getDiagnoses().clear();
            for (Diagnosis diagnosis : consultation.getDiagnoses()) {
                clinicalAttributionService.apply(diagnosis, consultation.getDoctor(), consultation.getPatientRegistration());
                if (diagnosis.getCreatedAt() == null) {
                    diagnosis.setCreatedAt(LocalDateTime.now());
                    diagnosis.setDiagnosisDate(LocalDateTime.now());
                }
                diagnosis.setUpdatedAt(LocalDateTime.now());
                diagnosis.setConsultation(existing);
                existing.getDiagnoses().add(diagnosis);
            }
        }

        // Update Past Medical History
        if (consultation.getPastMedicalHistories() != null) {
            existing.getPastMedicalHistories().clear();
            for (PastMedicalHistory history : consultation.getPastMedicalHistories()) {
                clinicalAttributionService.apply(history, consultation.getDoctor(), consultation.getPatientRegistration());
                if (history.getCreatedAt() == null) {
                    history.setCreatedAt(LocalDateTime.now());
                }
                history.setUpdatedAt(LocalDateTime.now());
                history.setConsultation(existing);
                existing.getPastMedicalHistories().add(history);
            }
        }

        // Update Vitals
        if (consultation.getVitals() != null) {
            Vitals vitals = vitalsRepository.save(consultation.getVitals());
            existing.setVitals(vitals);
        }

        existing.setConsultationNumber(consultation.getConsultationNumber());
        existing.setDoctor(consultation.getDoctor());
        existing.setPatientRegistration(consultation.getPatientRegistration());
        existing.setAdvice(consultation.getAdvice());
        existing.setFollowUpDate(consultation.getFollowUpDate());
        existing.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(consultationRepo.save(existing));
    }

    @Override
    @Transactional
    public ConsultationResponse updateVitals(Integer consultationId, Vitals requestVitals) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", consultationId));
        accessControl.requireConsultationAccess(consultation);

        Vitals vitals = consultation.getVitals();
        if (vitals == null) {
            vitals = new Vitals();
            vitals.setCreatedAt(LocalDateTime.now());
        }

        vitals.setHeight(requestVitals.getHeight());
        vitals.setWeight(requestVitals.getWeight());
        vitals.setTemprature(requestVitals.getTemprature());
        vitals.setPulse(requestVitals.getPulse());
        vitals.setSpo2(requestVitals.getSpo2());
        vitals.setBp(requestVitals.getBp());
        vitals.setRespiratoryRate(requestVitals.getRespiratoryRate());

        consultation.setVitals(vitalsRepository.save(vitals));
        consultation.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(consultationRepo.save(consultation));
    }

    @Override
    public void deleteConsultation(Integer consultationId) {
        Consultation consultation = consultationRepo.findById(consultationId)
                .orElseThrow(() -> new RuntimeException(
                        "Consultation not found with id : " + consultationId));
        accessControl.requireConsultationAccess(consultation);
        if (prescriptionRepository.existsByConsultation(consultation)) {
            throw new ConflictException("Consultation cannot be deleted while prescriptions exist");
        }
        consultationRepo.delete(consultation);
    }


    private ConsultationResponse mapToResponse(Consultation c) {

        ConsultationResponse response = new ConsultationResponse();
        response.setConsultationId(c.getConsultationId());
        response.setConsultationNumber(c.getConsultationNumber());
        response.setAdvice(c.getAdvice());
        response.setFollowUpDate(c.getFollowUpDate());
        response.setCreatedAt(c.getCreatedAt());
        response.setUpdatedAt(c.getUpdatedAt());
        response.setStatus(c.getStatus());
        response.setPriority(c.getPriority());
        response.setConsultReason(c.getConsultReason());
        response.setRequestedAt(c.getRequestedAt());
        response.setAcceptedAt(c.getAcceptedAt());
        response.setCompletedAt(c.getCompletedAt());
        response.setCancelledAt(c.getCancelledAt());

        if (c.getRequestedBy() != null) {
            response.setRequestedByRmoId(c.getRequestedBy().getId());
            response.setRequestedByRmoName(c.getRequestedBy().getRmoName());
        }

        if (c.getDoctor() != null) {
            response.setDoctorId(c.getDoctor().getId());
            response.setDoctorName(c.getDoctor().getDoctorName());
            response.setDoctorCode(c.getDoctor().getDoctorCode());
            response.setSpecialization(c.getDoctor().getSpecialization());
            response.setQualification(c.getDoctor().getQualification());
            response.setDoctorRegistrationNo(c.getDoctor().getRegistrationNumber());
            response.setDoctorSignatureUrl(c.getDoctor().getSignatureUrl());

            if (c.getDoctor().getClinic() != null) {
                response.setClinicId(c.getDoctor().getClinic().getId());
                response.setClinicName(c.getDoctor().getClinic().getClinicName());
                response.setClinicPhone(c.getDoctor().getClinic().getMobileNumber());
                response.setClinicAddress(formatAddress(
                        c.getDoctor().getClinic().getAddressLine1(),
                        c.getDoctor().getClinic().getCity(),
                        c.getDoctor().getClinic().getState(),
                        c.getDoctor().getClinic().getPincode()));
            }
            if (c.getDoctor().getHospital() != null) {
                response.setHospitalId(c.getDoctor().getHospital().getId());
                response.setHospitalName(c.getDoctor().getHospital().getHospitalName());
                response.setHospitalPhone(c.getDoctor().getHospital().getMobileNumber());
                response.setHospitalAddress(formatAddress(
                        c.getDoctor().getHospital().getAddressLine1(),
                        c.getDoctor().getHospital().getCity(),
                        c.getDoctor().getHospital().getState(),
                        c.getDoctor().getHospital().getPincode()));
            }
        }

        if (c.getPatientRegistration() != null) {
            response.setRegistrationId(c.getPatientRegistration().getRegistrationId());
            response.setRegistrationNumber(c.getPatientRegistration().getRegistrationNumber());
            response.setPatientId(c.getPatientRegistration().getRegistrationId());
            response.setPatientName(c.getPatientRegistration().getPatientName());
            response.setMobileNumber(c.getPatientRegistration().getMobileNumber());
            response.setGender(c.getPatientRegistration().getGender());
            response.setAge(c.getPatientRegistration().getAge());
            response.setPatientAddress(formatAddress(
                    c.getPatientRegistration().getAddress(),
                    c.getPatientRegistration().getCity(),
                    c.getPatientRegistration().getState(),
                    c.getPatientRegistration().getPincode()));
        }

        if (c.getCheifComplaints() != null && !c.getCheifComplaints().isEmpty()) {
            CheifComplaints first = c.getCheifComplaints().get(0);
            response.setCheifComplaintId(first.getCheifComplaintId());
            response.setComplaintName(first.getComplaintName());
            response.setComplaintFrequency(first.getFrequency());
            response.setSeverity(first.getSev());
            response.setComplaintDuration(first.getDuration());

            List<ConsultationResponse.ChiefComplaintResponse> list = c.getCheifComplaints().stream()
                .map(comp -> {
                    ConsultationResponse.ChiefComplaintResponse r = new ConsultationResponse.ChiefComplaintResponse();
                    r.setCheifComplaintId(comp.getCheifComplaintId());
                    r.setComplaintName(comp.getComplaintName());
                    r.setComplaintFrequency(comp.getFrequency());
                    r.setSeverity(comp.getSev());
                    r.setComplaintDuration(comp.getDuration());
                    return r;
                }).collect(Collectors.toList());
            response.setComplaints(list);
        }

        if (c.getGeneralExaminations() != null && !c.getGeneralExaminations().isEmpty()) {
            GeneralExamination first = c.getGeneralExaminations().get(0);
            response.setGeneralExaminationId(first.getGeneralExaminationId());
            response.setGeneralExamination(first.getGeneralExamination());

            List<String> list = c.getGeneralExaminations().stream()
                .map(GeneralExamination::getGeneralExamination)
                .collect(Collectors.toList());
            response.setGeneralExaminations(list);
        }

        if (c.getDiagnoses() != null && !c.getDiagnoses().isEmpty()) {
            Diagnosis first = c.getDiagnoses().get(0);
            response.setDiagnosisId(first.getDiagnosisId());
            response.setDiagnosisName(first.getDiagnosisName());
            response.setDiagnosisCode(first.getDiagnosisCode());
            response.setDiagnosisDuration(first.getDuration());

            List<ConsultationResponse.DiagnosisResponse> list = c.getDiagnoses().stream()
                .map(diag -> {
                    ConsultationResponse.DiagnosisResponse r = new ConsultationResponse.DiagnosisResponse();
                    r.setDiagnosisId(diag.getDiagnosisId());
                    r.setDiagnosisName(diag.getDiagnosisName());
                    r.setDiagnosisCode(diag.getDiagnosisCode());
                    r.setDiagnosisDuration(diag.getDuration());
                    return r;
                }).collect(Collectors.toList());
            response.setDiagnoses(list);
        }

        if (c.getPastMedicalHistories() != null && !c.getPastMedicalHistories().isEmpty()) {
            PastMedicalHistory first = c.getPastMedicalHistories().get(0);
            response.setHistoryId(first.getHistoryId());
            response.setAllergies(first.getAllergeies());
            response.setCurrentMedicine(first.getCurrentMedicine());
            response.setMedicalHistory(first.getMedicalHistory());

            List<ConsultationResponse.PastMedicalHistoryResponse> list = c.getPastMedicalHistories().stream()
                .map(hist -> {
                    ConsultationResponse.PastMedicalHistoryResponse r = new ConsultationResponse.PastMedicalHistoryResponse();
                    r.setHistoryId(hist.getHistoryId());
                    r.setAllergies(hist.getAllergeies());
                    r.setCurrentMedicine(hist.getCurrentMedicine());
                    r.setMedicalHistory(hist.getMedicalHistory());
                    return r;
                }).collect(Collectors.toList());
            response.setPastMedicalHistories(list);
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

        consultationBillRepository.findByConsultationConsultationId(c.getConsultationId())
                .map(this::mapBill)
                .ifPresent(response::setBill);

        return response;
    }

    private ConsultationBillResponse mapBill(com.antss_prescription.entity.prescription.ConsultationBill bill) {
        ConsultationBillResponse response = new ConsultationBillResponse();
        response.setBillId(bill.getBillId());
        response.setBillNumber(bill.getBillNumber());
        response.setConsultationId(bill.getConsultation() == null ? null : bill.getConsultation().getConsultationId());
        response.setConsultationNumber(bill.getConsultation() == null ? null : bill.getConsultation().getConsultationNumber());
        response.setDoctorId(bill.getDoctor() == null ? null : bill.getDoctor().getId());
        response.setDoctorName(bill.getDoctor() == null ? null : bill.getDoctor().getDoctorName());
        response.setRegistrationId(bill.getPatientRegistration() == null ? null : bill.getPatientRegistration().getRegistrationId());
        response.setRegistrationNumber(bill.getPatientRegistration() == null ? null : bill.getPatientRegistration().getRegistrationNumber());
        response.setPatientName(bill.getPatientRegistration() == null ? null : bill.getPatientRegistration().getPatientName());
        response.setPatientMobileNumber(bill.getPatientRegistration() == null ? null : bill.getPatientRegistration().getMobileNumber());
        response.setConsultationFee(bill.getConsultationFee());
        response.setDiscountPolicy(bill.getDiscountPolicy());
        response.setDiscountValue(bill.getDiscountValue());
        response.setDiscountAmount(bill.getDiscountAmount());
        response.setPayableAmount(bill.getPayableAmount());
        response.setPaymentStatus(bill.getPaymentStatus());
        response.setCreatedAt(bill.getCreatedAt());
        response.setUpdatedAt(bill.getUpdatedAt());
        return response;
    }

    private DoctorOptionResponseDto mapDoctorOption(Doctor doctor) {
        DoctorOptionResponseDto response = new DoctorOptionResponseDto();
        response.setDoctorId(doctor.getId());
        response.setDoctorName(doctor.getDoctorName());
        response.setDoctorCode(doctor.getDoctorCode());
        response.setSpecialization(doctor.getSpecialization());
        response.setQualification(doctor.getQualification());
        response.setMobileNumber(doctor.getMobileNumber());
        response.setConsultationFee(doctor.getConsultationFee());
        response.setStatus(doctor.getStatus());
        return response;
    }

    private VitalsResponseDto mapVitals(Vitals vitals) {
        VitalsResponseDto response = new VitalsResponseDto();
        response.setVitalId(vitals.getVitalId());
        response.setHeight(vitals.getHeight());
        response.setWeight(vitals.getWeight());
        response.setTemperature(vitals.getTemprature());
        response.setPulse(vitals.getPulse());
        response.setSpo2(vitals.getSpo2());
        response.setBp(vitals.getBp());
        response.setRespiratoryRate(vitals.getRespiratoryRate());
        response.setCreatedAt(vitals.getCreatedAt());
        response.setUpdatedAt(vitals.getUpdatedAt());
        if (vitals.getPatientRegistration() != null) {
            response.setRegistrationId(vitals.getPatientRegistration().getRegistrationId());
        }
        if (vitals.getRecordedBy() != null) {
            response.setRecordedByRmoId(vitals.getRecordedBy().getId());
            response.setRecordedByRmoName(vitals.getRecordedBy().getRmoName());
        }
        return response;
    }

    private void applyVitals(Vitals vitals, VitalsRequestDto request) {
        vitals.setHeight(request.getHeight());
        vitals.setWeight(request.getWeight());
        vitals.setTemprature(request.getTemprature());
        vitals.setPulse(request.getPulse());
        vitals.setSpo2(request.getSpo2());
        vitals.setBp(request.getBp());
        vitals.setRespiratoryRate(request.getRespiratoryRate());
    }

    private void requireRmoForRegistration(Rmo rmo, PatientRegistration registration) {
        boolean sameHospital = rmo.getHospital() != null && registration.getHospital() != null
                && rmo.getHospital().getId().equals(registration.getHospital().getId());
        boolean sameClinic = rmo.getClinic() != null && registration.getClinic() != null
                && rmo.getClinic().getId().equals(registration.getClinic().getId());
        if (!sameHospital && !sameClinic) {
            throw new ConflictException("RMO and patient registration must belong to the same facility");
        }
    }

    private void requireAssignedDoctor(Consultation consultation, Doctor doctor) {
        if (consultation.getDoctor() == null || doctor == null
                || !consultation.getDoctor().getId().equals(doctor.getId())) {
            throw new ConflictException("Consultation is not assigned to the current doctor");
        }
    }

    private String formatAddress(String line1, String city, String state, String pin) {
        return java.util.stream.Stream.of(line1, city, state, pin)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private void resolveAndAuthorizeRelationships(Consultation consultation) {
        if (consultation.getPatientRegistration() == null
                || consultation.getPatientRegistration().getRegistrationId() <= 0) {
            throw new IllegalArgumentException("Patient registration is required");
        }
        var registration = registrationRepository.findById(
                        consultation.getPatientRegistration().getRegistrationId())
                .orElseThrow(() -> new RuntimeException("Patient registration not found"));
        accessControl.requireRegistrationAccess(registration);

        if (consultation.getDoctor() == null || consultation.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Doctor is required");
        }
        Doctor doctor = doctorRepository.findById(consultation.getDoctor().getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        accessControl.requireDoctorForRegistration(doctor, registration);

        consultation.setPatientRegistration(registration);
        consultation.setDoctor(doctor);
    }
}
