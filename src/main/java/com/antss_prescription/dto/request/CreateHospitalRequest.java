package com.antss_prescription.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateHospitalRequest {

    @NotBlank
    @Size(max = 150)
    private String hospitalName;

    @Size(max = 255)
    private String addressLine1;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String state;
    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "must be a valid 6-digit pincode")
    private String pincode;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String mobileNumber;
}
