package com.antss_prescription.dto.response;

import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorAddonResponse {
    private Long id;
    private UUID userSubscriptionId;
    private Integer additionalDoctors;
    private BigDecimal yearlyPricePerDoctor;
    private Integer remainingMonths;
    private BigDecimal prorataAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private PaymentStatus paymentStatus;
    private AddonApprovalStatus approvalStatus;
    private UUID approvedByUserId;
    private LocalDateTime approvedAt;
}
