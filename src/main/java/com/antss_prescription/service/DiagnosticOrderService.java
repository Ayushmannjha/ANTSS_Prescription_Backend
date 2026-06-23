package com.antss_prescription.service;

import java.util.List;

import com.antss_prescription.dto.request.DiagnosticOrderRequest;
import com.antss_prescription.dto.request.DiagnosticStatusRequest;
import com.antss_prescription.dto.response.DiagnosticOrderResponse;

public interface DiagnosticOrderService {
    DiagnosticOrderResponse create(DiagnosticOrderRequest request);
    DiagnosticOrderResponse getById(Integer id);
    List<DiagnosticOrderResponse> getByRegistrationNumber(String registrationNumber);
    List<DiagnosticOrderResponse> getByPrescription(Integer prescriptionId);
    List<DiagnosticOrderResponse> getByDocument(Integer documentId);
    DiagnosticOrderResponse updateStatus(Integer id, DiagnosticStatusRequest request);
    void delete(Integer id);
}
