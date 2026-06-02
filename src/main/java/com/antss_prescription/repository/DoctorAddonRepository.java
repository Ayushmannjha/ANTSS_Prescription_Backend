package com.antss_prescription.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.enums.AddonApprovalStatus;

@Repository
public interface DoctorAddonRepository extends JpaRepository<DoctorAddon, Long> {
    List<DoctorAddon> findByUserSubscriptionId(UUID userSubscriptionId);
    List<DoctorAddon> findByApprovalStatus(AddonApprovalStatus approvalStatus);
    
    @Query("""
            SELECT da FROM DoctorAddon da
            LEFT JOIN FETCH da.approvedBy
            WHERE da.userSubscription.id = :subscriptionId
            ORDER BY da.createdAt DESC
            """)
    List<DoctorAddon> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
 
    
    @Query("""
            SELECT da FROM DoctorAddon da
            WHERE da.userSubscription.id = :subscriptionId
              AND da.approvalStatus = 'APPROVED'
              AND da.paymentStatus  = 'PAID'
            """)
    List<DoctorAddon> findApprovedAndPaidBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
