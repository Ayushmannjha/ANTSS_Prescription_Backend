package com.antss_prescription.service;

import java.util.List;

import com.antss_prescription.dto.request.SavePrescriptionRequest;
import com.antss_prescription.dto.request.UpdatePrescriptionRequest;
import com.antss_prescription.dto.response.PrescriptionResponse;
import com.antss_prescription.dto.response.DetailedPrescriptionResponse;

public interface PrescriptionService {

    // Create
    PrescriptionResponse savePrescription(SavePrescriptionRequest request);

    // Read
    PrescriptionResponse getPrescriptionById(int prescriptionId);
    List<PrescriptionResponse> getAllPrescriptions();
    List<PrescriptionResponse> getPrescriptionsByPatientId(int patientId);
    List<PrescriptionResponse> getPrescriptionsByRegistrationId(int registrationId);
    DetailedPrescriptionResponse getDetailedPrescriptionById(int prescriptionId);
    DetailedPrescriptionResponse getDetailedPrescriptionForPrintById(int prescriptionId);
    List<DetailedPrescriptionResponse> getDetailedPrescriptionsByPatientId(int patientId);


    // Update
    PrescriptionResponse updatePrescription(int prescriptionId, UpdatePrescriptionRequest request);

    // Delete
    void deletePrescription(int prescriptionId);
}
