package com.antss_prescription.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestRequestedResponse {
    private Integer id;
    private String testName;
    private String notes;
    private Integer registrationId;
    private Integer prescriptionId;
    private Integer documentId;
    private String documentFileName;
    private String documentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
