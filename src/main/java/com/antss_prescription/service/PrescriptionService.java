package com.antss_prescription.service;

import com.antss_prescription.dto.request.CreatePrescriptionRequest;
import com.antss_prescription.dto.response.PrescriptionResponse;

import java.util.List;
import java.util.UUID;

public interface PrescriptionService {
    PrescriptionResponse createPrescription(CreatePrescriptionRequest request, UUID userId);
    List<PrescriptionResponse> getPrescriptions(UUID userId);
    PrescriptionResponse getPrescriptionById(Long id, UUID userId);
}
