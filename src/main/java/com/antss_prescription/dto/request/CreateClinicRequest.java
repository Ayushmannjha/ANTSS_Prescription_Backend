package com.antss_prescription.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClinicRequest {

    @NotBlank
    private String clinicName;

    private String addressLine1;
    private String city;
    private String state;
    private String pincode;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String mobileNumber;
}
