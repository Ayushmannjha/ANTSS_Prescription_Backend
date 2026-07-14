package com.antss_prescription.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.request.ConsultationRequest;
import com.antss_prescription.dto.request.ClinicalRequestMapper;
import com.antss_prescription.dto.request.UpdateVitalsRequest;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.service.ConsultationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ConsultationResponse> saveConsultation(
            @Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.ok(consultationService.saveConsultation(
                ClinicalRequestMapper.toConsultation(request)));
    }

    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationResponse> getConsultationById(
            @PathVariable Integer consultationId) {
        return ResponseEntity.ok(consultationService.getConsultationById(consultationId));
    }

    @GetMapping
    public ResponseEntity<List<ConsultationResponse>> getAllConsultations() {
        return ResponseEntity.ok(consultationService.getAllConsultations());
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<ConsultationResponse>> getConsultationsByDoctor(
            @PathVariable UUID doctorId) {
        return ResponseEntity.ok(consultationService.getConsultationsByDoctor(doctorId));
    }

    @PutMapping("/{consultationId}")
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable Integer consultationId,
            @Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.ok(consultationService.updateConsultation(
                consultationId, ClinicalRequestMapper.toConsultation(request)));
    }

    @RequestMapping(value = "/{consultationId}/vitals", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ConsultationResponse> updateVitals(
            @PathVariable Integer consultationId,
            @Valid @RequestBody UpdateVitalsRequest request) {
        return ResponseEntity.ok(consultationService.updateVitals(
                consultationId, ClinicalRequestMapper.toVitals(request)));
    }

    @DeleteMapping("/{consultationId}")
    public ResponseEntity<String> deleteConsultation(
            @PathVariable Integer consultationId) {
        consultationService.deleteConsultation(consultationId);
        return ResponseEntity.ok("Consultation deleted successfully");
    }
}
