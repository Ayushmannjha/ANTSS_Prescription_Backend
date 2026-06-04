package com.antss_prescription.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class AddDoctorAddonRequest {

    @NotNull
    private UUID userSubscriptionId;

    @NotNull
    @Min(1)
    private Integer additionalDoctors;
    
    
    private Long entityId;
}
