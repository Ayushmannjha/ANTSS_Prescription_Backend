package com.antss_prescription.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class VitalsResponseDto {
    private int vitalId;
    private Integer registrationId;
    private UUID recordedByRmoId;
    private String recordedByRmoName;
    private int height;
    private double weight;
    private double temperature;
    private double pulse;
    private double spo2;
    private String bp;
    private double respiratoryRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
