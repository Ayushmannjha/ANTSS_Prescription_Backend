package com.antss_prescription.dto.response;

import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {
    private UUID id;
    private UUID userId;
    private Long packageId;
    private String packageName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allowedDoctors;
    private Integer usedDoctors;
    private PaymentStatus paymentStatus;
    private SubscriptionStatus subscriptionStatus;
}
