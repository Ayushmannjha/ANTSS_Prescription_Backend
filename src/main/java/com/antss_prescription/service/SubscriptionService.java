package com.antss_prescription.service;

import com.antss_prescription.dto.request.AddDoctorAddonRequest;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.SubscriptionResponse;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    DoctorAddonResponse requestAddonDoctors(AddDoctorAddonRequest request, UUID userId);
    List<SubscriptionResponse> listActiveSubscriptions(UUID userId);
    List<DoctorAddonResponse> listAddonRequests(UUID userId);
}
