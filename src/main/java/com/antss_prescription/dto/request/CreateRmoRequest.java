package com.antss_prescription.dto.request;



import com.antss_prescription.enums.RmoRole;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(min = 8, max = 72)
    private String password;

    @Positive
    private Long hospitalId;
    @Positive
    private Long clinicId;

    @NotNull
    private RmoRole role;

    @AssertTrue(message = "hospitalId and clinicId cannot both be provided")
    public boolean isFacilitySelectionValid() {
        return hospitalId == null || clinicId == null;
    }
}
