package com.antss_prescription.service;

import com.antss_prescription.dto.request.CreateClinicRequest;
import com.antss_prescription.dto.response.ClinicResponse;
import java.util.List;
import java.util.UUID;

public interface ClinicService {
    ClinicResponse createClinic(CreateClinicRequest request, UUID ownerId);
    List<ClinicResponse> listClinics(UUID userId);
    ClinicResponse getClinicById(Long id, UUID userId);
}
