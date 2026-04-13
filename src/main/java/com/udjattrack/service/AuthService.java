package com.udjattrack.service;
import com.udjattrack.dto.response.FleetManagerSignupResponse;
import java.util.UUID;
import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    void logout(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    FleetManagerSignupResponse signupFleetManager(CreateFleetManagerRequest request);
    FleetManagerSignupResponse approveFleetManager(UUID managerId);
    FleetManagerSignupResponse rejectFleetManager(UUID managerId, String reason);
}
