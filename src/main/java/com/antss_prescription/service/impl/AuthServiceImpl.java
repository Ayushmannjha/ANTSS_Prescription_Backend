package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.*;
import com.antss_prescription.dto.response.AuthResponse;
import com.antss_prescription.dto.response.UserResponse;
import com.antss_prescription.entity.LoginSession;
import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.exception.BusinessException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.exception.UnauthorizedException;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.security.JwtTokenProvider;
import com.antss_prescription.service.AuthService;
import com.antss_prescription.service.EmailService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {


    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    @Value("${app.admin.email}")
    private String adminEmail;

    public AuthServiceImpl(UserRepository userRepository,
                           PackageRepository packageRepository,
                           LoginSessionRepository loginSessionRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           EmailService emailService,
                           ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.modelMapper = modelMapper;
    }

    @Override
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
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
        user.setClinicName(request.getClinicName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setCountry(request.getCountry());
        user.setSubscriptionPackage(pkg);
        user.setNumDoctors(request.getNumDoctors());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(RegistrationStatus.PENDING);
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        emailService.sendRegistrationNotificationToAdmin(
                adminEmail, user.getFullName(), user.getClinicName(),
                user.getEmail(), pkg.getName(), user.getNumDoctors()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == RegistrationStatus.PENDING) {
            throw new UnauthorizedException("Your account is pending approval from admin.");
        }
        if (user.getStatus() == RegistrationStatus.REJECTED) {
            throw new UnauthorizedException("Your account has been rejected.");
        }
        if (user.getStatus() == RegistrationStatus.EXPIRED) {
            throw new UnauthorizedException("Your subscription has expired.");
        }

        LocalDate curr = LocalDate.now();
        LocalDate exp = user.getSubscriptionEnd();
        if(curr.isAfter(exp)) {
            user.setStatus(RegistrationStatus.EXPIRED);
            userRepository.save(user);
            loginSessionRepository.expireAllSessionsForUser(user);
            emailService.sendExpiryReminderEmail(user.getEmail(), user.getFullName());

            log.info("User subscription expired: {}", user.getEmail());

            throw new UnauthorizedException("Your subscription has expired");
        }

        SubscriptionPackage pkg = user.getSubscriptionPackage();
        if (pkg != null && pkg.getDeviceLimit() == 1) {
            loginSessionRepository.expireAllSessionsForUser(user);
        } else {
            List<LoginSession> activeSessions = loginSessionRepository.findByUserAndExpiredFalse(user);
            if (pkg != null && activeSessions.size() >= pkg.getDeviceLimit()) {
                loginSessionRepository.expireAllSessionsForUser(user);
            }
        }

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

        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

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
