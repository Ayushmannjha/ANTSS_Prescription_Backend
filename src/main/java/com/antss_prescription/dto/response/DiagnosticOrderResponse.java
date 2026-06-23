package com.antss_prescription.dto.response;

import java.time.LocalDateTime;
import com.antss_prescription.enums.DiagnosticStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiagnosticOrderResponse {
    private Integer id;
    private String testName;
    private String notes;
    private String resultSummary;
    private DiagnosticStatus status;
    private Integer registrationId;
    private Integer prescriptionId;
    private Integer reportDocumentId;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
