package com.antss_prescription.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.antss_prescription.dto.request.UserSubscriptionSummaryDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAddonDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.DoctorAllocationDto;
import com.antss_prescription.dto.request.UserSubscriptionSummaryDto.FacilityDto;
import com.antss_prescription.dto.response.UserBasicDto;
import com.antss_prescription.enums.SubscriptionStatus;

public interface UserSubscriptionService {

    // =========================================================================
    // 1. SUBSCRIPTION SUMMARY & QUERY
    // =========================================================================

    /**
     * Full snapshot: package info, status, days left, doctor quota,
     * all addons, allocated doctors, linked hospitals & clinics.
     *
     * @param userId target user
     * @return complete subscription summary
     * @throws UserNotFoundException           if user does not exist
     * @throws NoActiveSubscriptionException   if user has no active subscription
     */
    UserSubscriptionSummaryDto getUserSubscriptionSummary(UUID userId);

    /**
     * Returns subscription history for a user across all packages,
     * ordered by start date descending.
     */
    List<UserSubscriptionSummaryDto> getSubscriptionHistory(UUID userId);

    /**
     * Fetch all users whose subscriptions expire within the given number of days.
     * Useful for sending renewal reminders.
     *
     * @param withinDays look-ahead window (e.g. 7, 15, 30)
     */
    List<UserSubscriptionSummaryDto> getSubscriptionsExpiringWithin(int withinDays);

    /**
     * Returns all subscriptions currently in the given status.
     * E.g. ACTIVE, EXPIRED, SUSPENDED, CANCELLED.
     */
    List<UserSubscriptionSummaryDto> getSubscriptionsByStatus(SubscriptionStatus status);

    // =========================================================================
    // 2. SUBSCRIPTION VALIDITY GUARDS
    //    Lightweight boolean / integer checks — call these before any write op.
    // =========================================================================

    /**
     * True if the user has an ACTIVE subscription that is not expired and
     * whose payment status is PAID.
     */
    boolean hasValidSubscription(UUID userId);

    /**
     * True if the subscription is valid AND the user still has at least one
     * free doctor slot (usedDoctors < effectiveAllowedDoctors).
     */
    boolean canAddDoctor(UUID userId);

    /**
     * True if the subscription is valid AND the user can still add a hospital
     * (linked hospital count < allowedHospitals).
     */
    boolean canAddHospital(UUID userId);

    /**
     * True if the subscription is valid AND the user can still add a clinic
     * (linked clinic count < allowedClinics).
     */
    boolean canAddClinic(UUID userId);

    /**
     * True if the subscription is valid AND the user can still write prescriptions.
     * (Hook for future per-prescription limits; currently delegates to hasValidSubscription.)
     */
    boolean canCreatePrescription(UUID userId);

    // =========================================================================
    // 3. DOCTOR QUOTA
    // =========================================================================

    /**
     * How many more doctors can this user add right now.
     * Returns 0 if quota is exhausted or no active subscription exists.
     */
    int getRemainingDoctorSlots(UUID userId);

    /**
     * Effective doctor ceiling = package base limit + all approved & paid
     * addon doctors whose end date has not passed.
     */
    int getEffectiveAllowedDoctors(UUID userId);

    /**
     * Number of doctors currently marked as used/allocated on the active subscription.
     */
    int getUsedDoctorCount(UUID userId);

    /**
     * Increments usedDoctors by 1 on the active subscription.
     * Called when a doctor is successfully linked/allocated.
     *
     * @throws NoActiveSubscriptionException if no active subscription
     * @throws DoctorQuotaExceededException  if usedDoctors already equals effective ceiling
     */
    void incrementUsedDoctors(UUID userId);

    /**
     * Decrements usedDoctors by 1 (minimum 0).
     * Called when a doctor is removed / deallocated.
     */
    void decrementUsedDoctors(UUID userId);

    // =========================================================================
    // 4. SUBSCRIPTION LIFECYCLE
    // =========================================================================

    /**
     * Creates a new subscription for the user on the given package.
     * Sets start date to today, end date based on the package duration type.
     * Sets paymentStatus = PENDING, subscriptionStatus = ACTIVE.
     *
     * @param userId    the subscribing user
     * @param packageId the chosen subscription package
     * @return UUID of the newly created subscription
     * @throws UserNotFoundException    if user not found
     * @throws PackageNotFoundException if package not found or inactive
     * @throws DuplicateSubscriptionException if user already has an ACTIVE subscription
     */
    UUID createSubscription(UUID userId, Long packageId);

