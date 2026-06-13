package com.antss_prescription.controller;


import com.antss_prescription.docs.service.dto.DocumentDto;
import com.antss_prescription.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DocumentType;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentDto> uploadDocument(
            @PathVariable Integer patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type) {

        DocumentDto document = documentService.uploadDocument(patientId, file, type);

        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getPatientDocuments(@PathVariable Integer patientId) {

        List<DocumentDto> documents = documentService.getPatientDocuments(patientId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable Integer patientId, @PathVariable Integer documentId) {

        DocumentDto document = documentService.getDocument(patientId, documentId);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Integer patientId, @PathVariable Integer documentId) {

        documentService.deleteDocument(patientId, documentId);
        return ResponseEntity.noContent().build();
    }
}
