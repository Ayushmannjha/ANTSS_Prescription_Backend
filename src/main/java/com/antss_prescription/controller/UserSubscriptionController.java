package com.antss_prescription.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.dto.request.UserSubscriptionSummaryDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAddonDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAllocationDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.FacilityDto;
import com.antss_prescription.dto.response.UserBasicDto;
import com.antss_prescription.enums.SubscriptionStatus;
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
        System.out.println("====request comes======");
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
    @GetMapping("/get-all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserBasicDto>> getAllUsers() {
        return ResponseEntity.ok(subscriptionService.getAllUsers());
    }

    // =========================================================================
    // 1. SUBSCRIPTION SUMMARY & QUERY
    // =========================================================================

    @GetMapping("/{userId}/history")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionHistory(userId));
    }

    @GetMapping("/expiring-within/{withinDays}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionsExpiringWithin(@PathVariable int withinDays) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsExpiringWithin(withinDays));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionsByStatus(@PathVariable SubscriptionStatus status) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByStatus(status));
    }

    // =========================================================================
    // 2. SUBSCRIPTION VALIDITY GUARDS
    // =========================================================================

    @GetMapping("/{userId}/can-add-doctor")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Boolean> canAddDoctor(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.canAddDoctor(userId));
    }

    @GetMapping("/{userId}/can-add-hospital")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Boolean> canAddHospital(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.canAddHospital(userId));
    }

    @GetMapping("/{userId}/can-add-clinic")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Boolean> canAddClinic(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.canAddClinic(userId));
    }

    @GetMapping("/{userId}/can-create-prescription")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Boolean> canCreatePrescription(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.canCreatePrescription(userId));
    }

    // =========================================================================
    // 3. DOCTOR QUOTA
    // =========================================================================

    @GetMapping("/{userId}/effective-allowed-doctors")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Integer> getEffectiveAllowedDoctors(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getEffectiveAllowedDoctors(userId));
    }

    @GetMapping("/{userId}/used-doctor-count")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Integer> getUsedDoctorCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getUsedDoctorCount(userId));
    }

    @PostMapping("/{userId}/increment-used-doctors")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Void> incrementUsedDoctors(@PathVariable UUID userId) {
        subscriptionService.incrementUsedDoctors(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/decrement-used-doctors")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Void> decrementUsedDoctors(@PathVariable UUID userId) {
        subscriptionService.decrementUsedDoctors(userId);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // 4. SUBSCRIPTION LIFECYCLE
    // =========================================================================

    @PostMapping("/{userId}/create")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<UUID> createSubscription(@PathVariable UUID userId, @RequestParam Long packageId) {
        return ResponseEntity.ok(subscriptionService.createSubscription(userId, packageId));
    }

    @PostMapping("/{subscriptionId}/renew")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<UserSubscriptionSummaryDto> renewSubscription(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(subscriptionService.renewSubscription(subscriptionId));
    }

    @PostMapping("/{userId}/upgrade")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<UserSubscriptionSummaryDto> upgradeSubscription(@PathVariable UUID userId, @RequestParam Long newPackageId) {
        return ResponseEntity.ok(subscriptionService.upgradeSubscription(userId, newPackageId));
    }

    @PostMapping("/{userId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Void> cancelSubscription(@PathVariable UUID userId, @RequestParam UUID cancelledBy) {
        subscriptionService.cancelSubscription(userId, cancelledBy);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspendSubscription(@PathVariable UUID subscriptionId, @RequestParam UUID suspendedBy) {
        subscriptionService.suspendSubscription(subscriptionId, suspendedBy);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateSubscription(@PathVariable UUID subscriptionId, @RequestParam UUID reactivatedBy) {
        subscriptionService.reactivateSubscription(subscriptionId, reactivatedBy);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> expireSubscription(@PathVariable UUID subscriptionId) {
        subscriptionService.expireSubscription(subscriptionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expire-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> expireAllOverdueSubscriptions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        LocalDate date = (asOf != null) ? asOf : LocalDate.now();
        return ResponseEntity.ok(subscriptionService.expireAllOverdueSubscriptions(date));
    }

    // =========================================================================
    // 5. PAYMENT STATUS
    // =========================================================================

    @PostMapping("/{subscriptionId}/payment-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markPaymentPaid(@PathVariable UUID subscriptionId, @RequestParam(required = false) String transactionRef) {
        subscriptionService.markPaymentPaid(subscriptionId, transactionRef);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/payment-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markPaymentFailed(@PathVariable UUID subscriptionId, @RequestParam(required = false) String reason) {
        subscriptionService.markPaymentFailed(subscriptionId, reason);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // 6. DOCTOR ADDONS
    // =========================================================================

    @PostMapping("/{subscriptionId}/addons/request")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Long> requestDoctorAddon(@PathVariable UUID subscriptionId, @RequestParam int additionalDoctors) {
        return ResponseEntity.ok(subscriptionService.requestDoctorAddon(subscriptionId, additionalDoctors));
    }

    @PostMapping("/addons/{addonId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveDoctorAddon(@PathVariable Long addonId, @RequestParam UUID approvedBy) {
        subscriptionService.approveDoctorAddon(addonId, approvedBy);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/addons/{addonId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectDoctorAddon(@PathVariable Long addonId, @RequestParam UUID rejectedBy, @RequestParam(required = false) String reason) {
        subscriptionService.rejectDoctorAddon(addonId, rejectedBy, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/addons/{addonId}/payment-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAddonPaymentPaid(@PathVariable Long addonId, @RequestParam(required = false) String transactionRef) {
        subscriptionService.markAddonPaymentPaid(addonId, transactionRef);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{subscriptionId}/addons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<DoctorAddonDto>> getAddonsForSubscription(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getAddonsForSubscription(subscriptionId));
    }

    @GetMapping("/{subscriptionId}/addons/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<DoctorAddonDto>> getActiveAddonsForSubscription(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getActiveAddonsForSubscription(subscriptionId));
    }

    // =========================================================================
    // 7. DOCTOR ALLOCATIONS
    // =========================================================================

    @PostMapping("/{subscriptionId}/allocate-doctor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> allocateDoctor(@PathVariable UUID subscriptionId, @RequestParam UUID doctorId, @RequestParam String allocationType) {
        subscriptionService.allocateDoctor(subscriptionId, doctorId, allocationType);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subscriptionId}/deallocate-doctor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> deallocateDoctor(@PathVariable UUID subscriptionId, @RequestParam UUID doctorId) {
        subscriptionService.deallocateDoctor(subscriptionId, doctorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{subscriptionId}/allocations/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<DoctorAllocationDto>> getActiveDoctorAllocations(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getActiveDoctorAllocations(subscriptionId));
    }

    @GetMapping("/{subscriptionId}/allocations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<DoctorAllocationDto>> getAllDoctorAllocations(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getAllDoctorAllocations(subscriptionId));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionsForDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsForDoctor(doctorId));
    }

    // =========================================================================
    // 8. FACILITY LINKAGE
    // =========================================================================

    @GetMapping("/{userId}/linked-hospitals")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<FacilityDto>> getLinkedHospitals(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getLinkedHospitals(userId));
    }

    @GetMapping("/{userId}/linked-clinics")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<FacilityDto>> getLinkedClinics(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getLinkedClinics(userId));
    }

    @GetMapping("/{userId}/hospital-count")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Integer> getHospitalCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getHospitalCount(userId));
    }

    @GetMapping("/{userId}/clinic-count")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Integer> getClinicCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getClinicCount(userId));
    }

    // =========================================================================
    // 9. ADMIN / REPORTING
    // =========================================================================

    @GetMapping("/active-subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getAllActiveSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllActiveSubscriptions());
    }

    @GetMapping("/package/{packageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionsByPackage(@PathVariable Long packageId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByPackage(packageId));
    }

    @GetMapping("/pending-addons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSubscriptionSummaryDto>> getSubscriptionsWithPendingAddons() {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsWithPendingAddons());
    }

    @GetMapping("/count/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countActiveSubscriptions() {
        return ResponseEntity.ok(subscriptionService.countActiveSubscriptions());
    }

    @GetMapping("/count/allocated-doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countTotalAllocatedDoctors() {
        return ResponseEntity.ok(subscriptionService.countTotalAllocatedDoctors());
    }
}