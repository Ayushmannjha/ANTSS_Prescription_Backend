package com.antss_prescription.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class TokenHashService {

    private static final String PREFIX = "sha256:";

    public String hash(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash security token", ex);
        }
    }

    public boolean isHash(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
