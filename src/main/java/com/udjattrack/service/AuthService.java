package com.udjattrack.service;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.AuthResponse;

/**
 * Auth service contract. Handles the full authentication lifecycle:
 * login, logout, forgot-password (OTP), verify-OTP, reset-password, token refresh.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    void logout(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
