package com.antss_prescription.configuration;

import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.service.EmailService;
import com.antss_prescription.service.impl.LinkedAccountStatusService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final EmailService emailService;
    private final LinkedAccountStatusService linkedAccountStatusService;

    public SchedulerConfig(UserSubscriptionRepository userSubscriptionRepository,
                           EmailService emailService,
                           LinkedAccountStatusService linkedAccountStatusService) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.emailService = emailService;
        this.linkedAccountStatusService = linkedAccountStatusService;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void expireSubscriptions() {
        log.info("Running daily subscription expiry check...");
        LocalDate today = LocalDate.now();

        List<UserSubscription> activeSubs = userSubscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        int expiredCount = 0;

        for (UserSubscription sub : activeSubs) {
            if (today.isAfter(sub.getEndDate())) {
                sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
                expiredCount++;

                // Check if user has any remaining active subscriptions
                User user = sub.getUser();
                List<UserSubscription> userActiveSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(user.getId(), SubscriptionStatus.ACTIVE);
                
                if (userActiveSubs.isEmpty()) {
                    linkedAccountStatusService.expireOwnerAndLinkedAccounts(user);

                    // Send expiry email
                    emailService.sendExpiryReminderEmail(user.getEmail(), user.getFullName());

                    log.info("All subscriptions expired for user: {}", user.getEmail());
                }
            }
        }

        log.info("Completed subscription expiry check. {} subscriptions expired.", expiredCount);
    }
}
