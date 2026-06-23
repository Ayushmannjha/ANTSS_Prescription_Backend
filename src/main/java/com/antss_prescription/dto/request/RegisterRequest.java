package com.antss_prescription.dto.request;

import com.antss_prescription.enums.UserType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String mobileNumber;

    @NotNull
    private UserType userType;

    @NotBlank
    @Size(max = 150)
    private String entityName; // Will be mapped to hospitalName or clinicName depending on userType

    @Size(max = 255)
    private String addressLine1;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String state;
    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "must be a valid 6-digit pincode")
    private String pincode;

    @NotNull
    @Positive
    private Long packageId;

    @PositiveOrZero
    private Integer allowedHospitals;
    @PositiveOrZero
    private Integer allowedClinics;
    @PositiveOrZero
    private Integer allowedDoctors;
}
