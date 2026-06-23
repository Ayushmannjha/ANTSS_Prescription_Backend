package com.antss_prescription.dto.request;

import com.antss_prescription.enums.DiagnosticStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiagnosticStatusRequest {
    @NotNull
    private DiagnosticStatus status;
    @Size(max = 2000)
    private String resultSummary;
    private Integer reportDocumentId;
}
