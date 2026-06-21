package com.antss_prescription.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddDoctorAddonRequest {

    @NotNull
    private UUID userSubscriptionId;

    @NotNull
    @Positive
    private Integer additionalDoctors;
    
    
    @Positive
    private Long entityId;
}
