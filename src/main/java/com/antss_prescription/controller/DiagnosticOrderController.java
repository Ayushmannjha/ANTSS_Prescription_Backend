package com.antss_prescription.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.dto.request.DiagnosticOrderRequest;
import com.antss_prescription.dto.request.DiagnosticStatusRequest;
import com.antss_prescription.dto.response.DiagnosticOrderResponse;
import com.antss_prescription.service.DiagnosticOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
public class DiagnosticOrderController {
    private final DiagnosticOrderService service;

    @PostMapping
    public ResponseEntity<DiagnosticOrderResponse> create(@Valid @RequestBody DiagnosticOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnosticOrderResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<List<DiagnosticOrderResponse>> getByRegistration(
            @PathVariable String registrationNumber) {
        return ResponseEntity.ok(service.getByRegistrationNumber(registrationNumber));
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<List<DiagnosticOrderResponse>> getByPrescription(@PathVariable Integer prescriptionId) {
        return ResponseEntity.ok(service.getByPrescription(prescriptionId));
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<DiagnosticOrderResponse>> getByDocument(@PathVariable Integer documentId) {
        return ResponseEntity.ok(service.getByDocument(documentId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DiagnosticOrderResponse> updateStatus(
            @PathVariable Integer id, @Valid @RequestBody DiagnosticStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
