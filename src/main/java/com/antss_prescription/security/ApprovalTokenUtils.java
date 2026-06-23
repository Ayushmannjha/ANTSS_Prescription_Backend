package com.antss_prescription.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ApprovalTokenUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VALIDITY_HOURS = 24;

    public static String generateToken(String userId, String adminEmail, String jwtSecret) {
        try {
            byte[] nonce = new byte[24];
            RANDOM.nextBytes(nonce);
            long expiresAt = Instant.now().plus(VALIDITY_HOURS, ChronoUnit.HOURS).getEpochSecond();
            String payload = userId + "|" + adminEmail + "|" + expiresAt + "|"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return encodedPayload + "." + sign(encodedPayload, jwtSecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate approval token", e);
        }
    }

    public static boolean verifyToken(String userId, String adminEmail, String jwtSecret, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] tokenParts = token.split("\\.", 2);
            if (tokenParts.length != 2) return false;

            byte[] suppliedSignature = Base64.getUrlDecoder().decode(tokenParts[1]);
            byte[] expectedSignature = Base64.getUrlDecoder().decode(sign(tokenParts[0], jwtSecret));
            if (!MessageDigest.isEqual(expectedSignature, suppliedSignature)) return false;

            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split("\\|", 4);
            if (fields.length != 4) return false;
            long expiresAt = Long.parseLong(fields[2]);
            return fields[0].equals(userId)
                    && fields[1].equals(adminEmail)
                    && Instant.now().getEpochSecond() <= expiresAt;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
