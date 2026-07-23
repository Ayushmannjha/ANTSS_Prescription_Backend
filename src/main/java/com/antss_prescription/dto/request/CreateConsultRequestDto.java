package com.antss_prescription.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.antss_prescription.enums.ConsultationPriority;
import com.antss_prescription.enums.DiscountPolicy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConsultRequestDto {
    @NotNull
    private Integer registrationId;

    @NotNull
    private UUID doctorId;

    private Integer vitalId;

    private ConsultationPriority priority = ConsultationPriority.ROUTINE;

    @Size(max = 1000)
    private String consultReason;

    private DiscountPolicy discountPolicy = DiscountPolicy.NONE;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountValue = BigDecimal.ZERO;
}
