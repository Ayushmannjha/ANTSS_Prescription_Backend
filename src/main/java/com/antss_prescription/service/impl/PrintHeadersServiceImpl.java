package com.antss_prescription.service.impl;

import com.antss_prescription.docs.service.CloudinaryService;
import com.antss_prescription.docs.service.CloudinaryService.UploadResult;
import com.antss_prescription.entity.prescription.PrintHeaders;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.prescription.PrintHeadersRepo;
import com.antss_prescription.service.PrintHeadersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PrintHeadersServiceImpl implements PrintHeadersService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final PrintHeadersRepo printHeadersRepo;
    private final CloudinaryService cloudinaryService;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;

    @Override
    public PrintHeaders uploadHeader(long entityId, String entityType, MultipartFile image) {
        validate(entityId, entityType, image);
        validateEntityExists(entityId, entityType);

        UploadResult uploaded = null;
        try {
            uploaded = cloudinaryService.uploadFile(image);

            String normalizedEntityType = normalizeEntityType(entityType);
            PrintHeaders headers = printHeadersRepo.findByEntityIdAndEntityTypeIgnoreCase(entityId, normalizedEntityType)
                    .orElseGet(PrintHeaders::new);
            headers.setEntityId(entityId);
            headers.setEntityType(normalizedEntityType);
            headers.setHeaderUrl(uploaded.url());

            return printHeadersRepo.save(headers);
        } catch (Exception e) {
            if (uploaded != null) {
                cleanupUpload(uploaded);
            }
            if (e instanceof BusinessException || e instanceof ResourceNotFoundException) {
                throw (RuntimeException) e;
            }
            log.error("Print header upload failed for {} {}", entityType, entityId, e);
            throw new BusinessException("Unable to upload print header image at this time");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrintHeaders> getHeaders(Long entityId, String entityType) {
        if (entityId != null && entityType != null && !entityType.isBlank()) {
            return printHeadersRepo.findByEntityIdAndEntityTypeIgnoreCaseOrderByUpdatedAtDesc(
                    entityId,
                    normalizeEntityType(entityType));
        }
        if (entityId != null) {
            return printHeadersRepo.findByEntityId(entityId);
        }
        if (entityType != null && !entityType.isBlank()) {
            return printHeadersRepo.findByEntityTypeIgnoreCase(normalizeEntityType(entityType));
        }
        return printHeadersRepo.findAllByOrderByUpdatedAtDesc();
    }

    private void validate(long entityId, String entityType, MultipartFile image) {
        if (entityId <= 0) {
            throw new BusinessException("Entity id is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new BusinessException("Entity type is required");
        }
        if (image == null || image.isEmpty()) {
            throw new BusinessException("Header image is required");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("Header image must not exceed 5 MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException("Only image files are allowed");
        }
    }

    private void validateEntityExists(long entityId, String entityType) {
        String normalizedEntityType = normalizeEntityType(entityType);
        if ("HOSPITAL".equals(normalizedEntityType)) {
            if (!hospitalRepository.existsById(entityId)) {
                throw new ResourceNotFoundException("Hospital", entityId);
            }
            return;
        }
        if ("CLINIC".equals(normalizedEntityType)) {
            if (!clinicRepository.existsById(entityId)) {
                throw new ResourceNotFoundException("Clinic", entityId);
            }
            return;
        }
        throw new BusinessException("Entity type must be HOSPITAL or CLINIC");
    }

    private String normalizeEntityType(String entityType) {
        return entityType.trim().toUpperCase(Locale.ROOT);
    }

    private void cleanupUpload(UploadResult uploaded) {
        try {
            cloudinaryService.deleteFile(uploaded.publicId(), uploaded.resourceType());
        } catch (IOException cleanupError) {
            log.error("Failed to clean up Cloudinary print header upload {}", uploaded.publicId(), cleanupError);
        }
    }
}
