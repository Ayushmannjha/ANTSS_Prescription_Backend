package com.antss_prescription.service;

import com.antss_prescription.entity.prescription.Investigations;
import com.antss_prescription.dto.request.InvestigationUploadRequest;
import com.antss_prescription.dto.response.InvestigationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InvestigationsService {

    Investigations save(Investigations investigations);

    InvestigationResponse saveWithDocument(InvestigationUploadRequest request, MultipartFile document);

    List<Investigations> getByRegistrationNumber(String registrationNumber);
    
    List<Investigations> getByPrescription(Integer prescriptionId);

    List<Investigations> getByDocumentId(Integer documentId);

    void deleteById(Integer id);
}
