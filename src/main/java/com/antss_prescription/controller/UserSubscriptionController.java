package com.antss_prescription.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.dto.request.UserSubscriptionSummaryDto;
import com.antss_prescription.service.UserSubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/subscriptions")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final UserSubscriptionService subscriptionService;

    /**
     * GET /api/v1/subscriptions/{userId}/summary
     *
     * Returns the full subscription snapshot for a user:
     * package info, status, days remaining, doctor quota,
     * all addons (pending + approved), doctor allocations,
     * and linked hospitals / clinics.
     *
     * Access: ADMIN or the user themselves.
     */
    @GetMapping("/{userId}/summary")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<UserSubscriptionSummaryDto> getSubscriptionSummary(
            @PathVariable UUID userId) {

        UserSubscriptionSummaryDto summary =
                subscriptionService.getUserSubscriptionSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/subscriptions/{userId}/valid
     *
     * Quick boolean check — is the subscription active and paid?
     * Used internally by other services before allowing doctor adds, etc.
     */
    @GetMapping("/{userId}/valid")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Boolean> isSubscriptionValid(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.hasValidSubscription(userId));
    }

    /**
     * GET /api/v1/subscriptions/{userId}/doctor-slots
     *
     * Returns how many more doctors can still be added.
     */
    @GetMapping("/{userId}/doctor-slots")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Integer> getRemainingDoctorSlots(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getRemainingDoctorSlots(userId));
    }
}