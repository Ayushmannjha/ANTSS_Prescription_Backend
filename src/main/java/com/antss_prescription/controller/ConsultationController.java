package com.antss_prescription.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.response.DoctorOptionResponseDto;
import com.antss_prescription.dto.response.VitalsResponseDto;
import com.antss_prescription.dto.request.ConsultationRequest;
import com.antss_prescription.dto.request.CreateConsultRequestDto;
import com.antss_prescription.dto.request.ClinicalRequestMapper;
import com.antss_prescription.dto.request.VitalsRequestDto;
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

    @GetMapping("/requests/my")
    public ResponseEntity<List<ConsultationResponse>> getMyConsultationRequests() {
        return ResponseEntity.ok(consultationService.getMyConsultationRequests());
    }

    @GetMapping("/registrations/{registrationId}/available-doctors")
    public ResponseEntity<List<DoctorOptionResponseDto>> getAvailableDoctorsForRegistration(
            @PathVariable Integer registrationId) {
        return ResponseEntity.ok(consultationService.getAvailableDoctorsForRegistration(registrationId));
    }

    @PostMapping("/registrations/{registrationId}/vitals")
    public ResponseEntity<VitalsResponseDto> saveVitals(
            @PathVariable Integer registrationId,
            @Valid @RequestBody VitalsRequestDto request) {
        return ResponseEntity.ok(consultationService.saveVitals(registrationId, request));
    }

    @PostMapping("/requests")
    public ResponseEntity<ConsultationResponse> createConsultRequest(
            @Valid @RequestBody CreateConsultRequestDto request) {
        return ResponseEntity.ok(consultationService.createConsultRequest(request));
    }

    @PatchMapping("/{consultationId}/start")
    public ResponseEntity<ConsultationResponse> startConsultation(
            @PathVariable Integer consultationId) {
        return ResponseEntity.ok(consultationService.startConsultation(consultationId));
    }

    @PatchMapping("/{consultationId}/complete")
    public ResponseEntity<ConsultationResponse> completeConsultation(
            @PathVariable Integer consultationId) {
        return ResponseEntity.ok(consultationService.completeConsultation(consultationId));
    }

    @PutMapping("/{consultationId}")
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable Integer consultationId,
            @Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.ok(consultationService.updateConsultation(
                consultationId, ClinicalRequestMapper.toConsultation(request)));
    }

    @DeleteMapping("/{consultationId}")
    public ResponseEntity<String> deleteConsultation(
            @PathVariable Integer consultationId) {
        consultationService.deleteConsultation(consultationId);
        return ResponseEntity.ok("Consultation deleted successfully");
    }
}
