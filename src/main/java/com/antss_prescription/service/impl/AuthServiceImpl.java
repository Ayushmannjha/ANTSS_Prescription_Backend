package com.antss_prescription.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antss_prescription.dto.request.ForgotPasswordRequest;
import com.antss_prescription.dto.request.LoginRequest;
import com.antss_prescription.dto.request.RefreshTokenRequest;
import com.antss_prescription.dto.request.RegisterRequest;
import com.antss_prescription.dto.request.ResetPasswordRequest;
import com.antss_prescription.dto.response.AuthResponse;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.LoginSession;
import com.antss_prescription.entity.Rmo;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.DurationType;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.exception.UnauthorizedException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.LoginCredentialRepository;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.RmoRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.security.ApprovalTokenUtils;
import com.antss_prescription.security.JwtTokenProvider;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.security.TokenHashService;
import com.antss_prescription.security.PasswordResetTokenService;
import com.antss_prescription.enums.LoginStatus;
import com.antss_prescription.service.AuthService;
import com.antss_prescription.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final DoctorRepository doctorRepository;
    private final RmoRepository rmoRepository;
    private final AccessControlService accessControl;
    private final TokenHashService tokenHashService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.base-url:http://localhost:2030}")
    private String baseUrl;


  @Override
public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        SubscriptionPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));

        if (!pkg.isActive()) {
            throw new BusinessException("Selected package is not active");
        }

        if (request.getUserType() != UserType.HOSPITAL && request.getUserType() != UserType.CLINIC) {
            throw new BusinessException("Only HOSPITAL and CLINIC users can register directly");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setUserType(request.getUserType());
        user.setStatus(RegistrationStatus.PENDING);
        user.setRole(Role.ROLE_USER);

        User savedUser = userRepository.save(user);

        log.info("User saved successfully. UserId={}", savedUser.getId());

        UserSubscription sub = new UserSubscription();
        sub.setUser(savedUser);
        sub.setSubscriptionPackage(pkg);
        sub.setStartDate(LocalDate.now());

        LocalDate endDate = LocalDate.now();

        if (pkg.getDurationType() == DurationType.SIX_MONTH) {
            endDate = endDate.plusMonths(6);
        } else if (pkg.getDurationType() == DurationType.ONE_YEAR) {
            endDate = endDate.plusYears(1);
        } else if (pkg.getDurationType() == DurationType.TWO_YEAR) {
            endDate = endDate.plusYears(2);
        }

        sub.setEndDate(endDate);
        sub.setAllowedDoctors(
                request.getAllowedDoctors() != null
                        ? request.getAllowedDoctors()
                        : pkg.getBaseDoctorLimit());

        sub.setAllowedHospitals(
                request.getAllowedHospitals() != null
                        ? request.getAllowedHospitals()
                        : 1);

        sub.setAllowedClinics(
                request.getAllowedClinics() != null
                        ? request.getAllowedClinics()
                        : 1);

        sub.setUsedDoctors(0);
        sub.setPaymentStatus(PaymentStatus.PENDING);
        sub.setSubscriptionStatus(SubscriptionStatus.PENDING);

        userSubscriptionRepository.save(sub);

        log.info("Subscription saved successfully");

        if (request.getUserType() == UserType.HOSPITAL) {

            Hospital hospital = new Hospital();
            hospital.setUser(savedUser);
            hospital.setOwner(savedUser);
            hospital.setHospitalName(request.getEntityName());
            hospital.setHospitalCode(generateUniqueHospitalCode());
            hospital.setAddressLine1(request.getAddressLine1());
            hospital.setCity(request.getCity());
            hospital.setState(request.getState());
            hospital.setPincode(request.getPincode());
            hospital.setEmail(request.getEmail());
            hospital.setMobileNumber(request.getMobileNumber());
            hospital.setMaxDoctorLimit(sub.getAllowedDoctors());
            hospital.setActiveDoctorCount(0);
            hospital.setStatus(EntityStatus.ACTIVE);

            log.info("Saving Hospital: {}", hospital);

            hospitalRepository.save(hospital);

            log.info("Hospital saved successfully");

        } else {

            Clinic clinic = new Clinic();
            clinic.setUser(savedUser);
            clinic.setOwner(savedUser);
            clinic.setClinicName(request.getEntityName());
            clinic.setClinicCode(generateUniqueClinicCode());

            // IMPORTANT
            // Add this only if registration number exists in request
            // clinic.setRegistrationNumber(request.getRegistrationNumber());

            clinic.setAddressLine1(request.getAddressLine1());
            clinic.setCity(request.getCity());
            clinic.setState(request.getState());
            clinic.setPincode(request.getPincode());
            clinic.setEmail(request.getEmail());
            clinic.setMobileNumber(request.getMobileNumber());
            clinic.setMaxDoctorLimit(sub.getAllowedDoctors());
            clinic.setActiveDoctorCount(0);
            clinic.setStatus(EntityStatus.ACTIVE);
           // clinic.setRegistrationNumber(adminEmail);

            log.info("Clinic Name: {}", clinic.getClinicName());
            log.info("Clinic Code: {}", clinic.getClinicCode());
            log.info("Registration Number: {}", clinic.getRegistrationNumber());

            clinicRepository.save(clinic);

            log.info("Clinic saved successfully");
        }

        log.info("New user registered: {}", savedUser.getEmail());

        String approvalToken = ApprovalTokenUtils.generateToken(
                savedUser.getId().toString(),
                adminEmail,
                jwtSecret
        );

        String approvalUrl = baseUrl + "/api/admin/approve-email"
                + "?userId=" + savedUser.getId()
                + "&token=" + approvalToken;

        emailService.sendRegistrationNotificationToAdmin(
                adminEmail,
                savedUser.getFullName(),
                request.getEntityName(),
                savedUser.getEmail(),
                pkg.getPackageName(),
                pkg.getBaseDoctorLimit(),
                approvalUrl
        );

}

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        LoginCredential credential = loginCredentialRepository.findByUserId(user.getId()).orElseThrow(
                ()-> new BusinessException("login credentials not found for the user, please contact admin")
        );

        if (credential.getLoginStatus() != LoginStatus.ACTIVE) {
            throw new UnauthorizedException("Login is disabled for this account");
        }

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == RegistrationStatus.PENDING) {
            throw new UnauthorizedException("Your account is pending approval from admin.");
        }
        if (user.getStatus() == RegistrationStatus.REJECTED) {
            throw new UnauthorizedException("Your account has been rejected.");
        }
        if (user.getStatus() == RegistrationStatus.EXPIRED || user.getStatus() == RegistrationStatus.INACTIVE) {
            throw new UnauthorizedException("Your account is inactive or expired.");
        }

        User subscriptionOwner = user.getRole() == Role.ROLE_ADMIN
                ? user : accessControl.resolveSubscriptionOwner(user);
        List<UserSubscription> activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(
                subscriptionOwner.getId(), SubscriptionStatus.ACTIVE);
        
        boolean hasValidSub = false;
        LocalDate today = LocalDate.now();
        for (UserSubscription sub : activeSubs) {
            if (today.isAfter(sub.getEndDate())) {
                sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
                log.info("Subscription {} expired for user {}", sub.getId(), user.getEmail());
            } else if (sub.getPaymentStatus() == PaymentStatus.PAID) {
                hasValidSub = true;
            }
        }

        if (!hasValidSub && !user.getRole().equals(Role.ROLE_ADMIN)) {

            loginSessionRepository.expireAllSessionsForUser(user);
            throw new UnauthorizedException("No active paid subscription is available for this account");
        }


        loginCredentialRepository.findByUserId(user.getId()).ifPresent(cred -> {
            cred.setLastLogin(LocalDateTime.now());
            loginCredentialRepository.save(cred);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        LoginSession session = new LoginSession();
        session.setUser(user);
        session.setToken(tokenHashService.hash(accessToken));
        session.setRefreshToken(tokenHashService.hash(refreshToken));
        session.setDeviceInfo(request.getDeviceInfo());
        loginSessionRepository.save(session);

        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, mapToUserResponse(user));
    }

    @Override
    public void logout(String token) {
        LoginSession session = loginSessionRepository.findByTokenAndExpiredFalse(tokenHashService.hash(token))
                .orElseThrow(() -> new UnauthorizedException("Invalid or already expired session"));
        session.setExpired(true);
        loginSessionRepository.save(session);
        log.info("User logged out, session invalidated.");
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = passwordResetTokenService.issue(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
            log.info("Password reset email sent to: {}", user.getEmail());
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        User user = userRepository.findByPasswordResetToken(tokenHashService.hash(request.getToken()))
                .orElseThrow(() -> new BusinessException("Invalid or expired password reset token"));

        LoginCredential credential = loginCredentialRepository.findByUserId(user.getId()).orElseThrow(
                () -> new BusinessException(" Login credentials not found please contact the admin")
        );

        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Password reset token has expired");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
       // user.setPassword(encodedPassword);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        loginCredentialRepository.findByUserId(user.getId()).ifPresent(cred -> {
            cred.setPasswordHash(encodedPassword);
            loginCredentialRepository.save(cred);
        });

        loginSessionRepository.expireAllSessionsForUser(user);
        log.info("Password reset successful for: {}", user.getEmail());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        LoginSession session = loginSessionRepository.findByRefreshTokenAndExpiredFalse(
                        tokenHashService.hash(request.getRefreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!jwtTokenProvider.validateToken(request.getRefreshToken(), "refresh")) {
            session.setExpired(true);
            loginSessionRepository.save(session);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = session.getUser();
        LoginCredential credential = loginCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Login credentials are unavailable"));
        if (user.getStatus() != RegistrationStatus.APPROVED
                || credential.getLoginStatus() != LoginStatus.ACTIVE
                || !hasValidSubscriptionFor(user)) {
            expireSession(session);
            throw new UnauthorizedException("Account or subscription is no longer active");
        }
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        session.setToken(tokenHashService.hash(newAccessToken));
        session.setRefreshToken(tokenHashService.hash(newRefreshToken));
        loginSessionRepository.save(session);

        return new AuthResponse(newAccessToken, newRefreshToken, mapToUserResponse(user));
    }

    private boolean hasValidSubscriptionFor(User user) {
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        User owner = accessControl.resolveSubscriptionOwner(user);
        LocalDate today = LocalDate.now();
        return userSubscriptionRepository.findByUserIdAndSubscriptionStatus(
                        owner.getId(), SubscriptionStatus.ACTIVE).stream()
                .anyMatch(sub -> !today.isAfter(sub.getEndDate())
                        && sub.getPaymentStatus() == PaymentStatus.PAID);
    }

    private void expireSession(LoginSession session) {
        session.setExpired(true);
        loginSessionRepository.save(session);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobileNumber(user.getMobileNumber());
        response.setUserType(user.getUserType());
        response.setStatus(user.getStatus());
        response.setRole(user.getRole());
        response.setRegistrationDate(user.getRegistrationDate());
        response.setCreatedAt(user.getCreatedAt());

        if (user.getUserType() == UserType.DOCTOR) {
            doctorRepository.findByUserId(user.getId()).ifPresent(doctor -> {
                response.setDoctorId(doctor.getId());
                if (doctor.getHospital() != null) {
                    response.setHospitalId(doctor.getHospital().getId());
                }
                if (doctor.getClinic() != null) {
                    response.setClinicId(doctor.getClinic().getId());
                }
            });
        } else if (user.getUserType() == UserType.RMO) {
            rmoRepository.findByUserId(user.getId()).ifPresent(rmo -> {
                response.setRmoId(rmo.getId());
                if (rmo.getHospital() != null) {
                    response.setHospitalId(rmo.getHospital().getId());
                }
                if (rmo.getClinic() != null) {
                    response.setClinicId(rmo.getClinic().getId());
                }
            });
        } else if (user.getUserType() == UserType.HOSPITAL) {
            List<Hospital> hospitals = hospitalRepository.findByUserId(user.getId());
            if (!hospitals.isEmpty()) {
                response.setHospitalId(hospitals.get(0).getId());
            }
        } else if (user.getUserType() == UserType.CLINIC) {
            List<Clinic> clinics = clinicRepository.findByUserId(user.getId());
            if (!clinics.isEmpty()) {
                response.setClinicId(clinics.get(0).getId());
            }
        }

        return response;
    }

    private String generateUniqueHospitalCode() {
        String code;
        do {
            code = "HOSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (hospitalRepository.findByHospitalCode(code).isPresent());
        return code;
    }

    private String generateUniqueClinicCode() {
        String code;
        do {
            code = "CLIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (clinicRepository.findByClinicCode(code).isPresent());
        return code;
    }
}
