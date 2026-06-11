package com.antss_prescription.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.service.ConsultationService;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ConsultationResponse> saveConsultation(
            @RequestBody Consultation consultation) {
        return ResponseEntity.ok(consultationService.saveConsultation(consultation));
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
            @RequestBody Consultation consultation) {
        return ResponseEntity.ok(consultationService.updateConsultation(consultationId, consultation));
    }

    @DeleteMapping("/{consultationId}")
    public ResponseEntity<String> deleteConsultation(
            @PathVariable Integer consultationId) {
        consultationService.deleteConsultation(consultationId);
        return ResponseEntity.ok("Consultation deleted successfully");
    }
}