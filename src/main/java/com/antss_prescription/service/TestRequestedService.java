package com.antss_prescription.service;

import com.antss_prescription.entity.prescription.TestRequested;

import java.util.List;

public interface TestRequestedService {

    TestRequested save(TestRequested testRequested);

    List<TestRequested> getByRegistrationNumber(String registrationNumber);

    List<TestRequested> getByPrescription(Integer prescriptionId);

    List<TestRequested> getByDocumentId(Integer documentId);

    void deleteById(Integer id);
}