package com.antss_prescription.service;
import java.util.List;

import com.antss_prescription.entity.prescription.Patient;

public interface PatientService {

    Patient savePatient(Patient patient);

    Patient getPatientById(Integer patientId);

    List<Patient> getAllPatients();

    Patient updatePatient(Integer patientId, Patient patient);

    void deletePatient(Integer patientId);
}
