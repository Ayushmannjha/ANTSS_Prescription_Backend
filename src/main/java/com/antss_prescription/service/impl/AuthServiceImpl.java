package com.antss_prescription.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.security.ApprovalTokenUtils;
import com.antss_prescription.security.JwtTokenProvider;
import com.antss_prescription.service.AuthService;
import com.antss_prescription.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
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

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.base-url:http://localhost:2030}")
    private String baseUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           PackageRepository packageRepository,
                           LoginSessionRepository loginSessionRepository,
                           HospitalRepository hospitalRepository,
                           ClinicRepository clinicRepository,
                           UserSubscriptionRepository userSubscriptionRepository,
                           LoginCredentialRepository loginCredentialRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           EmailService emailService,
                           DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.hospitalRepository = hospitalRepository;
        this.clinicRepository = clinicRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.loginCredentialRepository = loginCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.doctorRepository = doctorRepository;
    }

  @Override
public void register(RegisterRequest request) {

    try {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        SubscriptionPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package", request.getPackageId()));

        if (!pkg.isActive()) {
            throw new BusinessException("Selected package is not active");
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
        sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

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

    } catch (Exception e) {

        log.error("Registration failed", e);

        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        log.error("ROOT CAUSE => {}", root.getMessage());

        throw new BusinessException(
                "Registration failed: " + root.getMessage()
        );
    }
}

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        LoginCredential credential = loginCredentialRepository.findByUserId(user.getId()).orElseThrow(
                ()-> new BusinessException("login credentials not found for the user, please contact admin")
        );

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

        List<UserSubscription> activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(user.getId(), SubscriptionStatus.ACTIVE);
        System.out.println("===printintg usertype==="+user.getUserType());
        if (user.getUserType().equals(UserType.DOCTOR)) {
            Doctor doctor = doctorRepository.findByUserId(user.getId()).get();
            Hospital hospital = doctor.getHospital();
            Clinic clinic = doctor.getClinic();

            if (hospital != null) {
                UUID hospitalUserId = hospital.getUser().getId();
                System.out.println("====hospital is not empty====" + hospitalUserId);
                activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(
                        hospitalUserId, SubscriptionStatus.ACTIVE);
            } else if (clinic != null) {
                UUID clinicUserId = clinic.getUser().getId();
                System.out.println("====Clinic is not empty====" + clinicUserId);
                activeSubs = userSubscriptionRepository.findByUserIdAndSubscriptionStatus(
                        clinicUserId, SubscriptionStatus.ACTIVE);
            } else {
                throw new RuntimeException("Doctor is not associated with any Hospital or Clinic");
            }
        }
        
        boolean hasValidSub = false;
        LocalDate today = LocalDate.now();
        System.out.println("=======Doctor login Subscription checker=======");
        System.out.println(activeSubs);
        for (UserSubscription sub : activeSubs) {
        	System.out.println(sub.getEndDate());
            if (today.isAfter(sub.getEndDate())) {
                sub.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
                log.info("Subscription {} expired for user {}", sub.getId(), user.getEmail());
            } else {
                hasValidSub = true;
            }
        }

        if (!hasValidSub && !user.getRole().equals(Role.ROLE_ADMIN)) {

            user.setStatus(RegistrationStatus.EXPIRED);
            userRepository.save(user);
            loginSessionRepository.expireAllSessionsForUser(user);
            emailService.sendExpiryReminderEmail(user.getEmail(), user.getFullName());
            throw new UnauthorizedException("Your subscription has expired");
        }


        loginCredentialRepository.findByUserId(user.getId()).ifPresent(cred -> {
            cred.setLastLogin(LocalDateTime.now());
            loginCredentialRepository.save(cred);
        });

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        LoginSession session = new LoginSession();
        session.setUser(user);
        session.setToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(request.getDeviceInfo());
        loginSessionRepository.save(session);

        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, mapToUserResponse(user));
    }

    @Override
    public void logout(String token) {
        LoginSession session = loginSessionRepository.findByTokenAndExpiredFalse(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or already expired session"));
        session.setExpired(true);
        loginSessionRepository.save(session);
        log.info("User logged out, session invalidated.");
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        log.info("Password reset email sent to: {}", user.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        User user = userRepository.findByPasswordResetToken(request.getToken())
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
        LoginSession session = loginSessionRepository.findByRefreshTokenAndExpiredFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            session.setExpired(true);
            loginSessionRepository.save(session);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = session.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());

        session.setToken(newAccessToken);
        loginSessionRepository.save(session);

        return new AuthResponse(newAccessToken, request.getRefreshToken(), mapToUserResponse(user));
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
