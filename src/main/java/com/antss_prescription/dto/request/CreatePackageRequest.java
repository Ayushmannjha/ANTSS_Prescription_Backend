package com.antss_prescription.dto.request;

import com.antss_prescription.enums.DurationType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreatePackageRequest {

    @NotBlank
    private String packageName;

    @NotNull
    private DurationType durationType;

    @NotNull
    @Min(1)
    private Integer baseDoctorLimit;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal packagePrice;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal extraDoctorPrice;

    private String features;

    private boolean active = true;
}
