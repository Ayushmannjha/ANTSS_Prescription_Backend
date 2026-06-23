package com.antss_prescription.repository;

import com.antss_prescription.entity.SubscriptionPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<SubscriptionPackage, Long> {
    boolean existsByPackageName(String packageName);
}
