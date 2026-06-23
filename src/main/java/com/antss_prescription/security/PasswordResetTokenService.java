package com.antss_prescription.security;

import com.antss_prescription.entity.User;
import com.antss_prescription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final UserRepository userRepository;
    private final TokenHashService tokenHashService;
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setPasswordResetToken(tokenHashService.hash(rawToken));
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        return rawToken;
    }
}
