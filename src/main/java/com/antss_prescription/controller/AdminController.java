package com.antss_prescription.controller;

import com.antss_prescription.dto.request.ExtendValidityRequest;
import com.antss_prescription.dto.request.ModifyPackageRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.security.ApprovalTokenUtils;
import com.antss_prescription.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin APIs", description = "Admin workflow for user approvals and management")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.admin.email}")
    private String adminEmail;

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

    @GetMapping(value = "/approve-email", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
            summary = "One-click approval from admin email link (token-secured, no login needed)",
            security = {}
    )
    public ResponseEntity<String> approveViaEmail(
            @RequestParam UUID userId,
            @RequestParam String token) {

        boolean valid = ApprovalTokenUtils.verifyToken(
                userId.toString(), adminEmail, jwtSecret, token);

        if (!valid) {
            return ResponseEntity.status(403)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildResultPage(
                            false,
                            "Invalid or Expired Link",
                            "This approval link is invalid or has already been used. Please log in to the admin panel to manage registrations manually."
                    ));
        }

        try {
            adminService.approveUser(userId);
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildResultPage(
                            false,
                            "Approval Failed",
                            ex.getMessage()
                    ));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(buildResultPage(
                        true,
                        "Registration Approved!",
                        "The user has been successfully approved. Their subscription is now active and their login credentials have been emailed to them."
                ));
    }

   private String buildResultPage(boolean success, String title, String message) {
        String iconColor   = success ? "#10b981" : "#ef4444";
        String borderColor = success ? "rgba(16,185,129,0.3)" : "rgba(239,68,68,0.3)";
        String icon        = success ? "✓" : "✕";
        String badgeBg     = success ? "rgba(16,185,129,0.15)" : "rgba(239,68,68,0.15)";

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "  <title>" + title + " – Antss Prescription</title>" +
                "  <style>" +
                "    *{box-sizing:border-box;margin:0;padding:0}" +
                "    body{min-height:100vh;display:flex;align-items:center;justify-content:center;" +
                "         background:#0f172a;font-family:'Inter',system-ui,sans-serif;color:#f8fafc;padding:24px}" +
                "    .card{background:#1e293b;border:1px solid rgba(255,255,255,0.07);border-radius:24px;" +
                "          box-shadow:0 24px 48px rgba(0,0,0,0.4);max-width:500px;width:100%;padding:48px 40px;text-align:center}" +
                "    .icon-wrap{width:72px;height:72px;border-radius:50%;border:2px solid " + borderColor + ";" +
                "               background:" + badgeBg + ";display:flex;align-items:center;justify-content:center;margin:0 auto 28px}" +
                "    .icon{font-size:32px;font-weight:700;color:" + iconColor + "}" +
                "    .brand{font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:2px;" +
                "           color:#64748b;margin-bottom:12px}" +
                "    h1{font-size:26px;font-weight:800;letter-spacing:-0.5px;margin-bottom:16px;color:#f1f5f9}" +
                "    p{font-size:15px;line-height:1.7;color:#94a3b8}" +
                "    .footer{margin-top:36px;font-size:12px;color:#475569;border-top:1px solid rgba(255,255,255,0.05);padding-top:20px}" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"card\">" +
                "    <div class=\"icon-wrap\"><span class=\"icon\">" + icon + "</span></div>" +
                "    <div class=\"brand\">Antss Prescription · Admin Action</div>" +
                "    <h1>" + title + "</h1>" +
                "    <p>" + message + "</p>" +
                "    <div class=\"footer\">&copy; 2026 Antss Prescription. All rights reserved.</div>" +
                "  </div>" +
                "</body></html>";
    }
}
