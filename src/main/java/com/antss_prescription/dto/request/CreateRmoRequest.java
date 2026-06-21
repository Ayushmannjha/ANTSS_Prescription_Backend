package com.antss_prescription.dto.request;

import com.antss_prescription.enums.RmoRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateRmoRequest {

    @NotBlank
    @Size(max = 100)
    private String rmoName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String mobileNumber;

    @NotBlank
    @Size(max = 50)
    private String employeeCode;

    @Positive
    private Long hospitalId;
    @Positive
    private Long clinicId;

    @NotNull
    private RmoRole role;
}
