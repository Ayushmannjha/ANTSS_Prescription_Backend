package com.antss_prescription.service;

import com.antss_prescription.dto.request.ExtendValidityRequest;
import com.antss_prescription.dto.request.ModifyPackageRequest;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface AdminService {
    List<UserResponse> getPendingRegistrations();
    UserResponse approveUser(UUID userId);
    UserResponse rejectUser(UUID userId);
    UserResponse modifyUserPackage(UUID userId, ModifyPackageRequest request);
    UserResponse extendValidity(UUID userId, ExtendValidityRequest request);
    UserResponse blockUser(UUID userId);
    UserResponse unblockUser(UUID userId);
    
    // Addon approval endpoints
    List<DoctorAddonResponse> getPendingAddons();
    DoctorAddonResponse approveDoctorAddon(Long addonId, UUID adminUserId);
    DoctorAddonResponse rejectDoctorAddon(Long addonId, UUID adminUserId);
}
