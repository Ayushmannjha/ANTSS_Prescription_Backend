package com.antss_prescription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicineMasterRequest {
    @NotBlank @Size(max = 255)
    private String medicineName;
    @Size(max = 255)
    private String genericName;
    @Size(max = 100)
    private String strength;
    @Size(max = 100)
    private String dosageForm;
    @Size(max = 255)
    private String manufacturer;
    private Boolean active;
}
