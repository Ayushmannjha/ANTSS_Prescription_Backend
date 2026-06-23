package com.antss_prescription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank
    @Size(max = 4096)
    private String refreshToken;
}
