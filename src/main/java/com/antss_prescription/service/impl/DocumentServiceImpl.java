package com.antss_prescription.service.impl;

import com.antss_prescription.docs.service.CloudinaryService;
import com.antss_prescription.docs.service.CloudinaryService.UploadResult;
import com.antss_prescription.docs.service.dto.DocumentDto;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.service.DocumentService;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    private final DocumentRepo documentRepository;
    private final PatientRegistrationRepo registrationRepository;
    private final PrescriptionRepo prescriptionRepository;
    private final CloudinaryService cloudinaryService;
    private final AccessControlService accessControl;

    @Override
    public DocumentDto uploadDocument(Integer patientId, MultipartFile file, String type) {
        validateFile(file);
        UploadResult uploaded = null;
        try {
            PatientRegistration registration = registrationRepository.findById(patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient registration", patientId));
            accessControl.requireRegistrationAccess(registration);

            uploaded = cloudinaryService.uploadFile(file);

            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .url(uploaded.url())
                    .documentType(type)
                    .cloudinaryPublicId(uploaded.publicId())
                    .cloudinaryResourceType(uploaded.resourceType())
                    .patientRegistration(registration)
                    .build();

            Document savedDocument = documentRepository.saveAndFlush(document);

            return toDto(savedDocument);

        } catch (Exception e) {
            if (uploaded != null) {
                cleanupUpload(uploaded);
            }
            if (e instanceof BusinessException || e instanceof ResourceNotFoundException) {
                throw (RuntimeException) e;
            }
            log.error("Document upload failed for patient {}", patientId, e);
            throw new BusinessException("Unable to upload document at this time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getPatientDocuments(Integer patientId) {

        PatientRegistration registration = registrationRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient registration", patientId));
        accessControl.requireRegistrationAccess(registration);

        return documentRepository.findByPatientId(patientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocument(Integer patientId, Integer documentId) {

        Document document = documentRepository.findByIdAndPatientId(
                        documentId,
                        patientId
                ).orElseThrow(() -> new RuntimeException("Document not found"));

        accessControl.requireRegistrationAccess(document.getPatientRegistration());

        return toDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsByPrescription(Integer prescriptionId) {

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));

        accessControl.requirePrescriptionAccess(prescription);

        return documentRepository.getDocumentsByPrescription(prescription)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void deleteDocument(Integer patientId, Integer documentId) {

        Document document = documentRepository.findByIdAndPatientId(documentId, patientId).orElseThrow(() ->
                        new ResourceNotFoundException("Document", documentId));

        accessControl.requireRegistrationAccess(document.getPatientRegistration());

        try {
            String publicId = document.getCloudinaryPublicId() != null
                    ? document.getCloudinaryPublicId()
                    : cloudinaryService.extractPublicId(document.getUrl());
            cloudinaryService.deleteFile(
                    publicId, document.getCloudinaryResourceType());
        } catch (IOException e) {
            log.error("Cloudinary deletion failed for document {}", documentId, e);
            throw new BusinessException("Unable to delete document file at this time");
        }
        documentRepository.delete(document);
        documentRepository.flush();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Document file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Document file must not exceed 10 MB");
        }
        String fileName = file.getOriginalFilename();
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedContentType = ALLOWED_TYPES.get(extension);
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (expectedContentType == null || !expectedContentType.equals(contentType)) {
            throw new BusinessException("Only PDF, PNG, and JPEG documents are allowed");
        }
    }

    private void cleanupUpload(UploadResult uploaded) {
        try {
            cloudinaryService.deleteFile(uploaded.publicId(), uploaded.resourceType());
        } catch (IOException cleanupError) {
            log.error("Failed to clean up Cloudinary upload {}", uploaded.publicId(), cleanupError);
        }
    }

    private DocumentDto toDto(Document document) {
        PatientRegistration registration = document.getPatientRegistration();
        return DocumentDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .url(document.getUrl())
                .documentType(document.getDocumentType())
                .patientId(registration == null ? 0 : registration.getRegistrationId())
                .patientName(registration == null ? null : registration.getPatientName())
                .mobileNumber(registration == null ? null : registration.getMobileNumber())
                .build();
    }
}
