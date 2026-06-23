package com.antss_prescription.dto.request;

import com.antss_prescription.enums.DurationType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreatePackageRequest {

    @NotBlank
    @Size(max = 100)
    private String packageName;

    @NotNull
    private DurationType durationType;

    @NotNull
    @Min(1)
    private Integer baseDoctorLimit;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal packagePrice;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal extraDoctorPrice;

    @Size(max = 2000)
    private String features;

    private boolean active = true;
}
