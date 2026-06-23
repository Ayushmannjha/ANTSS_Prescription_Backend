package com.antss_prescription.security;

import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.User;
import com.antss_prescription.enums.Role;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("subscriptionAuthorization")
@RequiredArgsConstructor
public class SubscriptionAuthorization {

    private final AccessControlService accessControl;
    private final UserSubscriptionRepository subscriptionRepository;
    private final DoctorRepository doctorRepository;

    public boolean canAccessUser(UUID userId) {
        User current = accessControl.currentUser();
        return current.getRole() == Role.ROLE_ADMIN
                || current.getId().equals(userId)
                || accessControl.resolveSubscriptionOwner(current).getId().equals(userId);
    }

    public boolean canAccessSubscription(UUID subscriptionId) {
        User current = accessControl.currentUser();
        if (current.getRole() == Role.ROLE_ADMIN) return true;
        User owner = accessControl.resolveSubscriptionOwner(current);
        return subscriptionRepository.findById(subscriptionId)
                .map(subscription -> subscription.getUser().getId().equals(owner.getId()))
                .orElse(false);
    }

    public boolean canAccessDoctor(UUID doctorId) {
        User current = accessControl.currentUser();
        if (current.getRole() == Role.ROLE_ADMIN) return true;
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        return doctor != null && (doctor.getUser() != null && doctor.getUser().getId().equals(current.getId())
                || accessControl.canAccess(doctor.getHospital(), current)
                || accessControl.canAccess(doctor.getClinic(), current));
    }
}
