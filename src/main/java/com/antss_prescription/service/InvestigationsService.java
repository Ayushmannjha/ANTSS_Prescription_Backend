package com.antss_prescription.service;

import java.util.List;

import com.antss_prescription.entity.prescription.Investigations;

public interface InvestigationsService {

    Investigations save(Investigations investigations);

    List<Investigations> getByRegistrationNumber(String registrationNumber);
    
    List<Investigations> getByPrescription(Integer prescriptionId);

    List<Investigations> getByDocumentId(Integer documentId);

    void deleteById(Integer id);
}