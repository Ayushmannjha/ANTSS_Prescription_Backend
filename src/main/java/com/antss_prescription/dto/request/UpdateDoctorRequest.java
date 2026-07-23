package com.antss_prescription.dto.request;

import com.antss_prescription.enums.EntityStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateDoctorRequest {

    @NotBlank
    @Size(max = 100)
    private String doctorName;

    @NotBlank
    @Size(max = 100)
    private String specialization;

    @NotBlank
    @Size(max = 100)
    private String qualification;

    @NotNull
    @Min(0)
    private Integer experienceYears;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String mobileNumber;

    @NotBlank
    @Size(max = 100)
    private String registrationNumber;

    @Size(max = 2048)
    private String signatureUrl;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal consultationFee;

    @NotNull
    private EntityStatus status;
}
