package com.antss_prescription.controller;

import com.antss_prescription.dto.request.TestRequestedUploadRequest;
import com.antss_prescription.dto.response.TestRequestedResponse;
import com.antss_prescription.service.TestRequestedService;
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
@RequestMapping("/api/test-requested")
@RequiredArgsConstructor
public class TestRequestedController {

    private final TestRequestedService testRequestedService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestRequestedResponse> saveTestRequested(
            @Valid @RequestPart("dto") TestRequestedUploadRequest dto,
            @RequestPart("document") MultipartFile document) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testRequestedService.saveWithDocument(dto, document));
    }
}
