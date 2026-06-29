package com.antss_prescription.controller;


import com.antss_prescription.docs.service.dto.DocumentDto;
import com.antss_prescription.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/patients/{patientId}/documents")
    public ResponseEntity<DocumentDto> uploadDocument(
            @PathVariable Integer patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "INVESTIGATION") String type) {

        DocumentDto document = documentService.uploadDocument(patientId, file, type);

        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @GetMapping("/patients/{patientId}/documents")
    public ResponseEntity<List<DocumentDto>> getPatientDocuments(@PathVariable Integer patientId) {

        List<DocumentDto> documents = documentService.getPatientDocuments(patientId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/patients/{patientId}/documents/{documentId}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable Integer patientId, @PathVariable Integer documentId) {

        DocumentDto document = documentService.getDocument(patientId, documentId);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/prescription/{prescriptionId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsByPrescription(
            @PathVariable Integer prescriptionId) {

        return ResponseEntity.ok(documentService.getDocumentsByPrescription(prescriptionId));
    }

    @GetMapping("/prescriptions/{prescriptionId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsByPrescriptionAlias(
            @PathVariable Integer prescriptionId) {

        return ResponseEntity.ok(documentService.getDocumentsByPrescription(prescriptionId));
    }

    @DeleteMapping("/patients/{patientId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Integer patientId, @PathVariable Integer documentId) {

        documentService.deleteDocument(patientId, documentId);
        return ResponseEntity.noContent().build();
    }
}
