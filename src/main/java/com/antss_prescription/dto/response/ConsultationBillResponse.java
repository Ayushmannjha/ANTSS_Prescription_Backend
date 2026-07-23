package com.antss_prescription.dto.response;

import com.antss_prescription.enums.DiscountPolicy;
import com.antss_prescription.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConsultationBillResponse {
    private Long billId;
    private String billNumber;
    private Integer consultationId;
    private String consultationNumber;
    private UUID doctorId;
    private String doctorName;
    private Integer registrationId;
    private String registrationNumber;
    private String patientName;
    private String patientMobileNumber;
    private BigDecimal consultationFee;
    private DiscountPolicy discountPolicy;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
