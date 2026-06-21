package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.repository.prescription.PatientRepo;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.PatientService;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepo patientRepo;
    private final AccessControlService accessControl;

    @Override
    public Patient savePatient(Patient patient) {

        // Normal patient creation happens through patient registration, where a
        // facility relationship can be established immediately.
        accessControl.requireAdmin();

        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());

        return patientRepo.save(patient);
    }

    @Override
    public Patient getPatientById(Integer patientId) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id : " + patientId));
        accessControl.requirePatientAccess(patient);
        return patient;
    }

    @Override
    public List<Patient> getAllPatients() {

        var user = accessControl.currentUser();
        return patientRepo.findAll().stream()
                .filter(patient -> accessControl.canAccess(patient, user))
                .toList();
    }

    @Override
    public Patient updatePatient(Integer patientId, Patient patient) {

        Patient existingPatient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id : " + patientId));

        accessControl.requirePatientAccess(existingPatient);

        existingPatient.setPatientName(patient.getPatientName());
        existingPatient.setMobileNumber(patient.getMobileNumber());
        existingPatient.setGender(patient.getGender());
        existingPatient.setDateOfBirth(patient.getDateOfBirth());
        existingPatient.setAge(patient.getAge());
        existingPatient.setAddress(patient.getAddress());
        existingPatient.setState(patient.getState());
        existingPatient.setCity(patient.getCity());
        existingPatient.setPincode(patient.getPincode());

        existingPatient.setUpdatedAt(LocalDateTime.now());

        return patientRepo.save(existingPatient);
    }

    @Override
    public void deletePatient(Integer patientId) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id : " + patientId));

        accessControl.requirePatientAccess(patient);

        patientRepo.delete(patient);
    }
}
