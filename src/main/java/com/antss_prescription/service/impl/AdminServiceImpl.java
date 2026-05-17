package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.ExtendValidityRequest;
import com.antss_prescription.dto.request.ModifyPackageRequest;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.AdminService;
import com.antss_prescription.service.EmailService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AdminServiceImpl implements AdminService {


    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    public AdminServiceImpl(UserRepository userRepository,
                            PackageRepository packageRepository,
                            LoginSessionRepository loginSessionRepository,
                            EmailService emailService,
                            ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.emailService = emailService;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<UserResponse> getPendingRegistrations() {
        return userRepository.findByStatus(RegistrationStatus.PENDING)
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse approveUser(Long userId) {
        User user = getUserOrThrow(userId);

        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException("Only PENDING registrations can be approved");
        }

        SubscriptionPackage pkg = user.getSubscriptionPackage();
        if (pkg == null) {
            throw new BusinessException("User has no package assigned");
        }

        user.setStatus(RegistrationStatus.APPROVED);
        user.setSubscriptionStart(LocalDate.now());
        user.setSubscriptionEnd(LocalDate.now().plusDays(pkg.getValidityDays()));
        User saved = userRepository.save(user);

        emailService.sendApprovalEmail(user.getEmail(), user.getFullName());
        log.info("User approved: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse rejectUser(Long userId) {
        User user = getUserOrThrow(userId);

        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException("Only PENDING registrations can be rejected");
        }

        user.setStatus(RegistrationStatus.REJECTED);
        User saved = userRepository.save(user);

        emailService.sendRejectionEmail(user.getEmail(), user.getFullName());
        log.info("User rejected: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse modifyUserPackage(Long userId, ModifyPackageRequest request) {
        User user = getUserOrThrow(userId);

        SubscriptionPackage newPkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));

        if (!newPkg.isActive()) {
            throw new BusinessException("Selected package is not active");
        }

        user.setSubscriptionPackage(newPkg);
        if (user.getStatus() == RegistrationStatus.APPROVED && user.getSubscriptionStart() != null) {
            user.setSubscriptionEnd(user.getSubscriptionStart().plusDays(newPkg.getValidityDays()));
        }
        User saved = userRepository.save(user);
        log.info("Package modified for user: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse extendValidity(Long userId, ExtendValidityRequest request) {
        User user = getUserOrThrow(userId);

        if (user.getSubscriptionEnd() == null) {
            throw new BusinessException("User does not have an active subscription to extend");
        }

        LocalDate currentEnd = user.getSubscriptionEnd();
        LocalDate base = currentEnd.isBefore(LocalDate.now()) ? LocalDate.now() : currentEnd;
        user.setSubscriptionEnd(base.plusDays(request.getDays()));

        if (user.getStatus() == RegistrationStatus.EXPIRED) {
            user.setStatus(RegistrationStatus.APPROVED);
        }

        User saved = userRepository.save(user);
        log.info("Validity extended for user: {} by {} days", user.getEmail(), request.getDays());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse blockUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(RegistrationStatus.REJECTED);
        loginSessionRepository.expireAllSessionsForUser(user);
        User saved = userRepository.save(user);
        log.info("User blocked: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    @Override
    public UserResponse unblockUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(RegistrationStatus.APPROVED);
        User saved = userRepository.save(user);
        log.info("User unblocked: {}", user.getEmail());
        return mapToUserResponse(saved);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setStatus(user.getStatus().name());
        response.setRole(user.getRole().name());
        if (user.getSubscriptionPackage() != null) {
            response.setPackageName(user.getSubscriptionPackage().getName());
            response.setPackageDoctorLimit(user.getSubscriptionPackage().getDoctorLimit());
            response.setPackageDeviceLimit(user.getSubscriptionPackage().getDeviceLimit());
        }
        return response;
    }
}
