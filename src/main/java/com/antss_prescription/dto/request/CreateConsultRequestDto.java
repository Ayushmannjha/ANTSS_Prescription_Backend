package com.antss_prescription.dto.request;

import java.util.UUID;

import com.antss_prescription.enums.ConsultationPriority;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConsultRequestDto {
    @NotNull
    private Integer registrationId;

    @NotNull
    private UUID doctorId;

    private Integer vitalId;

    private ConsultationPriority priority = ConsultationPriority.ROUTINE;

    @Size(max = 1000)
    private String consultReason;
}
