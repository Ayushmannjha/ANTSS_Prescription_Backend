package com.antss_prescription.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class ExtendValidityRequest {

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer days;
}
