package com.antss_prescription.repository;

import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    List<UserSubscription> findByUser(User user);
    List<UserSubscription> findByUserId(UUID userId);
    List<UserSubscription> findByUserIdAndSubscriptionStatus(UUID userId, SubscriptionStatus subscriptionStatus);
    List<UserSubscription> findBySubscriptionStatus(SubscriptionStatus subscriptionStatus);
}
