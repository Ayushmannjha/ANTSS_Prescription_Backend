package com.antss_prescription.dto.request;

import com.antss_prescription.enums.RmoRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateRmoRequest {

    @NotBlank
    private String rmoName;

    @NotBlank
    @Email
    private String email;

    private String mobileNumber;

    @NotBlank
    private String employeeCode;

    private Long hospitalId;
    private Long clinicId;

    @NotNull
    private RmoRole role;
}
