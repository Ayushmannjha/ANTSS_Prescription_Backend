package com.antss_prescription.dto.request;

import com.antss_prescription.enums.DiscountPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsultationBillDiscountRequest {
    @NotNull
    private DiscountPolicy discountPolicy = DiscountPolicy.NONE;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountValue = BigDecimal.ZERO;
}
