package com.antss_prescription.service.impl;

import com.antss_prescription.docs.service.CloudinaryService;
import com.antss_prescription.docs.service.CloudinaryService.UploadResult;
import com.antss_prescription.dto.request.InvestigationUploadRequest;
import com.antss_prescription.dto.response.InvestigationResponse;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.Investigations;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.InvestigationsRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.InvestigationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvestigationsServiceImpl implements InvestigationsService {

    private final InvestigationsRepo investigationsRepo;
    private final PrescriptionRepo prescriptionRepo;
    private final PatientRegistrationRepo registrationRepo;
    private final DocumentRepo documentRepo;
    private final CloudinaryService cloudinaryService;
    private final AccessControlService accessControl;

    @Override
    public Investigations save(Investigations investigations) {
        LocalDateTime now = LocalDateTime.now();
        if (investigations.getId() == 0) {
            investigations.setCreateAt(now);
        }
        investigations.setUpdatedAt(now);
        return investigationsRepo.save(investigations);
    }

    @Override
    public InvestigationResponse saveWithDocument(InvestigationUploadRequest request, MultipartFile documentFile) {
        requireDocument(documentFile);
        UploadResult uploaded = null;
        try {
            ClinicalContext context = resolveContext(request.getRegistrationId(), request.getPrescriptionId());
            accessControl.requireRegistrationAccess(context.registration());

            uploaded = cloudinaryService.uploadFile(documentFile);
            Document document = Document.builder()
                    .fileName(documentFile.getOriginalFilename())
                    .url(uploaded.url())
                    .documentType("INVESTIGATION")
                    .cloudinaryPublicId(uploaded.publicId())
                    .cloudinaryResourceType(uploaded.resourceType())
                    .patient(context.registration().getPatient())
                    .prescription(context.prescription())
                    .build();
            Document savedDocument = documentRepo.saveAndFlush(document);

            LocalDateTime now = LocalDateTime.now();
            Investigations investigation = new Investigations();
            investigation.setInestigationName(request.getInvestigationName());
            investigation.setNotes(request.getNotes());
            investigation.setPatientRegistration(context.registration());
            investigation.setPrescription(context.prescription());
            investigation.setDocument(savedDocument);
            investigation.setCreateAt(now);
            investigation.setUpdatedAt(now);

            return toResponse(investigationsRepo.save(investigation));
        } catch (RuntimeException | IOException ex) {
            if (uploaded != null) {
                cleanupUpload(uploaded);
            }
            if (ex instanceof BusinessException || ex instanceof ResourceNotFoundException) {
                throw (RuntimeException) ex;
            }
            log.error("Failed to save investigation with document", ex);
            throw new BusinessException("Unable to save investigation document at this time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Investigations> getByRegistrationNumber(String registrationNumber) {
        return investigationsRepo.findByPatientRegistrationRegistrationNumber(registrationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Investigations> getByDocumentId(Integer documentId) {
        return investigationsRepo.findByDocumentId(documentId);
    }

    @Override
    public void deleteById(Integer id) {
        investigationsRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Investigations> getByPrescription(Integer prescriptionId) {
        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));
        accessControl.requirePrescriptionAccess(prescription);
        return investigationsRepo.findByPrescription(prescription);
    }

    private ClinicalContext resolveContext(Integer registrationId, Integer prescriptionId) {
        Prescription prescription = null;
        PatientRegistration registration = null;

        if (prescriptionId != null) {
            prescription = prescriptionRepo.findById(prescriptionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));
            accessControl.requirePrescriptionAccess(prescription);
            registration = prescription.getConsultation().getPatientRegistration();
        }

        if (registrationId != null) {
            PatientRegistration explicitRegistration = registrationRepo.findById(registrationId)
                    .orElseThrow(() -> new ResourceNotFoundException("PatientRegistration", registrationId));
            accessControl.requireRegistrationAccess(explicitRegistration);
            if (registration != null && registration.getRegistrationId() != explicitRegistration.getRegistrationId()) {
                throw new BusinessException("Prescription and registration do not belong to the same patient visit");
            }
            registration = explicitRegistration;
        }

        if (registration == null) {
            throw new BusinessException("Registration or prescription is required");
        }

        return new ClinicalContext(registration, prescription);
    }

    private void requireDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Document file is required");
        }
    }

    private void cleanupUpload(UploadResult uploaded) {
        try {
            cloudinaryService.deleteFile(uploaded.publicId(), uploaded.resourceType());
        } catch (IOException cleanupError) {
            log.error("Failed to clean up investigation upload {}", uploaded.publicId(), cleanupError);
        }
    }

    private InvestigationResponse toResponse(Investigations investigation) {
        Document document = investigation.getDocument();
        Prescription prescription = investigation.getPrescription();
        PatientRegistration registration = investigation.getPatientRegistration();
        return InvestigationResponse.builder()
                .id(investigation.getId())
                .investigationName(investigation.getInestigationName())
                .notes(investigation.getNotes())
                .registrationId(registration == null ? null : registration.getRegistrationId())
                .prescriptionId(prescription == null ? null : prescription.getPrescriptionId())
                .documentId(document == null ? null : document.getId())
                .documentFileName(document == null ? null : document.getFileName())
                .documentUrl(document == null ? null : document.getUrl())
                .createdAt(investigation.getCreateAt())
                .updatedAt(investigation.getUpdatedAt())
                .build();
    }

    private record ClinicalContext(PatientRegistration registration, Prescription prescription) {}
}