    /**
     * Renews an existing subscription by extending its end date by one
     * package duration period from today (or from current end date, whichever is later).
     * Resets paymentStatus to PENDING.
     *
     * @param subscriptionId the subscription to renew
     * @return the updated subscription summary
     */
    UserSubscriptionSummaryDto renewSubscription(UUID subscriptionId);

    /**
     * Upgrades a user to a different package mid-cycle.
     * Prorates the remaining value and adjusts doctor limits accordingly.
     *
     * @param userId        the user
     * @param newPackageId  the target package
     * @return updated summary
     */
    UserSubscriptionSummaryDto upgradeSubscription(UUID userId, Long newPackageId);

    /**
     * Cancels the active subscription immediately.
     * Sets subscriptionStatus = CANCELLED.
     *
     * @param userId       the user whose subscription to cancel
     * @param cancelledBy  the admin/user performing the cancellation
     */
    void cancelSubscription(UUID userId, UUID cancelledBy);

    /**
     * Suspends the active subscription (e.g. on payment failure).
     * Sets subscriptionStatus = SUSPENDED. Doctor logins remain but
     * prescription creation is blocked.
     *
     * @param subscriptionId the subscription to suspend
     * @param suspendedBy    admin user performing the action
     */
    void suspendSubscription(UUID subscriptionId, UUID suspendedBy);

    /**
     * Re-activates a previously suspended subscription.
     *
     * @param subscriptionId the subscription to reactivate
     * @param reactivatedBy  admin user performing the action
     */
    void reactivateSubscription(UUID subscriptionId, UUID reactivatedBy);

    /**
     * Marks a subscription as EXPIRED. Typically called by a scheduled job
     * when endDate < today and status is still ACTIVE.
     */
    void expireSubscription(UUID subscriptionId);

    /**
     * Batch expiry — called by a scheduled job to expire all subscriptions
     * whose end date is before the given date and whose status is still ACTIVE.
     *
     * @param asOf the reference date (usually LocalDate.now())
     * @return number of subscriptions expired
     */
    int expireAllOverdueSubscriptions(LocalDate asOf);

    // =========================================================================
    // 5. PAYMENT STATUS
    // =========================================================================

    /**
     * Marks payment for a subscription as PAID.
     * If the subscription was SUSPENDED due to non-payment, reactivates it.
     *
     * @param subscriptionId the subscription
     * @param transactionRef optional payment gateway reference
     */
    void markPaymentPaid(UUID subscriptionId, String transactionRef);

    /**
     * Marks payment as FAILED. May trigger suspension logic.
     *
     * @param subscriptionId the subscription
     * @param reason         reason string for audit purposes
     */
    void markPaymentFailed(UUID subscriptionId, String reason);

    // =========================================================================
    // 6. DOCTOR ADDONS
    // =========================================================================

    /**
     * Creates a new doctor addon request (approvalStatus = PENDING,
     * paymentStatus = PENDING). Prorata amount is computed automatically
     * based on remaining months in the subscription cycle.
     *
     * @param subscriptionId     the parent subscription
     * @param additionalDoctors  number of extra doctors requested
     * @return id of the created DoctorAddon
     */
    Long requestDoctorAddon(UUID subscriptionId, int additionalDoctors);

    /**
     * Admin approves a pending doctor addon. Sets approvalStatus = APPROVED,
     * records approvedBy and approvedAt.
     * The addon only becomes effective after payment is confirmed via
     * {@link #markAddonPaymentPaid(Long, String)}.
     *
     * @param addonId    the addon to approve
     * @param approvedBy the admin user approving it
     */
    void approveDoctorAddon(Long addonId, UUID approvedBy);

    /**
     * Admin rejects a pending doctor addon. Sets approvalStatus = REJECTED.
     *
     * @param addonId    the addon to reject
     * @param rejectedBy the admin user rejecting it
     * @param reason     reason for rejection (stored for audit)
     */
    void rejectDoctorAddon(Long addonId, UUID rejectedBy, String reason);

    /**
     * Marks addon payment as PAID and updates the subscription's allowedDoctors
     * ceiling by adding the addon's additionalDoctors count.
     *
     * @param addonId        the addon whose payment is confirmed
     * @param transactionRef payment reference
     */
    void markAddonPaymentPaid(Long addonId, String transactionRef);

    /**
     * All addons for a given subscription, regardless of status.
     */
    List<DoctorAddonDto> getAddonsForSubscription(UUID subscriptionId);

