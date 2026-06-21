package com.antss_prescription.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ModifyPackageRequest {

    @NotNull
    @Positive
    private Long packageId;
}
