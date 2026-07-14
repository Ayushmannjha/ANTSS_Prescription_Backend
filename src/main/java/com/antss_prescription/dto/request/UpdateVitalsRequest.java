package com.antss_prescription.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVitalsRequest {

    @PositiveOrZero
    private int height;

    @PositiveOrZero
    private double weight;

    @JsonAlias("temperature")
    @PositiveOrZero
    private double temprature;

    @PositiveOrZero
    private double pulse;

    @PositiveOrZero
    private double spo2;

    @Size(max = 20)
    private String bp;

    @PositiveOrZero
    private double respiratoryRate;
}
