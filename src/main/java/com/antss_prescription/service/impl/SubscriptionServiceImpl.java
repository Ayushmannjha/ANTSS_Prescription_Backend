package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.AddDoctorAddonRequest;
import com.antss_prescription.dto.response.DoctorAddonResponse;
import com.antss_prescription.dto.response.SubscriptionResponse;
import com.antss_prescription.entity.DoctorAddon;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.AddonApprovalStatus;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.DoctorAddonRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.service.SubscriptionService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final DoctorAddonRepository doctorAddonRepository;
    private final ModelMapper modelMapper;

    public SubscriptionServiceImpl(UserSubscriptionRepository userSubscriptionRepository,
                                   DoctorAddonRepository doctorAddonRepository,
                                   ModelMapper modelMapper) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.doctorAddonRepository = doctorAddonRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public DoctorAddonResponse requestAddonDoctors(AddDoctorAddonRequest request, UUID userId) {
        UserSubscription sub = userSubscriptionRepository.findById(request.getUserSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", request.getUserSubscriptionId()));

        if (!sub.getUser().getId().equals(userId)) {
            throw new BusinessException("Unauthorized subscription access");
        }

        if (sub.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Cannot add doctors to an inactive/expired subscription");
        }

        SubscriptionPackage pkg = sub.getSubscriptionPackage();
        BigDecimal extraPrice = pkg.getExtraDoctorPrice();

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
        if (daysRemaining <= 0) {
            throw new BusinessException("Subscription is already expiring or expired");
        }

        int remainingMonths = (int) Math.max(1, Math.ceil(daysRemaining / 30.4375)); // average days in a month

        BigDecimal prorataAmount = extraPrice
                .multiply(BigDecimal.valueOf(request.getAdditionalDoctors()))
                .multiply(BigDecimal.valueOf(remainingMonths))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        DoctorAddon addon = new DoctorAddon();
        addon.setUserSubscription(sub);
        addon.setAdditionalDoctors(request.getAdditionalDoctors());
        addon.setYearlyPricePerDoctor(extraPrice);
        addon.setRemainingMonths(remainingMonths);
        addon.setProrataAmount(prorataAmount);
        addon.setStartDate(LocalDate.now());
        addon.setEndDate(sub.getEndDate());
        addon.setPaymentStatus(PaymentStatus.PENDING);
        addon.setApprovalStatus(AddonApprovalStatus.PENDING);

        DoctorAddon saved = doctorAddonRepository.save(addon);
        log.info("Addon doctor request created: subscription {}, additional docs {}", sub.getId(), request.getAdditionalDoctors());

        return modelMapper.map(saved, DoctorAddonResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listActiveSubscriptions(UUID userId) {
        return userSubscriptionRepository.findByUserIdAndSubscriptionStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(sub -> {
                    SubscriptionResponse response = modelMapper.map(sub, SubscriptionResponse.class);
                    response.setPackageName(sub.getSubscriptionPackage().getPackageName());
                    response.setPackageId(sub.getSubscriptionPackage().getId());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAddonResponse> listAddonRequests(UUID userId) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
                .flatMap(sub -> doctorAddonRepository.findByUserSubscriptionId(sub.getId()).stream())
                .map(addon -> modelMapper.map(addon, DoctorAddonResponse.class))
                .collect(Collectors.toList());
    }
}
