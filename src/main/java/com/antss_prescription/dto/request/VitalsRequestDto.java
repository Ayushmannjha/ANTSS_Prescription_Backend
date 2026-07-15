package com.antss_prescription.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VitalsRequestDto {
    private int height;
    private double weight;
    private double temprature;
    private double pulse;
    private double spo2;

    @Size(max = 20)
    private String bp;

    private double respiratoryRate;
}
