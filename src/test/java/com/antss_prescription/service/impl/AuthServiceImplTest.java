package com.antss_prescription.service.impl;

import com.antss_prescription.dto.request.LoginRequest;
import com.antss_prescription.dto.request.RefreshTokenRequest;
import com.antss_prescription.dto.response.AuthResponse;
import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.LoginSession;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.LoginStatus;
import com.antss_prescription.enums.PaymentStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.SubscriptionStatus;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.UnauthorizedException;
import com.antss_prescription.repository.*;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.security.JwtTokenProvider;
import com.antss_prescription.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private LoginSessionRepository loginSessionRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private ClinicRepository clinicRepository;
    @Mock private UserSubscriptionRepository userSubscriptionRepository;
    @Mock private LoginCredentialRepository loginCredentialRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;
    @Mock private DoctorRepository doctorRepository;
    @Mock private RmoRepository rmoRepository;
    @Mock private AccessControlService accessControl;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void childFacilityLoginUsesOwnersPaidSubscription() {
        User child = approvedUser(UserType.HOSPITAL);
        User owner = approvedUser(UserType.HOSPITAL);
        LoginCredential credential = activeCredential(child);
        UserSubscription subscription = new UserSubscription();
        subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentStatus(PaymentStatus.PAID);
        subscription.setEndDate(LocalDate.now().plusDays(5));

        LoginRequest request = new LoginRequest();
        request.setEmail(child.getEmail());
        request.setPassword("secret");

        when(userRepository.findByEmail(child.getEmail())).thenReturn(Optional.of(child));
        when(loginCredentialRepository.findByUserId(child.getId())).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("secret", credential.getPasswordHash())).thenReturn(true);
        when(accessControl.resolveSubscriptionOwner(child)).thenReturn(owner);
        when(userSubscriptionRepository.findByUserIdAndSubscriptionStatus(owner.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(jwtTokenProvider.generateAccessToken(child.getEmail())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(child.getEmail())).thenReturn("refresh");
        when(hospitalRepository.findByUserId(child.getId())).thenReturn(List.of());

        AuthResponse response = authService.login(request);

        assertEquals("access", response.getAccessToken());
        verify(loginSessionRepository).save(any(LoginSession.class));
    }

    @Test
    void refreshIsRejectedAndSessionExpiredWhenCredentialIsBlocked() {
        User user = approvedUser(UserType.CLINIC);
        LoginCredential credential = activeCredential(user);
        credential.setLoginStatus(LoginStatus.BLOCKED);
        LoginSession session = new LoginSession();
        session.setUser(user);
        session.setRefreshToken("refresh");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh");

        when(loginSessionRepository.findByRefreshTokenAndExpiredFalse("refresh"))
                .thenReturn(Optional.of(session));
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh");
        when(loginCredentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(credential));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        assertTrue(session.isExpired());
        verify(loginSessionRepository).save(session);
    }

    private User approvedUser(UserType type) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Test User");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setMobileNumber("9876543210");
        user.setUserType(type);
        user.setRole(Role.ROLE_USER);
        user.setStatus(RegistrationStatus.APPROVED);
        return user;
    }

    private LoginCredential activeCredential(User user) {
        LoginCredential credential = new LoginCredential();
        credential.setUser(user);
        credential.setPasswordHash("hash");
        credential.setLoginStatus(LoginStatus.ACTIVE);
        return credential;
    }
}
