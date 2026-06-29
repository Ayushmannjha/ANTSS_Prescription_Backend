package com.antss_prescription.controller;

import com.antss_prescription.dto.request.InvestigationUploadRequest;
import com.antss_prescription.dto.response.InvestigationResponse;
import com.antss_prescription.service.InvestigationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/investigations")
@RequiredArgsConstructor
public class InvestigationsController {

    private final InvestigationsService investigationsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvestigationResponse> saveInvestigation(
            @Valid @RequestPart("dto") InvestigationUploadRequest dto,
            @RequestPart("document") MultipartFile document) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investigationsService.saveWithDocument(dto, document));
    }
}
