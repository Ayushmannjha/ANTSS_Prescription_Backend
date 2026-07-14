package com.antss_prescription.service;

import com.antss_prescription.dto.request.CreateDoctorRequest;
import com.antss_prescription.dto.request.UpdateDoctorRequest;
import com.antss_prescription.dto.response.DoctorResponse;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    DoctorResponse addDoctor(CreateDoctorRequest request, UUID userId);
    DoctorResponse updateDoctor(UUID id, UpdateDoctorRequest request, UUID userId);
    void deleteDoctor(UUID id, UUID userId);
    List<DoctorResponse> listDoctors(UUID userId, Long hospitalId, Long clinicId);
    DoctorResponse getDoctorById(UUID id, UUID userId);
    DoctorResponse getDoctorByUserId(UUID id);
}
