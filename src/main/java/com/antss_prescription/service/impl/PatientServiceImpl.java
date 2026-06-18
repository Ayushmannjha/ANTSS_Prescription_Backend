package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.repository.prescription.PatientRepo;
import com.antss_prescription.service.PatientService;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepo patientRepo;

    @Override
    public Patient savePatient(Patient patient) {

        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());

        return patientRepo.save(patient);
    }

    @Override
    public Patient getPatientById(Integer patientId) {

        return patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id : " + patientId));
    }

    @Override
    public List<Patient> getAllPatients() {

        return patientRepo.findAll();
    }

    @Override
    public Patient updatePatient(Integer patientId, Patient patient) {

        Patient existingPatient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id : " + patientId));

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

        patientRepo.delete(patient);
    }
}