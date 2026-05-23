package com.antss_prescription.dto.response;

import java.math.BigDecimal;
import com.antss_prescription.enums.DurationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PackageResponse {

    private Long id;
    private String packageName;
    private DurationType durationType;
    private Integer baseDoctorLimit;
    private BigDecimal packagePrice;
    private BigDecimal extraDoctorPrice;
    private String features;
    private boolean active;
}
