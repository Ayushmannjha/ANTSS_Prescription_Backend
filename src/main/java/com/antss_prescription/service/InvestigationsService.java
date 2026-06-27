package com.antss_prescription.service;

import com.antss_prescription.entity.prescription.Investigations;

import java.util.List;

public interface InvestigationsService {

    Investigations save(Investigations investigations);

    List<Investigations> getByRegistrationNumber(String registrationNumber);
    
    List<Investigations> getByPrescription(Integer prescriptionId);

    List<Investigations> getByDocumentId(Integer documentId);

    void deleteById(Integer id);
}