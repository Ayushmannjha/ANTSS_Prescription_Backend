package com.antss_prescription.controller;

import com.antss_prescription.dto.response.ClinicalRecordResponse;
import com.antss_prescription.service.DoctorHospitalClinicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clinical-records/doctors/{doctorId}/{facilityType:hospitals|clinics}/{facilityId}")
public class DoctorHospitalClinicalRecordController {
    private final DoctorHospitalClinicalRecordService service;

    @GetMapping("/diagnoses")
    public ResponseEntity<List<ClinicalRecordResponse>> diagnoses(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.diagnoses(doctorId, facilityType, facilityId));
    }

    @GetMapping("/chief-complaints")
    public ResponseEntity<List<ClinicalRecordResponse>> complaints(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.complaints(doctorId, facilityType, facilityId));
    }

    @GetMapping("/general-examinations")
    public ResponseEntity<List<ClinicalRecordResponse>> examinations(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.examinations(doctorId, facilityType, facilityId));
    }

    @GetMapping("/past-medical-histories")
    public ResponseEntity<List<ClinicalRecordResponse>> histories(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.histories(doctorId, facilityType, facilityId));
    }

    @GetMapping("/investigations")
    public ResponseEntity<List<ClinicalRecordResponse>> investigations(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.investigations(doctorId, facilityType, facilityId));
    }

    @GetMapping("/tests-requested")
    public ResponseEntity<List<ClinicalRecordResponse>> tests(@PathVariable UUID doctorId, @PathVariable String facilityType, @PathVariable Long facilityId) {
        return ResponseEntity.ok(service.tests(doctorId, facilityType, facilityId));
    }
}
