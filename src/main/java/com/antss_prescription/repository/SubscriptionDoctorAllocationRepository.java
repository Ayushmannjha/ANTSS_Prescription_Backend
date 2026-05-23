package com.antss_prescription.repository;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.SubscriptionDoctorAllocation;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionDoctorAllocationRepository extends JpaRepository<SubscriptionDoctorAllocation, Long> {
    List<SubscriptionDoctorAllocation> findByUserSubscriptionAndStatus(UserSubscription subscription, AllocationStatus status);
    List<SubscriptionDoctorAllocation> findByUserSubscriptionIdAndStatus(UUID subscriptionId, AllocationStatus status);
    Optional<SubscriptionDoctorAllocation> findByUserSubscriptionIdAndDoctorIdAndStatus(UUID subscriptionId, UUID doctorId, AllocationStatus status);
    List<SubscriptionDoctorAllocation> findByDoctorAndStatus(Doctor doctor, AllocationStatus status);
}