    /**
     * Only addons that are currently active (APPROVED + PAID + not expired).
     */
    List<DoctorAddonDto> getActiveAddonsForSubscription(UUID subscriptionId);

    // =========================================================================
    // 7. DOCTOR ALLOCATIONS
    // =========================================================================

    /**
     * Allocates a doctor to a subscription. Validates quota before allocating.
     * Creates a SubscriptionDoctorAllocation record and increments usedDoctors.
     *
     * @param subscriptionId the subscription
     * @param doctorId       the doctor to allocate
     * @param allocationType BASE or ADDON
     * @throws DoctorQuotaExceededException if no slots remain
     * @throws DoctorAlreadyAllocatedException if doctor is already active on this subscription
     */
    void allocateDoctor(UUID subscriptionId, UUID doctorId, String allocationType);

    /**
     * Deallocates a doctor from a subscription.
     * Sets the allocation status to INACTIVE and decrements usedDoctors.
     *
     * @param subscriptionId the subscription
     * @param doctorId       the doctor to remove
     */
    void deallocateDoctor(UUID subscriptionId, UUID doctorId);

    /**
     * All currently active allocations for a subscription.
     */
    List<DoctorAllocationDto> getActiveDoctorAllocations(UUID subscriptionId);

    /**
     * Full allocation history (active + inactive) for a subscription.
     */
    List<DoctorAllocationDto> getAllDoctorAllocations(UUID subscriptionId);

    /**
     * All subscriptions a given doctor is currently allocated to.
     * Useful when a doctor is shared across hospital + clinic.
     */
    List<UserSubscriptionSummaryDto> getSubscriptionsForDoctor(UUID doctorId);

    // =========================================================================
    // 8. FACILITY LINKAGE
    // =========================================================================

    /**
     * All hospitals linked to the user's active subscription.
     */
    List<FacilityDto> getLinkedHospitals(UUID userId);

    /**
     * All clinics linked to the user's active subscription.
     */
    List<FacilityDto> getLinkedClinics(UUID userId);

    /**
     * Total count of hospitals the user has created/owns.
     * Used to enforce the allowedHospitals limit.
     */
    int getHospitalCount(UUID userId);

    /**
     * Total count of clinics the user has created/owns.
     * Used to enforce the allowedClinics limit.
     */
    int getClinicCount(UUID userId);

    // =========================================================================
    // 9. ADMIN / REPORTING
    // =========================================================================

    /**
     * All active subscriptions across all users — for admin dashboard.
     */
    List<UserSubscriptionSummaryDto> getAllActiveSubscriptions();

    /**
     * All subscriptions on a given package — useful for package deprecation checks.
     */
    List<UserSubscriptionSummaryDto> getSubscriptionsByPackage(Long packageId);

    /**
     * All subscriptions that have pending (unapproved) doctor addon requests.
     * Useful for the admin approval queue.
     */
    List<UserSubscriptionSummaryDto> getSubscriptionsWithPendingAddons();

    /**
     * Total number of active subscriptions across the platform.
     */
    long countActiveSubscriptions();

    /**
     * Total number of active (allocated) doctors across all subscriptions.
     */
    long countTotalAllocatedDoctors();
    
 // In section 9. ADMIN / REPORTING

    /**
     * Returns a summary snapshot for every user in the system.
     * Users with no active subscription will have null subscription fields.
     *
     * @return list of all user subscription summaries
     */
    List<UserBasicDto> getAllUsers();

    // =========================================================================
    // EXCEPTION TYPES
    // =========================================================================

    class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID id) {
            super("User not found: " + id);
        }
    }

    class NoActiveSubscriptionException extends RuntimeException {
        public NoActiveSubscriptionException(UUID userId) {
            super("No active subscription found for user: " + userId);
        }
    }

    class PackageNotFoundException extends RuntimeException {
        public PackageNotFoundException(Long packageId) {
            super("Subscription package not found or inactive: " + packageId);
        }
    }

    class DuplicateSubscriptionException extends RuntimeException {
        public DuplicateSubscriptionException(UUID userId) {
            super("User already has an active subscription: " + userId);
        }
    }

    class DoctorQuotaExceededException extends RuntimeException {
        public DoctorQuotaExceededException(UUID userId) {
            super("Doctor quota exhausted for user: " + userId);
        }
    }

    class DoctorAlreadyAllocatedException extends RuntimeException {
        public DoctorAlreadyAllocatedException(UUID doctorId, UUID subscriptionId) {
            super("Doctor " + doctorId + " is already allocated to subscription " + subscriptionId);
        }
    }
}