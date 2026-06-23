package com.antss_prescription.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRequest {
    @NotBlank @Size(max = 100)
    private String patientName;
    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
    private String mobileNumber;
    @NotBlank @Size(max = 20)
    private String gender;
    @Size(max = 20)
    private String dateOfBirth;
    @Min(0) @Max(150)
    private int age;
    @Size(max = 255)
    private String address;
    @Size(max = 100)
    private String state;
    @Size(max = 100)
    private String city;
    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "must be a valid 6-digit pincode")
    private String pincode;
}
