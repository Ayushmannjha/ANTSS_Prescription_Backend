package com.antss_prescription.controller;

import com.antss_prescription.dto.request.AddDoctorAddonRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.SubscriptionResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscription APIs", description = "User subscription billing and doctor addon management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @PostMapping("/addons")
    @Operation(summary = "Request additional doctor licenses (addon)")
    public ResponseEntity<ApiResponse<DoctorAddonResponse>> requestAddonDoctors(@Valid @RequestBody AddDoctorAddonRequest request) {
        UUID userId = getCurrentUserId();
        System.out.println("===doctor addons request==="+request);
        DoctorAddonResponse response = subscriptionService.requestAddonDoctors(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor addon requested successfully", response));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active subscriptions for the logged-in user")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> listActiveSubscriptions() {
        UUID userId = getCurrentUserId();
        List<SubscriptionResponse> responses = subscriptionService.listActiveSubscriptions(userId);
        return ResponseEntity.ok(ApiResponse.success("Active subscriptions fetched successfully", responses));
    }

    @GetMapping("/addons")
    @Operation(summary = "List all doctor addon requests for the logged-in user")
    public ResponseEntity<ApiResponse<List<DoctorAddonResponse>>> listAddonRequests() {
        UUID userId = getCurrentUserId();
        List<DoctorAddonResponse> responses = subscriptionService.listAddonRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor addon requests fetched successfully", responses));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
