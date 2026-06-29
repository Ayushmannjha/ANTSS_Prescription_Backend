package com.antss_prescription.service;

import com.antss_prescription.entity.prescription.TestRequested;
import com.antss_prescription.dto.request.TestRequestedUploadRequest;
import com.antss_prescription.dto.response.TestRequestedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestRequestedService {

    TestRequested save(TestRequested testRequested);

    TestRequestedResponse saveWithDocument(TestRequestedUploadRequest request, MultipartFile document);

    List<TestRequested> getByRegistrationNumber(String registrationNumber);

    List<TestRequested> getByPrescription(Integer prescriptionId);

    List<TestRequested> getByDocumentId(Integer documentId);

    void deleteById(Integer id);
}
