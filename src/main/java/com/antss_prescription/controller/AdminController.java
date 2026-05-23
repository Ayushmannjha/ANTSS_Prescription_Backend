package com.antss_prescription.controller;

import com.antss_prescription.dto.request.ExtendValidityRequest;
import com.antss_prescription.dto.request.ModifyPackageRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin APIs", description = "Admin workflow for user approvals and management")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, UserRepository userRepository) {
        this.adminService = adminService;
        this.userRepository = userRepository;
    }

    @GetMapping("/registrations/pending")
    @Operation(summary = "Get all pending registrations")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingRegistrations() {
        List<UserResponse> responses = adminService.getPendingRegistrations();
        return ResponseEntity.ok(ApiResponse.success("Pending registrations fetched successfully", responses));
    }

    @PostMapping("/users/{id}/approve")
    @Operation(summary = "Approve a user registration")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(@PathVariable UUID id) {
        UserResponse response = adminService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.success("User approved successfully", response));
    }

    @PostMapping("/users/{id}/reject")
    @Operation(summary = "Reject a user registration")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(@PathVariable UUID id) {
        UserResponse response = adminService.rejectUser(id);
        return ResponseEntity.ok(ApiResponse.success("User rejected successfully", response));
    }

    @PutMapping("/users/{id}/package")
    @Operation(summary = "Modify a user's subscription package")
    public ResponseEntity<ApiResponse<UserResponse>> modifyPackage(@PathVariable UUID id, @Valid @RequestBody ModifyPackageRequest request) {
        UserResponse response = adminService.modifyUserPackage(id, request);
        return ResponseEntity.ok(ApiResponse.success("User package updated successfully", response));
    }

    @PutMapping("/users/{id}/extend")
    @Operation(summary = "Extend a user's subscription validity")
    public ResponseEntity<ApiResponse<UserResponse>> extendValidity(@PathVariable UUID id, @Valid @RequestBody ExtendValidityRequest request) {
        UserResponse response = adminService.extendValidity(id, request);
        return ResponseEntity.ok(ApiResponse.success("User validity extended successfully", response));
    }

    @PutMapping("/users/{id}/block")
    @Operation(summary = "Block an active user")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(@PathVariable UUID id) {
        UserResponse response = adminService.blockUser(id);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", response));
    }

    @PutMapping("/users/{id}/unblock")
    @Operation(summary = "Unblock a blocked user")
    public ResponseEntity<ApiResponse<UserResponse>> unblockUser(@PathVariable UUID id) {
        UserResponse response = adminService.unblockUser(id);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", response));
    }

    @GetMapping("/addons/pending")
    @Operation(summary = "Get all pending doctor addon requests")
    public ResponseEntity<ApiResponse<List<DoctorAddonResponse>>> getPendingAddons() {
        List<DoctorAddonResponse> responses = adminService.getPendingAddons();
        return ResponseEntity.ok(ApiResponse.success("Pending doctor addons fetched successfully", responses));
    }

    @PostMapping("/addons/{id}/approve")
    @Operation(summary = "Approve a doctor addon request")
    public ResponseEntity<ApiResponse<DoctorAddonResponse>> approveDoctorAddon(@PathVariable Long id) {
        UUID adminUserId = getCurrentAdminUserId();
        DoctorAddonResponse response = adminService.approveDoctorAddon(id, adminUserId);
        return ResponseEntity.ok(ApiResponse.success("Doctor addon request approved successfully", response));
    }

    @PostMapping("/addons/{id}/reject")
    @Operation(summary = "Reject a doctor addon request")
    public ResponseEntity<ApiResponse<DoctorAddonResponse>> rejectDoctorAddon(@PathVariable Long id) {
        UUID adminUserId = getCurrentAdminUserId();
        DoctorAddonResponse response = adminService.rejectDoctorAddon(id, adminUserId);
        return ResponseEntity.ok(ApiResponse.success("Doctor addon request rejected successfully", response));
    }

    private UUID getCurrentAdminUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", email));
        return user.getId();
    }
}
