package com.antss_prescription.service;

import com.antss_prescription.docs.service.dto.DocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

        DocumentDto uploadDocument(Integer patientId, MultipartFile file, String type);

        List<DocumentDto> getPatientDocuments(Integer patientId);

        DocumentDto getDocument(Integer patientId, Integer documentId);

        List<DocumentDto> getDocumentsByPrescription(Integer prescriptionId);

        void deleteDocument(Integer patientId, Integer documentId);


}
