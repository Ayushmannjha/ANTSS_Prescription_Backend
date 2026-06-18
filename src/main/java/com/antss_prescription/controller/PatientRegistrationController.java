package com.antss_prescription.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.dto.response.PatientRegistrationResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.service.PatientRegistrationService;

@RestController
@RequestMapping("/api/patient-registrations")
@RequiredArgsConstructor
public class PatientRegistrationController {

    private final PatientRegistrationService patientRegistrationService;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    

    // Create Registration
    @PostMapping
    public ResponseEntity<PatientRegistration> saveRegistration(
            @RequestBody PatientRegistration registration) {

        PatientRegistration savedRegistration =
                patientRegistrationService.saveRegistration(registration);

        return ResponseEntity.ok(savedRegistration);
    }

    // Get Registration By Id
    @GetMapping("/{registrationId}")
    public ResponseEntity<PatientRegistrationResponse> getRegistrationById(
            @PathVariable Integer registrationId) {

    	PatientRegistrationResponse registration =
                patientRegistrationService.getRegistrationById(registrationId);

        return ResponseEntity.ok(registration);
    }

    // Get All Registrations
    @GetMapping
    public ResponseEntity<List<PatientRegistrationResponse>> getAllRegistrations() {

        List<PatientRegistrationResponse> registrations =
                patientRegistrationService.getAllRegistrations();

        return ResponseEntity.ok(registrations);
    }
    @GetMapping("/clinic/{clinicId}")
    public ResponseEntity<List<PatientRegistrationResponse>> getByClinic(
            @PathVariable Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId).get();
        return ResponseEntity.ok(patientRegistrationService.getAllRegistrationsByClinic(clinic));
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<PatientRegistrationResponse>> getByHospital(
            @PathVariable Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).get();
        return ResponseEntity.ok(patientRegistrationService.getAllRegistrationsByHospital(hospital));
    }
    // Update Registration
    @PutMapping("/{registrationId}")
    public ResponseEntity<PatientRegistrationResponse> updateRegistration(
            @PathVariable Integer registrationId,
            @RequestBody PatientRegistration registration) {
    	
    	PatientRegistrationResponse updatedRegistration =
                patientRegistrationService.updateRegistration(
                        registrationId,
                        registration);

        return ResponseEntity.ok(updatedRegistration);
    }

    // Delete Registration
    @DeleteMapping("/{registrationId}")
    public ResponseEntity<String> deleteRegistration(
            @PathVariable Integer registrationId) {

        patientRegistrationService.deleteRegistration(registrationId);

        return ResponseEntity.ok(
                "Patient Registration deleted successfully");
    }
}