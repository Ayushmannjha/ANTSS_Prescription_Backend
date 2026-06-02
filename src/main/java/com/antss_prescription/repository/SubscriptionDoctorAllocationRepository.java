package com.antss_prescription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.SubscriptionDoctorAllocation;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AllocationStatus;

@Repository
public interface SubscriptionDoctorAllocationRepository extends JpaRepository<SubscriptionDoctorAllocation, Long> {
    List<SubscriptionDoctorAllocation> findByUserSubscriptionAndStatus(UserSubscription subscription, AllocationStatus status);
    List<SubscriptionDoctorAllocation> findByUserSubscriptionIdAndStatus(UUID subscriptionId, AllocationStatus status);
    Optional<SubscriptionDoctorAllocation> findByUserSubscriptionIdAndDoctorIdAndStatus(UUID subscriptionId, UUID doctorId, AllocationStatus status);
    List<SubscriptionDoctorAllocation> findByDoctorAndStatus(Doctor doctor, AllocationStatus status);
    
    @Query("""
            SELECT sda FROM SubscriptionDoctorAllocation sda
            JOIN FETCH sda.doctor d
            WHERE sda.userSubscription.id = :subscriptionId
              AND sda.status = 'ACTIVE'
            ORDER BY sda.allocatedAt DESC
            """)
    List<SubscriptionDoctorAllocation> findActiveBySubscriptionId(
            @Param("subscriptionId") UUID subscriptionId);
}
