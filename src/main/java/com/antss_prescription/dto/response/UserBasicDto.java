package com.antss_prescription.dto.response;

import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UserBasicDto {
    // User
    private UUID   userId;
    private String fullName;
    private String email;
    private String role;

    // Subscription
    private UUID               subscriptionId;
    private String             packageName;
    private LocalDate          startDate;
    private LocalDate          endDate;
    private long               daysRemaining;
    private SubscriptionStatus subscriptionStatus;
    private PaymentStatus      paymentStatus;

    // Quota (just the numbers, no addon breakdown)
    private int allowedDoctors;
    private int usedDoctors;
    private int allowedHospitals;
    private int allowedClinics;
}