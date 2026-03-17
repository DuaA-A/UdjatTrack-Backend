package com.udjattrack.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility for generating cryptographically secure OTP codes.
 * Uses {@link SecureRandom} to avoid predictability.
 */
@Component
public class OtpUtil {

    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 6;

    /**
     * Generates a numeric OTP of the specified length.
     */
    public String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a 6-digit OTP (default).
     */
    public String generateOtp() {
        return generateOtp(DEFAULT_LENGTH);
    }
}
