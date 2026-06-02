package com.antss_prescription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.SubscriptionStatus;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    List<UserSubscription> findByUser(User user);
    List<UserSubscription> findByUserId(UUID userId);
    List<UserSubscription> findByUserIdAndSubscriptionStatus(UUID userId, SubscriptionStatus subscriptionStatus);
    List<UserSubscription> findBySubscriptionStatus(SubscriptionStatus subscriptionStatus);
    
    @Query("""
            SELECT us FROM UserSubscription us
            JOIN FETCH us.subscriptionPackage
            WHERE us.user.id = :userId
              AND us.subscriptionStatus = 'ACTIVE'
            ORDER BY us.startDate DESC
            """)
    Optional<UserSubscription> findActiveByUserId(@Param("userId") UUID userId);
 
    @Query("""
            SELECT us FROM UserSubscription us
            JOIN FETCH us.subscriptionPackage
            WHERE us.user.id = :userId
            ORDER BY us.startDate DESC
            """)
    List<UserSubscription> findAllByUserId(@Param("userId") UUID userId);
}
