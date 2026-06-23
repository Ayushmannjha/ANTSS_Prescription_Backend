package com.antss_prescription.dto.request;

import com.antss_prescription.enums.FacilityType;
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
    
    
    @NotNull
    @Positive
    private Long entityId;

    @NotNull
    private FacilityType entityType;
}
