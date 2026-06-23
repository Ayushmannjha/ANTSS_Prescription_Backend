package com.antss_prescription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Size(max = 500)
    private String token;
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
    @NotBlank
    private String confirmPassword;

    @AssertTrue(message = "newPassword and confirmPassword must match")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
