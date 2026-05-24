package com.antss_prescription.dto.request;

import com.antss_prescription.enums.UserType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String mobileNumber;

    @NotNull
    private UserType userType;

    @NotBlank
    private String entityName; // Will be mapped to hospitalName or clinicName depending on userType

    private String addressLine1;
    private String city;
    private String state;
    private String pincode;

    @NotNull
    private Long packageId;

    private Integer allowedHospitals;
    private Integer allowedClinics;
    private Integer allowedDoctors;
}
