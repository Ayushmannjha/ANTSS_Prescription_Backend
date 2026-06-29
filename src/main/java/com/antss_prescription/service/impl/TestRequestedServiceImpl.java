package com.antss_prescription.service.impl;

import com.antss_prescription.docs.service.CloudinaryService;
import com.antss_prescription.docs.service.CloudinaryService.UploadResult;
import com.antss_prescription.dto.request.TestRequestedUploadRequest;
import com.antss_prescription.dto.response.TestRequestedResponse;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.TestRequested;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.repository.prescription.TestRequestedRepo;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.TestRequestedService;
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
public class TestRequestedServiceImpl implements TestRequestedService {

    private final TestRequestedRepo testRequestedRepo;
    private final PrescriptionRepo prescriptionRepo;
    private final PatientRegistrationRepo registrationRepo;
    private final DocumentRepo documentRepo;
    private final CloudinaryService cloudinaryService;
    private final AccessControlService accessControl;

    @Override
    public TestRequested save(TestRequested testRequested) {
        LocalDateTime now = LocalDateTime.now();
        if (testRequested.getId() == 0) {
            testRequested.setCreateAt(now);
        }
        testRequested.setUpdatedAt(now);
        return testRequestedRepo.save(testRequested);
    }

    @Override
    public TestRequestedResponse saveWithDocument(TestRequestedUploadRequest request, MultipartFile documentFile) {
        requireDocument(documentFile);
        UploadResult uploaded = null;
        try {
            ClinicalContext context = resolveContext(request.getRegistrationId(), request.getPrescriptionId());
            accessControl.requireRegistrationAccess(context.registration());

            uploaded = cloudinaryService.uploadFile(documentFile);
            Document document = Document.builder()
                    .fileName(documentFile.getOriginalFilename())
                    .url(uploaded.url())
                    .documentType("TEST_REQUESTED")
                    .cloudinaryPublicId(uploaded.publicId())
                    .cloudinaryResourceType(uploaded.resourceType())
                    .patient(context.registration().getPatient())
                    .prescription(context.prescription())
                    .build();
            Document savedDocument = documentRepo.saveAndFlush(document);

            LocalDateTime now = LocalDateTime.now();
            TestRequested testRequested = new TestRequested();
            testRequested.setTestName(request.getTestName());
            testRequested.setNotes(request.getNotes());
            testRequested.setPatientRegistration(context.registration());
            testRequested.setPrescription(context.prescription());
            testRequested.setDocument(savedDocument);
            testRequested.setCreateAt(now);
            testRequested.setUpdatedAt(now);

            return toResponse(testRequestedRepo.save(testRequested));
        } catch (RuntimeException | IOException ex) {
            if (uploaded != null) {
                cleanupUpload(uploaded);
            }
            if (ex instanceof BusinessException || ex instanceof ResourceNotFoundException) {
                throw (RuntimeException) ex;
            }
            log.error("Failed to save requested test with document", ex);
            throw new BusinessException("Unable to save requested test document at this time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequested> getByRegistrationNumber(String registrationNumber) {
        return testRequestedRepo.findByPatientRegistrationRegistrationNumber(registrationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequested> getByDocumentId(Integer documentId) {
        return testRequestedRepo.findByDocumentId(documentId);
    }

    @Override
    public void deleteById(Integer id) {
        testRequestedRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequested> getByPrescription(Integer prescriptionId) {
        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));
        accessControl.requirePrescriptionAccess(prescription);
        return testRequestedRepo.findByPrescription(prescription);
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
            log.error("Failed to clean up requested test upload {}", uploaded.publicId(), cleanupError);
        }
    }

    private TestRequestedResponse toResponse(TestRequested testRequested) {
        Document document = testRequested.getDocument();
        Prescription prescription = testRequested.getPrescription();
        PatientRegistration registration = testRequested.getPatientRegistration();
        return TestRequestedResponse.builder()
                .id(testRequested.getId())
                .testName(testRequested.getTestName())
                .notes(testRequested.getNotes())
                .registrationId(registration == null ? null : registration.getRegistrationId())
                .prescriptionId(prescription == null ? null : prescription.getPrescriptionId())
                .documentId(document == null ? null : document.getId())
                .documentFileName(document == null ? null : document.getFileName())
                .documentUrl(document == null ? null : document.getUrl())
                .createdAt(testRequested.getCreateAt())
                .updatedAt(testRequested.getUpdatedAt())
                .build();
    }

    private record ClinicalContext(PatientRegistration registration, Prescription prescription) {}
}
