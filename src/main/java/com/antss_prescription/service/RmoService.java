package com.antss_prescription.service;

import com.antss_prescription.dto.request.CreateRmoRequest;
import com.antss_prescription.dto.response.RmoResponse;

import java.util.List;
import java.util.UUID;

public interface RmoService {
    RmoResponse addRmo(CreateRmoRequest request, UUID userId);
    RmoResponse updateRmo(UUID id, CreateRmoRequest request, UUID userId);
    void deleteRmo(UUID id, UUID userId);
    List<RmoResponse> listRmos(UUID userId);
    RmoResponse getRmoById(UUID id, UUID userId);
}
