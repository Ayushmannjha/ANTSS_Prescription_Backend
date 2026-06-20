package com.antss_prescription.service.impl;

import com.antss_prescription.docs.service.CloudinaryService;
import com.antss_prescription.docs.service.dto.DocumentDto;
import com.antss_prescription.entity.prescription.Document;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.repository.prescription.DocumentRepo;
import com.antss_prescription.repository.prescription.PatientRepo;
import com.antss_prescription.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepo documentRepository;
    private final PatientRepo patientRepository;
    private final CloudinaryService cloudinaryService;
    private final ModelMapper modelMapper;

    @Override
    public DocumentDto uploadDocument(Integer patientId, MultipartFile file, String type) {

        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            String fileUrl = cloudinaryService.uploadFile(file);

            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .url(fileUrl)
                    .patient(patient)
                    .build();

            Document savedDocument = documentRepository.save(document);

            return modelMapper.map(savedDocument, DocumentDto.class);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getPatientDocuments(Integer patientId) {

        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        return documentRepository.findByPatientId(patientId)
                .stream()
                .map(document ->
                        modelMapper.map(document, DocumentDto.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocument(Integer patientId, Integer documentId) {

        Document document = documentRepository.findByIdAndPatientId(
                        documentId,
                        patientId
                ).orElseThrow(() -> new RuntimeException("Document not found"));

        return modelMapper.map(document, DocumentDto.class);
    }

    @Override
    public void deleteDocument(Integer patientId, Integer documentId) {

        Document document = documentRepository.findByIdAndPatientId(documentId, patientId).orElseThrow(() ->
                        new RuntimeException("Document not found"));

        documentRepository.delete(document);
    }
}
