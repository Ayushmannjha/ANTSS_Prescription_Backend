package com.antss_prescription.repository;

import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.enums.AddonApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorAddonRepository extends JpaRepository<DoctorAddon, Long> {
    List<DoctorAddon> findByUserSubscriptionId(UUID userSubscriptionId);
    List<DoctorAddon> findByApprovalStatus(AddonApprovalStatus approvalStatus);
}
