package com.antss_prescription.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.dto.request.SavePrescriptionRequest;
import com.antss_prescription.dto.request.UpdatePrescriptionRequest;
import com.antss_prescription.dto.response.PrescriptionResponse;
import com.antss_prescription.service.PrescriptionService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/save")
    public ResponseEntity<PrescriptionResponse> save(
            @RequestBody SavePrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.savePrescription(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PrescriptionResponse>> getAll() {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponse>> getByPatient(
            @PathVariable int patientId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByPatientId(patientId));
    }

    @GetMapping("/registration/{registrationId}")
    public ResponseEntity<List<PrescriptionResponse>> getByRegistration(
            @PathVariable int registrationId) {
        return ResponseEntity.ok(
                prescriptionService.getPrescriptionsByRegistrationId(registrationId));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PrescriptionResponse> update(
            @PathVariable int id,
            @RequestBody UpdatePrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.updatePrescription(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable int id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.ok("Prescription deleted successfully");
    }
}