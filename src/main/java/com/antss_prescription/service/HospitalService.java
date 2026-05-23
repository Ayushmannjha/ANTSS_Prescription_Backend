package com.antss_prescription.service;

import com.antss_prescription.dto.request.CreateHospitalRequest;
import com.antss_prescription.dto.response.HospitalResponse;
import java.util.List;
import java.util.UUID;

public interface HospitalService {
    HospitalResponse createHospital(CreateHospitalRequest request, UUID ownerId);
    List<HospitalResponse> listHospitals(UUID userId);
    HospitalResponse getHospitalById(Long id, UUID userId);
}
