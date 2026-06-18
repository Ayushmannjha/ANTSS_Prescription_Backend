package com.antss_prescription.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.service.PatientService;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // Create Patient
    @PostMapping
    public ResponseEntity<Patient> savePatient(@RequestBody Patient patient) {

        Patient savedPatient = patientService.savePatient(patient);

        return ResponseEntity.ok(savedPatient);
    }

    // Get Patient By Id
    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable Integer patientId) {

        Patient patient = patientService.getPatientById(patientId);

        return ResponseEntity.ok(patient);
    }

    // Get All Patients
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {

        List<Patient> patients = patientService.getAllPatients();

        return ResponseEntity.ok(patients);
    }

    // Update Patient
    @PutMapping("/{patientId}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable Integer patientId,
            @RequestBody Patient patient) {

        Patient updatedPatient =
                patientService.updatePatient(patientId, patient);

        return ResponseEntity.ok(updatedPatient);
    }

    // Delete Patient
    @DeleteMapping("/{patientId}")
    public ResponseEntity<String> deletePatient(
            @PathVariable Integer patientId) {

        patientService.deletePatient(patientId);

        return ResponseEntity.ok("Patient deleted successfully");
    }
}