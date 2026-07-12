package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antss_prescription.dto.request.DiagnosticOrderRequest;
import com.antss_prescription.dto.request.DiagnosticStatusRequest;
import com.antss_prescription.dto.response.DiagnosticOrderResponse;
import com.antss_prescription.entity.prescription.DiagnosticOrder;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.enums.DiagnosticStatus;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.prescription.DiagnosticOrderRepository;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.DiagnosticOrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DiagnosticOrderServiceImpl implements DiagnosticOrderService {
    private final DiagnosticOrderRepository repository;
    private final PatientRegistrationRepo registrationRepository;
    private final PrescriptionRepo prescriptionRepository;
    private final DocumentRepo documentRepository;
    private final AccessControlService accessControl;

    @Override
    public DiagnosticOrderResponse create(DiagnosticOrderRequest request) {
        DiagnosticOrder order = new DiagnosticOrder();
        order.setTestName(request.getTestName());
        order.setNotes(request.getNotes());
        order.setStatus(DiagnosticStatus.REQUESTED);

        Prescription prescription = request.getPrescriptionId() == null
                ? null : requirePrescription(request.getPrescriptionId());
        PatientRegistration registration = prescription == null
                ? requireRegistration(request.getRegistrationId())
                : prescription.getConsultation().getPatientRegistration();
        if (request.getRegistrationId() != null
                && registration.getRegistrationId() != request.getRegistrationId()) {
            throw new BusinessException("Prescription and registration do not belong together");
        }

        order.setPatientRegistration(registration);
        order.setPrescription(prescription);
        if (request.getReportDocumentId() != null) {
            order.setReportDocument(requireDocumentForRegistration(request.getReportDocumentId(), registration));
        }
        return map(repository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosticOrderResponse getById(Integer id) {
        return map(requireAuthorized(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticOrderResponse> getByRegistrationNumber(String registrationNumber) {
        PatientRegistration registration = registrationRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Patient registration", registrationNumber));
        accessControl.requireRegistrationAccess(registration);
        return repository.findByPatientRegistrationRegistrationNumber(registrationNumber)
                .stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticOrderResponse> getByPrescription(Integer prescriptionId) {
        Prescription prescription = requirePrescription(prescriptionId);
        return repository.findByPrescription(prescription).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticOrderResponse> getByDocument(Integer documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        accessControl.requireRegistrationAccess(document.getPatientRegistration());
        return repository.findByReportDocumentId(documentId).stream().map(this::map).toList();
    }

    @Override
    public DiagnosticOrderResponse updateStatus(Integer id, DiagnosticStatusRequest request) {
        DiagnosticOrder order = requireAuthorized(id);
        DiagnosticStatus target = request.getStatus();
        validateTransition(order.getStatus(), target);
        LocalDateTime now = LocalDateTime.now();

        if (target == DiagnosticStatus.IN_PROGRESS && order.getStartedAt() == null) order.setStartedAt(now);
        if (target == DiagnosticStatus.COMPLETED) {
            if ((request.getResultSummary() == null || request.getResultSummary().isBlank())
                    && request.getReportDocumentId() == null && order.getReportDocument() == null) {
                throw new BusinessException("A result summary or report document is required to complete a diagnostic");
            }
            order.setCompletedAt(now);
        }
        if (target == DiagnosticStatus.CANCELLED) order.setCancelledAt(now);
        if (request.getResultSummary() != null) order.setResultSummary(request.getResultSummary());
        if (request.getReportDocumentId() != null) {
            order.setReportDocument(requireDocumentForRegistration(
                    request.getReportDocumentId(), order.getPatientRegistration()));
        }
        order.setStatus(target);
        return map(repository.save(order));
    }

    @Override
    public void delete(Integer id) {
        repository.delete(requireAuthorized(id));
    }

    private DiagnosticOrder requireAuthorized(Integer id) {
        DiagnosticOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic order", id));
        accessControl.requireRegistrationAccess(order.getPatientRegistration());
        return order;
    }

    private PatientRegistration requireRegistration(Integer id) {
        PatientRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient registration", id));
        accessControl.requireRegistrationAccess(registration);
        return registration;
    }

    private Prescription requirePrescription(Integer id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
        accessControl.requirePrescriptionAccess(prescription);
        return prescription;
    }

    private Document requireDocumentForRegistration(Integer id, PatientRegistration registration) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        accessControl.requireRegistrationAccess(document.getPatientRegistration());
        if (document.getPatientRegistration() == null
                || document.getPatientRegistration().getRegistrationId() != registration.getRegistrationId()) {
            throw new BusinessException("Report document belongs to a different registration");
        }
        return document;
    }

    private void validateTransition(DiagnosticStatus current, DiagnosticStatus target) {
        if (current == target) return;
        boolean valid = current == DiagnosticStatus.REQUESTED
                && (target == DiagnosticStatus.IN_PROGRESS || target == DiagnosticStatus.CANCELLED)
                || current == DiagnosticStatus.IN_PROGRESS
                && (target == DiagnosticStatus.COMPLETED || target == DiagnosticStatus.CANCELLED);
        if (!valid) throw new BusinessException("Invalid diagnostic status transition: " + current + " to " + target);
    }

    private DiagnosticOrderResponse map(DiagnosticOrder order) {
        return DiagnosticOrderResponse.builder()
                .id(order.getId()).testName(order.getTestName()).notes(order.getNotes())
                .resultSummary(order.getResultSummary()).status(order.getStatus())
                .registrationId(order.getPatientRegistration().getRegistrationId())
                .prescriptionId(order.getPrescription() == null ? null : order.getPrescription().getPrescriptionId())
                .reportDocumentId(order.getReportDocument() == null ? null : order.getReportDocument().getId())
                .requestedAt(order.getRequestedAt()).startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt()).cancelledAt(order.getCancelledAt()).build();
    }
}
